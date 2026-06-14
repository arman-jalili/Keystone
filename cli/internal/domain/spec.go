package domain

import (
	"crypto/sha256"
	"encoding/hex"
	"time"
)

// ChecksumBytes computes the SHA-256 hex checksum of a byte slice.
func ChecksumBytes(data []byte) string {
	h := sha256.Sum256(data)
	return hex.EncodeToString(h[:])
}

// SpecDocument is the canonical representation of a parsed OpenAPI 3.x
// specification. It decouples the domain from the kin-openapi library so
// that only the infrastructure adapter imports the external library.
type SpecDocument struct {
	// OpenAPI version string (e.g., "3.0.3", "3.1.0").
	Version string `json:"version"`

	// Title from the spec's info.title field.
	Title string `json:"title"`

	// Version from the spec's info.version field.
	APIVersion string `json:"apiVersion"`

	// Endpoints extracted from the spec's paths.
	Endpoints []Endpoint `json:"endpoints"`

	// SHA-256 checksum of the raw spec file content.
	Checksum string `json:"checksum"`

	// ParsedAt records when this document was parsed.
	ParsedAt time.Time `json:"parsedAt"`
}

// Endpoint represents a single path+method combination in an OpenAPI spec.
type Endpoint struct {
	// Path is the URL path template (e.g., "/pets/{petId}").
	Path string `json:"path"`

	// Method is the HTTP method in lowercase (e.g., "get", "post").
	Method string `json:"method"`

	// Summary from the operation object.
	Summary string `json:"summary,omitempty"`

	// OperationID from the operation object.
	OperationID string `json:"operationId,omitempty"`

	// Deprecated is true when the operation is marked deprecated.
	Deprecated bool `json:"deprecated,omitempty"`
}

// CachedSpec is the serialised form stored in the local filesystem cache.
// See: internal/application/interfaces/cache.go
type CachedSpec struct {
	// Checksum of the original spec file (SHA-256 hex).
	Checksum string `json:"checksum"`

	// Endpoints captured at cache time.
	Endpoints []Endpoint `json:"endpoints"`

	// CachedAt is when this entry was written to the cache.
	CachedAt time.Time `json:"cachedAt"`
}
