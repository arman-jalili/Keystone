// Package event defines domain event payloads that the cli-orchestrator emits.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#components
//
// Events are emitted by the application layer after significant actions
// complete. Infrastructure adapters (event publishers) translate these
// into the appropriate transport (e.g., stdout log line, structured log,
// HTTP callback, etc.).
package event

import "github.com/arman-jalili/keystone-cli/internal/domain"

// --- Event type registry ---

// Type uniquely identifies a domain event kind.
// Consumers use this to route events to the correct handler.
type Type string

const (
	TypeSpecParsed         Type = "spec.parsed"
	TypeDiffComputed       Type = "diff.computed"
	TypeUploadInitiated    Type = "upload.initiated"
	TypeUploadCompleted    Type = "upload.completed"
	TypeUploadFailed       Type = "upload.failed"
	TypeCacheHit           Type = "cache.hit"
	TypeCacheMiss          Type = "cache.miss"
)

// --- Event payloads ---

// Event is the envelope wrapper for all domain events.
// Every event carries a type discriminator and the correlation ID
// so consumers can trace a single analysis run across multiple events.
type Event struct {
	// Type discriminator — see Type constants above.
	Type Type `json:"type" validate:"required"`

	// CorrelationID ties all events from a single CLI invocation together.
	CorrelationID string `json:"correlationId" validate:"required"`

	// Payload is the typed event data. Consumers switch on Type.
	Payload interface{} `json:"payload" validate:"required"`
}

// SpecParsedPayload is emitted after a spec file is successfully parsed.
type SpecParsedPayload struct {
	Path     string `json:"path" validate:"required"`
	Checksum string `json:"checksum" validate:"required"`
	EndpointCount int `json:"endpointCount" validate:"gte=0"`
}

// DiffComputedPayload is emitted after a diff has been computed.
type DiffComputedPayload struct {
	Verdict    domain.Verdict `json:"verdict" validate:"required"`
	ChangeCount int           `json:"changeCount" validate:"gte=0"`
	AnalysisMs int64          `json:"analysisMs" validate:"gte=0"`
}

// UploadInitiatedPayload is emitted when an upload attempt begins.
type UploadInitiatedPayload struct {
	ServerURL string `json:"serverUrl" validate:"required"`
	Attempt   int    `json:"attempt" validate:"gte=1"`
}

// UploadCompletedPayload is emitted when an upload succeeds.
type UploadCompletedPayload struct {
	StatusCode int   `json:"statusCode" validate:"required"`
	DurationMs int64 `json:"durationMs" validate:"gte=0"`
}

// UploadFailedPayload is emitted when all upload retry attempts are exhausted.
type UploadFailedPayload struct {
	Attempts int    `json:"attempts" validate:"gte=1"`
	LastErr  string `json:"lastError" validate:"required"`
}
