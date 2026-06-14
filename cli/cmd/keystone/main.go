// Package main is the CLI entry point for keystone-cli.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#cli-main
//
// Contract: This package MUST forward all flags to the application layer
// and MUST NOT contain business logic. All orchestration belongs in the
// application layer.
//
// Flags:
//
//	--spec     string   Path to the OpenAPI 3.x YAML/JSON spec file (required)
//	--server   string   Keystone server URL for audit upload (optional)
//	--token    string   API token for authenticated upload (optional)
//	--cache    string   Cache directory path (default: ~/.keystone/cache)
//	--verbose  bool     Enable verbose logging
//	--commit   string   Git commit SHA for the analysis context (optional)
//	--branch   string   Git branch name for the analysis context (optional)
//	--ci-job   string   CI pipeline job identifier (optional)
//
// Exit codes (contract — MUST NOT change):
//
//	0 = PASS    — no breaking changes detected
//	1 = FAIL    — breaking changes detected
//	2 = WARN    — only additive/non-breaking warnings
//	3 = ERROR   — internal error (parse failure, I/O error, etc.)
//
// See: internal/domain/exitcode.go
package main

import (
	"context"
	"flag"
	"fmt"
	"log/slog"
	"os"
	"os/exec"
	"os/signal"
	"strings"
	"syscall"

	"github.com/arman-jalili/keystone-cli/internal/application/dto"
	"github.com/arman-jalili/keystone-cli/internal/application/service"
	"github.com/arman-jalili/keystone-cli/internal/domain"
	infraCache "github.com/arman-jalili/keystone-cli/internal/infrastructure/cache"
	"github.com/arman-jalili/keystone-cli/internal/infrastructure/diff"
	infraEvent "github.com/arman-jalili/keystone-cli/internal/infrastructure/event"
	"github.com/arman-jalili/keystone-cli/internal/infrastructure/parser"
	"github.com/arman-jalili/keystone-cli/internal/infrastructure/uploader"
)

// Build-time variables (set via -ldflags).
var (
	version = "dev"
	commit  = "none"
	date    = "unknown"
)

func main() {
	// ─────────────────────────────────────────────────────────
	// 1. Parse flags
	// ─────────────────────────────────────────────────────────
	flagSpec := flag.String("spec", "", "Path to the OpenAPI 3.x YAML/JSON spec file (required)")
	flagServer := flag.String("server", "", "Keystone server URL for audit upload (optional)")
	flagToken := flag.String("token", "", "API token for authenticated upload (optional)")
	flagCache := flag.String("cache", "", "Cache directory path (default: ~/.keystone/cache)")
	flagVerbose := flag.Bool("verbose", false, "Enable verbose (debug) logging")
	flagCommit := flag.String("commit", "", "Git commit SHA for the analysis context")
	flagBranch := flag.String("branch", "", "Git branch name for the analysis context")
	flagCIJob := flag.String("ci-job", "", "CI pipeline job identifier")
	flagVersion := flag.Bool("version", false, "Print version and exit")

	flag.Usage = func() {
		fmt.Fprintf(os.Stderr, `Keystone CLI — OpenAPI Specification Governance

Usage:
  keystone-cli --spec <path> [options]

Required:
  --spec  string   Path to the OpenAPI 3.x YAML/JSON spec file

Options:
  --server  string   Keystone server URL for audit upload (e.g. http://localhost:8080)
  --token   string   API token for authenticated upload
  --cache   string   Cache directory path (default: ~/.keystone/cache)
  --commit  string   Git commit SHA for the analysis context
  --branch  string   Git branch name for the analysis context
  --ci-job  string   CI pipeline job identifier
  --verbose          Enable verbose (debug) logging
  --version          Print version and exit

Exit codes:
  0  PASS    — no breaking changes detected
  1  FAIL    — breaking changes detected
  2  WARN    — only additive/non-breaking warnings
  3  ERROR   — internal error (parse failure, I/O error, etc.)

Examples:
  keystone-cli --spec ./openapi.yaml
  keystone-cli --spec ./openapi.yaml --server http://keystone:8080 --token $KEYSTONE_TOKEN
  keystone-cli --spec ./openapi.yaml --verbose --branch main --commit $(git rev-parse HEAD)
`)
	}

	flag.Parse()

	// ─────────────────────────────────────────────────────────
	// 2. Handle --version
	// ─────────────────────────────────────────────────────────
	if *flagVersion {
		fmt.Printf("keystone-cli %s (commit: %s, built: %s)\n", version, commit, date)
		os.Exit(0)
	}

	// ─────────────────────────────────────────────────────────
	// 3. Validate required flags
	// ─────────────────────────────────────────────────────────
	if *flagSpec == "" {
		fmt.Fprintln(os.Stderr, "ERROR: --spec is required")
		flag.Usage()
		os.Exit(int(domain.ExitError))
	}

	// ─────────────────────────────────────────────────────────
	// 4. Set up structured logging
	// ─────────────────────────────────────────────────────────
	logLevel := slog.LevelInfo
	if *flagVerbose {
		logLevel = slog.LevelDebug
	}
	logger := slog.New(slog.NewTextHandler(os.Stderr, &slog.HandlerOptions{
		Level:     logLevel,
		AddSource: *flagVerbose,
	}))

	// ─────────────────────────────────────────────────────────
	// 5. Resolve environment defaults
	// ─────────────────────────────────────────────────────────
	serverURL := *flagServer
	if serverURL == "" {
		serverURL = os.Getenv("KEYSTONE_SERVER_URL")
	}

	apiToken := *flagToken
	if apiToken == "" {
		apiToken = os.Getenv("KEYSTONE_API_TOKEN")
	}

	cacheDir := *flagCache
	if cacheDir == "" {
		cacheDir = os.Getenv("KEYSTONE_CACHE_DIR")
	}

	// Resolve commit SHA (try git if not provided)
	commitSHA := *flagCommit
	if commitSHA == "" {
		commitSHA = os.Getenv("GIT_COMMIT_SHA")
	}
	if commitSHA == "" {
		commitSHA = tryGitRevParse(logger)
	}

	// Resolve branch name
	branchName := *flagBranch
	if branchName == "" {
		branchName = os.Getenv("GIT_BRANCH_NAME")
	}
	if branchName == "" {
		branchName = tryGitBranch(logger)
	}

	ciJobID := *flagCIJob
	if ciJobID == "" {
		ciJobID = os.Getenv("CI_JOB_ID")
	}

	// ─────────────────────────────────────────────────────────
	// 6. Construct infrastructure adapters
	// ─────────────────────────────────────────────────────────
	specParser := parser.NewKinOpenAPIParser()
	specDiff := diff.NewSpecDiffEngine()

	specCache, err := infraCache.NewFilesystemCache(cacheDir)
	if err != nil {
		logger.Error("failed to initialise cache", "error", err)
		os.Exit(int(domain.ExitError))
	}

	var uploadSvc *uploader.HTTPUploader
	if serverURL != "" {
		uploadSvc = uploader.NewHTTPUploader(serverURL, apiToken)
	} else {
		logger.Info("no server URL configured — upload will be skipped")
	}

	eventPublisher := infraEvent.NewStdoutPublisher(logger)

	// ─────────────────────────────────────────────────────────
	// 7. Construct the orchestrator
	// ─────────────────────────────────────────────────────────
	orchestrator := service.NewOrchestrator(
		specParser,
		specCache,
		specDiff,
		uploadSvc,
		eventPublisher,
		logger,
	)

	// ─────────────────────────────────────────────────────────
	// 8. Set up graceful shutdown
	// ─────────────────────────────────────────────────────────
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
	go func() {
		select {
		case sig := <-sigCh:
			logger.Warn("received signal, shutting down", "signal", sig)
			cancel()
		case <-ctx.Done():
		}
	}()

	_ = ctx // Used for future context propagation

	// ─────────────────────────────────────────────────────────
	// 9. Run the analysis
	// ─────────────────────────────────────────────────────────
	req := &dto.AnalyzeRequest{
		SpecPath:     *flagSpec,
		ServerURL:    serverURL,
		APIToken:     apiToken,
		CacheDir:     cacheDir,
		GitCommitSHA: commitSHA,
		BranchName:   branchName,
		CIJobID:      ciJobID,
	}

	logger.Info("analysis starting",
		"spec", req.SpecPath,
		"server", req.ServerURL,
		"commit", req.GitCommitSHA,
		"branch", req.BranchName,
	)

	result, err := orchestrator.Analyze(req)
	if err != nil {
		logger.Error("analysis failed", "error", err)
		fmt.Fprintln(os.Stderr, "ERROR:", err)
		os.Exit(int(domain.ExitError))
	}

	// ─────────────────────────────────────────────────────────
	// 10. Print result summary to stderr, detail to stdout
	// ─────────────────────────────────────────────────────────
	fmt.Println(result.Summary)
	for _, ch := range result.Changes {
		fmt.Printf("  [%s] %s %s — %s\n", ch.Severity, ch.Method, ch.Path, ch.Description)
	}

	if result.CacheHit {
		logger.Debug("cache was hit — diffed against previous version")
	}
	if result.UploadSucceeded {
		logger.Debug("results uploaded to server")
	}

	// ─────────────────────────────────────────────────────────
	// 11. Exit with appropriate code (contract — MUST NOT change)
	// ─────────────────────────────────────────────────────────
	switch result.Verdict {
	case domain.VerdictPass:
		logger.Info("verdict: PASS — no breaking changes")
		os.Exit(int(domain.ExitPass))
	case domain.VerdictWarning:
		logger.Info("verdict: WARNING — non-breaking changes detected",
			"changeCount", len(result.Changes),
		)
		os.Exit(int(domain.ExitWarn))
	case domain.VerdictBreaking:
		logger.Info("verdict: BREAKING — breaking changes detected",
			"changeCount", len(result.Changes),
		)
		os.Exit(int(domain.ExitFail))
	default:
		logger.Error("unknown verdict", "verdict", result.Verdict)
		os.Exit(int(domain.ExitError))
	}
}

// ─────────────────────────────────────────────────────────────
// Git helpers — best-effort context extraction
// ─────────────────────────────────────────────────────────────

func tryGitRevParse(logger *slog.Logger) string {
	cmd := exec.Command("git", "rev-parse", "HEAD")
	out, err := cmd.Output()
	if err != nil {
		logger.Debug("could not determine git commit SHA (not a git repository?)", "error", err)
		return "unknown"
	}
	sha := strings.TrimSpace(string(out))
	logger.Debug("resolved git commit SHA", "sha", sha)
	return sha
}

func tryGitBranch(logger *slog.Logger) string {
	cmd := exec.Command("git", "rev-parse", "--abbrev-ref", "HEAD")
	out, err := cmd.Output()
	if err != nil {
		logger.Debug("could not determine git branch (not a git repository?)", "error", err)
		return "unknown"
	}
	branch := strings.TrimSpace(string(out))
	logger.Debug("resolved git branch", "branch", branch)
	return branch
}
