package interfaces

import "github.com/arman-jalili/keystone-cli/internal/domain"

// SpecCache provides read/write access to locally cached spec versions.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#local-cache
//
// Responsibilities:
//   - Persist parsed spec summaries keyed by SHA-256 checksum
//   - Retrieve previously cached specs by checksum key
//   - Handle cache misses gracefully (return CacheMissError)
//   - Detect and report corrupt cache entries (return CacheCorruptError)
//
// Storage:
//   - Default location: ~/.keystone/cache/
//   - One file per checksum: <checksum>.json
//
// Implementations:
//   - cli/cache.go (filesystem-based implementation)
type SpecCache interface {
	// Get retrieves a cached spec by its checksum key.
	// Returns:
	//   - (*domain.CachedSpec, nil) on hit
	//   - (nil, *domain.CacheMissError) on miss (not an error — diff against empty)
	//   - (nil, *domain.CacheCorruptError) when file exists but is unreadable
	Get(key string) (*domain.CachedSpec, error)

	// Set writes a spec entry to the cache, keyed by its checksum.
	// Overwrites any existing entry with the same key.
	Set(key string, spec *domain.CachedSpec) error
}
