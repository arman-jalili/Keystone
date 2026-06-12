package interfaces

import "github.com/arman-jalili/keystone-cli/internal/domain"

// Uploader sends the local diff result to the Keystone Spring Boot server
// for audit persistence.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#uploader
//
// Responsibilities:
//   - Serialise the DiffResult and AnalysisContext into the audit payload format
//   - POST to the server's /api/v1/ingestion/audit endpoint
//   - Authenticate via Bearer token from KEYSTONE_API_TOKEN env var
//   - Retry on transient failures (up to 3 attempts with exponential backoff)
//   - Fail gracefully — the CLI exit code is determined by the diff verdict,
//     not the upload result. Upload errors are logged as warnings.
//
// Implementations:
//   - cli/uploader.go (HTTP-based implementation)
type Uploader interface {
	// Upload transmits the diff result and analysis context to the server.
	// Returns:
	//   - nil on success (any 2xx status code)
	//   - *domain.UploadFailedError after exhausting retries
	//   - error for configuration issues (missing URL, invalid token format)
	Upload(result *domain.DiffResult, ctx *domain.AnalysisContext) error
}
