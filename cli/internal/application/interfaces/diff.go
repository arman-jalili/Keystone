package interfaces

import "github.com/arman-jalili/keystone-cli/internal/domain"

// DiffEngine compares two parsed OpenAPI specs and produces a structured
// diff result with a verdict.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#local-diff-engine
//
// Responsibilities:
//   - Compare all paths and operations between base and target specs
//   - Classify each difference as ADDITIVE, BREAKING, WARNING, or INFO
//   - Produce a Verdict summarising the overall impact
//   - Complete within <50ms p99 for specs under 1 MB
//
// Diff rules (contract):
//   - Removed path → BREAKING
//   - Removed operation → BREAKING
//   - Changed request/response schema (narrowing) → BREAKING
//   - Added path → ADDITIVE
//   - Added operation → ADDITIVE
//   - Added optional field → ADDITIVE
//   - Deprecation → WARNING
//   - Description/example change → INFO
//
// Implementations:
//   - cli/diff.go (embedded diff engine)
type DiffEngine interface {
	// Diff compares the base spec against the target spec.
	// If base is nil (cache miss), all target changes are classified as ADDITIVE.
	Diff(base, target *domain.SpecDocument) (*domain.DiffResult, error)
}
