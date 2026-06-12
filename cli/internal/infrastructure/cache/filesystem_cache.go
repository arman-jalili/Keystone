// Package cache provides a filesystem-based implementation of the SpecCache interface.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#local-cache
//
// Storage layout:
//
//	~/.keystone/cache/
//	├── <checksum>.json
//	├── <checksum>.json
//	└── ...
//
// Each file contains a JSON-serialised domain.CachedSpec.
// Files are named by their SHA-256 checksum for O(1) lookup.
package cache

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"

	"github.com/arman-jalili/keystone-cli/internal/domain"
)

// FilesystemCache implements interfaces.SpecCache using the local filesystem.
type FilesystemCache struct {
	// CacheDir is the directory where cached spec files are stored.
	// Defaults to ~/.keystone/cache/.
	CacheDir string
}

// NewFilesystemCache creates a new filesystem cache.
// If cacheDir is empty, it defaults to ~/.keystone/cache/.
func NewFilesystemCache(cacheDir string) (*FilesystemCache, error) {
	if cacheDir == "" {
		home, err := os.UserHomeDir()
		if err != nil {
			return nil, fmt.Errorf("cannot determine home directory: %w", err)
		}
		cacheDir = filepath.Join(home, ".keystone", "cache")
	}

	// Ensure the cache directory exists.
	if err := os.MkdirAll(cacheDir, 0755); err != nil {
		return nil, fmt.Errorf("cannot create cache directory %q: %w", cacheDir, err)
	}

	return &FilesystemCache{CacheDir: cacheDir}, nil
}

// Get retrieves a cached spec by its checksum key.
func (c *FilesystemCache) Get(key string) (*domain.CachedSpec, error) {
	path := filepath.Join(c.CacheDir, key+".json")

	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, &domain.CacheMissError{Key: key}
		}
		return nil, &domain.CacheCorruptError{
			Key:   key,
			Cause: fmt.Errorf("read error: %w", err),
		}
	}

	var spec domain.CachedSpec
	if err := json.Unmarshal(data, &spec); err != nil {
		return nil, &domain.CacheCorruptError{
			Key:   key,
			Cause: fmt.Errorf("unmarshal error: %w", err),
		}
	}

	return &spec, nil
}

// Set writes a spec entry to the cache, keyed by its checksum.
func (c *FilesystemCache) Set(key string, spec *domain.CachedSpec) error {
	data, err := json.Marshal(spec)
	if err != nil {
		return fmt.Errorf("cache marshal error: %w", err)
	}

	path := filepath.Join(c.CacheDir, key+".json")
	if err := os.WriteFile(path, data, 0644); err != nil {
		return fmt.Errorf("cache write error for %q: %w", key, err)
	}

	return nil
}

// Delete removes a cached spec entry by checksum.
func (c *FilesystemCache) Delete(key string) error {
	path := filepath.Join(c.CacheDir, key+".json")
	if err := os.Remove(path); err != nil {
		if os.IsNotExist(err) {
			return nil // idempotent — already gone
		}
		return fmt.Errorf("cache delete error for %q: %w", key, err)
	}
	return nil
}

// Clear removes all cached spec entries from the cache directory.
func (c *FilesystemCache) Clear() error {
	entries, err := os.ReadDir(c.CacheDir)
	if err != nil {
		return fmt.Errorf("cache clear: read dir error: %w", err)
	}

	for _, entry := range entries {
		if !entry.IsDir() && filepath.Ext(entry.Name()) == ".json" {
			if err := os.Remove(filepath.Join(c.CacheDir, entry.Name())); err != nil {
				return fmt.Errorf("cache clear: remove %q error: %w", entry.Name(), err)
			}
		}
	}

	return nil
}
