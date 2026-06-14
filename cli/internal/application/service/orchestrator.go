// Package service implements the application-layer orchestration for the
// keystone-cli analysis pipeline.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#cli-main
//
// The orchestrator is the central coordinator that wires together:
//  1. SpecParser  → parse the current spec file
//  2. SpecCache   → retrieve previous cached version
//  3. DiffEngine  → compute the diff
//  4. SpecCache   → cache the current spec for future runs
//  5. Uploader    → send results to the Keystone server (best-effort)
//  6. Publisher   → emit domain events for observability
package service

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"log/slog"
	"time"

	"github.com/arman-jalili/keystone-cli/internal/application/dto"
	"github.com/arman-jalili/keystone-cli/internal/application/interfaces"
	"github.com/arman-jalili/keystone-cli/internal/domain"
	domainevent "github.com/arman-jalili/keystone-cli/internal/domain/event"
	infraevent "github.com/arman-jalili/keystone-cli/internal/infrastructure/event"
)

// Orchestrator implements interfaces.Orchestrator by coordinating
// the full analysis pipeline.
type Orchestrator struct {
	parser    interfaces.SpecParser
	cache     interfaces.SpecCache
	diff      interfaces.DiffEngine
	uploader  interfaces.Uploader
	publisher infraevent.Publisher

	logger *slog.Logger
}

// NewOrchestrator creates a fully wired Orchestrator.
//
// All dependencies are injected — no service locator or global state.
// The caller (main.go) is responsible for constructing the adapters
// and passing them in.
func NewOrchestrator(
	parser interfaces.SpecParser,
	cache interfaces.SpecCache,
	diff interfaces.DiffEngine,
	uploader interfaces.Uploader,
	publisher infraevent.Publisher,
	logger *slog.Logger,
) *Orchestrator {
	return &Orchestrator{
		parser:    parser,
		cache:     cache,
		diff:      diff,
		uploader:  uploader,
		publisher: publisher,
		logger:    logger,
	}
}

// Analyze runs the full pipeline: parse → cache → diff → cache → upload.
func (o *Orchestrator) Analyze(req *dto.AnalyzeRequest) (*dto.AnalyzeResponse, error) {
	correlationID := newCorrelationID()
	start := time.Now()

	o.logger.InfoContext(nil,
		"analysis started",
		"specPath", req.SpecPath,
		"correlationId", correlationID,
		"serverUrl", req.ServerURL,
		"branch", req.BranchName,
		"commit", req.GitCommitSHA,
	)

	// 1. Parse the current spec file
	o.logger.Debug("parsing spec", "path", req.SpecPath)
	target, err := o.parser.Parse(req.SpecPath)
	if err != nil {
		o.logger.Error("spec parsing failed",
			"path", req.SpecPath,
			"error", err,
		)
		return nil, fmt.Errorf("parse spec: %w", err)
	}

	o.publishEvent(&domainevent.Event{
		Type:          domainevent.TypeSpecParsed,
		CorrelationID: correlationID,
		Payload: domainevent.SpecParsedPayload{
			Path:          req.SpecPath,
			Checksum:      target.Checksum,
			EndpointCount: len(target.Endpoints),
		},
	})

	// 2. Retrieve previous cached version
	o.logger.Debug("checking cache", "checksum", target.Checksum)
	var base *domain.SpecDocument
	var cacheHit bool

	cached, err := o.cache.Get(target.Checksum)
	if err == nil {
		// Cache hit — found a previous version of the same spec.
		// Diff against the cached version.
		base = &domain.SpecDocument{
			Version:    "",
			Title:      target.Title,
			APIVersion: target.APIVersion,
			Endpoints:  cached.Endpoints,
		}
		cacheHit = true
		o.logger.Debug("cache hit", "checksum", target.Checksum)
		o.publishEvent(&domainevent.Event{
			Type:          domainevent.TypeCacheHit,
			CorrelationID: correlationID,
			Payload:       domainevent.SpecParsedPayload{Path: req.SpecPath, Checksum: target.Checksum},
		})
	} else {
		// Cache miss — this is the first time we've seen this spec.
		// Diff against nil (all endpoints are additive).
		o.logger.Debug("cache miss — no previous version found",
			"checksum", target.Checksum,
			"cacheError", err,
		)
		cacheHit = false
		o.publishEvent(&domainevent.Event{
			Type:          domainevent.TypeCacheMiss,
			CorrelationID: correlationID,
			Payload:       domainevent.SpecParsedPayload{Path: req.SpecPath, Checksum: target.Checksum},
		})
	}

	// 3. Compute diff
	o.logger.Debug("computing diff")
	diffResult, err := o.diff.Diff(base, target)
	if err != nil {
		o.logger.Error("diff computation failed", "error", err)
		return nil, fmt.Errorf("compute diff: %w", err)
	}

	o.publishEvent(&domainevent.Event{
		Type:          domainevent.TypeDiffComputed,
		CorrelationID: correlationID,
		Payload: domainevent.DiffComputedPayload{
			Verdict:     diffResult.Verdict,
			ChangeCount: len(diffResult.Changes),
			AnalysisMs:  diffResult.AnalysisMs,
		},
	})

	// 4. Cache the current spec for future runs
	o.logger.Debug("caching current spec", "checksum", target.Checksum)
	cacheSpec := &domain.CachedSpec{
		Checksum:  target.Checksum,
		Endpoints: target.Endpoints,
		CachedAt:  time.Now(),
	}
	if err := o.cache.Set(target.Checksum, cacheSpec); err != nil {
		// Non-fatal — cache write failure doesn't affect the verdict
		o.logger.Warn("failed to cache spec", "checksum", target.Checksum, "error", err)
	}

	// 5. Upload to server (best-effort, async-safe)
	uploadSucceeded := false
	analysisCtx := &domain.AnalysisContext{
		SpecPath:     req.SpecPath,
		GitCommitSHA: req.GitCommitSHA,
		BranchName:   req.BranchName,
		CIJobID:      req.CIJobID,
		CacheHit:     cacheHit,
	}

	if req.ServerURL != "" {
		o.logger.Debug("uploading results to server",
			"serverUrl", req.ServerURL,
		)
		o.publishEvent(&domainevent.Event{
			Type:          domainevent.TypeUploadInitiated,
			CorrelationID: correlationID,
			Payload:       domainevent.UploadInitiatedPayload{ServerURL: req.ServerURL, Attempt: 1},
		})

		err := o.uploader.Upload(diffResult, analysisCtx)
		if err != nil {
			o.logger.Warn("upload failed (non-fatal)", "error", err)
			o.publishEvent(&domainevent.Event{
				Type:          domainevent.TypeUploadFailed,
				CorrelationID: correlationID,
				Payload:       domainevent.UploadFailedPayload{Attempts: 3, LastErr: err.Error()},
			})
		} else {
			uploadSucceeded = true
			o.publishEvent(&domainevent.Event{
				Type:          domainevent.TypeUploadCompleted,
				CorrelationID: correlationID,
				Payload:       domainevent.UploadCompletedPayload{StatusCode: 200, DurationMs: time.Since(start).Milliseconds()},
			})
		}
	} else {
		o.logger.Debug("no server URL configured — skipping upload")
	}

	// 6. Build summary
	elapsed := time.Since(start)
	summary := buildSummary(req.SpecPath, diffResult, cacheHit, uploadSucceeded)
	o.logger.InfoContext(nil,
		"analysis complete",
		"verdict", diffResult.Verdict,
		"changes", len(diffResult.Changes),
		"cacheHit", cacheHit,
		"uploaded", uploadSucceeded,
		"elapsed", elapsed,
	)

	return &dto.AnalyzeResponse{
		Verdict:         diffResult.Verdict,
		Changes:         diffResult.Changes,
		AnalysisMs:      diffResult.AnalysisMs,
		CacheHit:        cacheHit,
		UploadSucceeded: uploadSucceeded,
		Summary:         summary,
	}, nil
}

// publishEvent dispatches a domain event through the publisher.
// Failures are logged but not propagated — events are advisory.
func (o *Orchestrator) publishEvent(e *domainevent.Event) {
	if err := o.publisher.Publish(e); err != nil {
		o.logger.Warn("failed to publish event", "type", e.Type, "error", err)
	}
}

// newCorrelationID generates a unique correlation ID for a single analysis run.
func newCorrelationID() string {
	b := make([]byte, 16)
	if _, err := rand.Read(b); err != nil {
		// Fallback to timestamp-based ID if crypto/rand fails
		return fmt.Sprintf("fallback-%d", time.Now().UnixNano())
	}
	return hex.EncodeToString(b)
}

// buildSummary creates a short human-readable summary of the analysis result.
func buildSummary(specPath string, result *domain.DiffResult, cacheHit, uploaded bool) string {
	verdict := string(result.Verdict)
	changeCount := len(result.Changes)

	switch {
	case result.Verdict == domain.VerdictPass && changeCount == 0:
		return fmt.Sprintf("✅ %s — no changes detected", verdict)
	case result.Verdict == domain.VerdictPass && changeCount > 0:
		return fmt.Sprintf("✅ %s — %d additive change(s)", verdict, changeCount)
	case result.Verdict == domain.VerdictWarning:
		return fmt.Sprintf("⚠️  %s — %d warning(s)", verdict, changeCount)
	case result.Verdict == domain.VerdictBreaking:
		return fmt.Sprintf("❌ %s — %d breaking change(s)", verdict, changeCount)
	default:
		return fmt.Sprintf("%s — %d change(s)", verdict, changeCount)
	}
}
