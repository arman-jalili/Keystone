// Package diff provides an implementation of the DiffEngine interface that
// compares two parsed OpenAPI specs at the endpoint level.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#local-diff-engine
//
// The diff is based on path+method endpoint keys rather than full schema
// comparison, enabling fast (<50ms) computation for specs under 1 MB.
// When the base spec is nil (cache miss), all target endpoints are
// classified as ADDITIVE.
package diff

import (
	"fmt"
	"time"

	"github.com/arman-jalili/keystone-cli/internal/domain"
)

// SpecDiffEngine implements interfaces.DiffEngine by comparing endpoint
// sets between two SpecDocuments.
type SpecDiffEngine struct{}

// NewSpecDiffEngine creates a new SpecDiffEngine.
func NewSpecDiffEngine() *SpecDiffEngine {
	return &SpecDiffEngine{}
}

// endpointKey uniquely identifies an endpoint by its path and method.
type endpointKey struct {
	path   string
	method string
}

// Diff compares the base spec against the target spec.
func (e *SpecDiffEngine) Diff(base, target *domain.SpecDocument) (*domain.DiffResult, error) {
	start := time.Now()

	if target == nil || target.Endpoints == nil {
		return nil, fmt.Errorf("target spec is nil or has no endpoints")
	}

	// If no base, everything is additive.
	if base == nil || base.Endpoints == nil {
		changes := make([]domain.Change, len(target.Endpoints))
		for i, ep := range target.Endpoints {
			changes[i] = domain.Change{
				Severity:    domain.SeverityAdditive,
				Path:        ep.Path,
				Method:      ep.Method,
				Description: fmt.Sprintf("Added %s %s endpoint", ep.Method, ep.Path),
			}
		}
		return &domain.DiffResult{
			Verdict:    classifyVerdict(changes),
			Changes:    changes,
			AnalysisMs: time.Since(start).Milliseconds(),
		}, nil
	}

	// Build index of base endpoints for O(1) lookup.
	baseIndex := make(map[endpointKey]domain.Endpoint, len(base.Endpoints))
	for _, ep := range base.Endpoints {
		key := endpointKey{path: ep.Path, method: ep.Method}
		baseIndex[key] = ep
	}

	// Build index of target endpoints.
	targetIndex := make(map[endpointKey]bool, len(target.Endpoints))
	for _, ep := range target.Endpoints {
		key := endpointKey{path: ep.Path, method: ep.Method}
		targetIndex[key] = true
	}

	var changes []domain.Change

	// Detect removed endpoints (in base but not in target) → BREAKING.
	for _, ep := range base.Endpoints {
		key := endpointKey{path: ep.Path, method: ep.Method}
		if !targetIndex[key] {
			changes = append(changes, domain.Change{
				Severity:    domain.SeverityBreaking,
				Path:        ep.Path,
				Method:      ep.Method,
				Description: fmt.Sprintf("Removed %s %s endpoint", ep.Method, ep.Path),
			})
		}
	}

	// Detect added endpoints (in target but not in base) → ADDITIVE.
	for _, ep := range target.Endpoints {
		key := endpointKey{path: ep.Path, method: ep.Method}
		if _, exists := baseIndex[key]; !exists {
			changes = append(changes, domain.Change{
				Severity:    domain.SeverityAdditive,
				Path:        ep.Path,
				Method:      ep.Method,
				Description: fmt.Sprintf("Added %s %s endpoint", ep.Method, ep.Path),
			})
		}
	}

	// Detect deprecation changes (exists in both but deprecated flag changed) → WARNING.
	for _, targetEP := range target.Endpoints {
		key := endpointKey{path: targetEP.Path, method: targetEP.Method}
		baseEP, exists := baseIndex[key]
		if !exists {
			continue
		}
		if targetEP.Deprecated && !baseEP.Deprecated {
			changes = append(changes, domain.Change{
				Severity:    domain.SeverityWarning,
				Path:        targetEP.Path,
				Method:      targetEP.Method,
				Description: fmt.Sprintf("%s %s endpoint is now deprecated", targetEP.Method, targetEP.Path),
			})
		}
	}

	verdict := classifyVerdict(changes)

	return &domain.DiffResult{
		Verdict:    verdict,
		Changes:    changes,
		AnalysisMs: time.Since(start).Milliseconds(),
	}, nil
}

// classifyVerdict determines the overall verdict from a set of changes.
func classifyVerdict(changes []domain.Change) domain.Verdict {
	hasBreaking := false
	hasWarning := false

	for _, c := range changes {
		switch c.Severity {
		case domain.SeverityBreaking:
			hasBreaking = true
		case domain.SeverityWarning:
			hasWarning = true
		}
	}

	if hasBreaking {
		return domain.VerdictBreaking
	}
	if hasWarning {
		return domain.VerdictWarning
	}
	if len(changes) > 0 {
		return domain.VerdictPass // only additive/info changes → still a pass
	}
	return domain.VerdictPass
}
