package interfaces

import "github.com/arman-jalili/keystone-cli/internal/application/dto"

// Orchestrator is the top-level application service that coordinates
// the full analysis pipeline: parse → cache → diff → upload.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#cli-main
//
// Responsibilities:
//   - Accept an AnalyzeRequest from the CLI entry point
//   - Invoke SpecParser to parse the current spec
//   - Invoke SpecCache to retrieve the previous cached version
//   - Invoke DiffEngine to compute the diff
//   - Cache the current spec for future runs
//   - Invoke Uploader to send results to the server (async, best-effort)
//   - Return an AnalyzeResponse with the final verdict
type Orchestrator interface {
	// Analyze runs the full analysis pipeline and returns the result.
	// This is the single entry point for the application logic.
	Analyze(req *dto.AnalyzeRequest) (*dto.AnalyzeResponse, error)
}
