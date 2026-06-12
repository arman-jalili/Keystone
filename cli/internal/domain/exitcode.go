// Package domain defines the core domain types for the keystone-cli orchestrator.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#components
package domain

// ExitCode communicates the analysis verdict to the CI pipeline via the
// process exit code. These values are a CONTRACT — they MUST NOT change
// without updating all CI pipeline consumers.
type ExitCode int

const (
	ExitPass    ExitCode = 0 // No breaking changes — spec is safe
	ExitFail    ExitCode = 1 // Breaking changes detected — CI should fail
	ExitWarn    ExitCode = 2 // Only additive/warning changes — alert but non-blocking
	ExitError   ExitCode = 3 // Internal error (parse failure, I/O, network)
)
