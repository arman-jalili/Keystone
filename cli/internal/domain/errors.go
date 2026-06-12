package domain

import "fmt"

// --- SpecParseError ---

// SpecParseError indicates that the spec file could not be parsed or
// validated. This is a terminal error that should cause the CLI to
// exit with ExitError (3).
type SpecParseError struct {
	Path    string
	Details []error
}

func (e *SpecParseError) Error() string {
	return fmt.Sprintf("spec parse failed for %q: %v", e.Path, e.Details)
}

func (e *SpecParseError) Unwrap() []error { return e.Details }

// --- CacheMissError ---

// CacheMissError is returned when a cached spec version is not found.
// It is NOT a terminal error — the engine should diff against an empty
// baseline so all changes appear additive.
type CacheMissError struct {
	Key string
}

func (e *CacheMissError) Error() string {
	return fmt.Sprintf("cache miss for key %q (diffing against empty baseline)", e.Key)
}

// CacheCorruptError is returned when the cached file exists but cannot
// be deserialised. The caller should discard the entry and treat it
// as a miss.
type CacheCorruptError struct {
	Key   string
	Cause error
}

func (e *CacheCorruptError) Error() string {
	return fmt.Sprintf("cache entry %q is corrupt: %v", e.Key, e.Cause)
}

func (e *CacheCorruptError) Unwrap() error { return e.Cause }

// --- UploadFailedError ---

// UploadFailedError is returned when the async upload to the Keystone
// server did not succeed after all retry attempts. This is a non-terminal
// warning — the CLI exit code is determined by the diff verdict alone.
type UploadFailedError struct {
	Attempts int
	LastErr  error
}

func (e *UploadFailedError) Error() string {
	return fmt.Sprintf("upload failed after %d attempts: %v", e.Attempts, e.LastErr)
}

func (e *UploadFailedError) Unwrap() error { return e.LastErr }
