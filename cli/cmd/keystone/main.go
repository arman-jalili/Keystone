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
	"fmt"
	"os"

	"github.com/arman-jalili/keystone-cli/internal/domain"
)

// main is the CLI entry point. Its implementation will be provided by the
// cli-main component implementation issue. This placeholder ensures the
// project compiles while keeping the contract frozen.
func main() {
	fmt.Fprintln(os.Stderr, "keystone-cli: not yet implemented — this is a contract placeholder")
	os.Exit(int(domain.ExitError))
}
