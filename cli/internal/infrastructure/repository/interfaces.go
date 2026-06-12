// Package repository defines persistence contracts for the cli-orchestrator.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#components
//
// These interfaces abstract the storage layer behind the SpecCache component.
// The local filesystem cache is the default implementation, but consumers
// could swap in Redis, S3, etc. as long as the interface is satisfied.
package repository

import "github.com/arman-jalili/keystone-cli/internal/domain"

// SpecRepository persists and retrieves parsed OpenAPI specifications.
//
// This is the DAO-level interface. The SpecCache (application/interfaces/cache.go)
// uses SpecRepository internally for I/O; higher-level orchestration code
// depends on SpecCache, not SpecRepository directly.
type SpecRepository interface {
	// Save persists a parsed spec and returns its checksum key.
	Save(spec *domain.SpecDocument) (checksum string, err error)

	// FindByChecksum retrieves a previously saved spec by its checksum.
	// Returns CacheMissError if not found.
	FindByChecksum(checksum string) (*domain.SpecDocument, error)

	// Delete removes a cached spec entry by checksum.
	Delete(checksum string) error

	// ListChecksums returns all checksum keys currently in the repository.
	ListChecksums() ([]string, error)
}
