// Package interfaces defines the service interfaces for the cli-orchestrator
// components. Each interface maps to one component in the architecture module.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#component-details
//
// These interfaces are the PRIMARY CONTRACT between the application's
// orchestration logic and the infrastructure adapters. Implementation
// issues must satisfy these interfaces, not the other way around.
package interfaces

import "github.com/arman-jalili/keystone-cli/internal/domain"

// SpecParser parses and validates an OpenAPI 3.x specification file.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#spec-parser
//
// Responsibilities:
//   - Load and parse a YAML/JSON OpenAPI 3.x spec from a file path
//   - Load and parse a YAML/JSON OpenAPI 3.x spec from a byte slice
//   - Validate structural conformance against the OpenAPI 3.x schema
//   - Return a canonical domain.SpecDocument decoupled from the parser library
//
// Implementations:
//   - cli/parser.go (kin-openapi-based implementation)
type SpecParser interface {
	// Parse loads and validates a spec from the given file path.
	// Returns SpecParseError if the file cannot be read, parsed, or validated.
	Parse(path string) (*domain.SpecDocument, error)

	// ParseFromBytes parses a spec from raw data (e.g., from stdin or git diff).
	// Returns SpecParseError if the data cannot be parsed or validated.
	ParseFromBytes(data []byte) (*domain.SpecDocument, error)
}
