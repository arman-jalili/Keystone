// Package http defines the HTTP client-side API contracts for communicating
// with the Keystone Spring Boot server.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#uploader
//
// These constants and types define the contract for the server's audit
// ingestion endpoint. Both the CLI uploader and the server controller
// must agree on these values.
package http

// --- Endpoint contracts ---

// ServerAPIBasePath is the base path for the Keystone server API.
const ServerAPIBasePath = "/api/v1"

// AuditEndpoint is the server endpoint for audit record uploads.
//
//   POST /api/v1/ingestion/audit
//
// Request body: AuditUploadPayload (application/json)
// Success response: 202 Accepted
//   Body: { "status": "accepted" }
// Error responses:
//   400 Bad Request  — malformed payload
//   401 Unauthorized — missing or invalid API token
//   413 Payload Too Large — payload exceeds server limit
//   429 Too Many Requests — rate limited
//   500 Internal Server Error — server-side failure
const AuditEndpoint = "/api/v1/ingestion/audit"

// HealthEndpoint is the server health check endpoint (used for connectivity
// validation before upload).
//
//   GET /api/v1/health
// Success response: 200 OK
const HealthEndpoint = "/api/v1/health"

// --- HTTP header contracts ---

const (
	// HeaderAuthorization is the standard HTTP Authorization header.
	HeaderAuthorization = "Authorization"

	// HeaderContentType is the standard HTTP Content-Type header.
	HeaderContentType = "Content-Type"

	// HeaderRequestID is the custom header for request correlation.
	// The server echoes this in the response for tracing.
	HeaderRequestID = "X-Request-ID"
)

const (
	// ContentTypeJSON is the MIME type for JSON payloads.
	ContentTypeJSON = "application/json"

	// AuthSchemeBearer is the Bearer token prefix for the Authorization header.
	AuthSchemeBearer = "Bearer"
)

// --- Retry contract ---

const (
	// MaxUploadRetries is the maximum number of upload attempts.
	MaxUploadRetries = 3

	// UploadTimeout is the per-attempt HTTP client timeout.
	UploadTimeout = 10 // seconds

	// RetryBaseDelay is the initial backoff delay for retries.
	RetryBaseDelay = 1 // second
)

// --- Error response contract ---

// ErrorResponse is the standard error body returned by the Keystone server
// for non-2xx responses.
type ErrorResponse struct {
	// Code is a machine-readable error code (e.g., "VALIDATION_ERROR").
	Code string `json:"code" validate:"required"`

	// Message is a human-readable error description.
	Message string `json:"message" validate:"required"`

	// RequestID echoes the X-Request-ID header for tracing.
	RequestID string `json:"requestId,omitempty"`
}
