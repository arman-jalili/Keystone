package cache

import (
	"errors"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/arman-jalili/keystone-cli/internal/domain"
)

func TestNewFilesystemCache_DefaultDir(t *testing.T) {
	c, err := NewFilesystemCache("")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	home, _ := os.UserHomeDir()
	expected := filepath.Join(home, ".keystone", "cache")
	if c.CacheDir != expected {
		t.Errorf("expected cache dir %q, got %q", expected, c.CacheDir)
	}

	// Verify directory was created
	if _, err := os.Stat(expected); os.IsNotExist(err) {
		t.Error("cache directory was not created")
	}

	// Cleanup
	os.RemoveAll(filepath.Join(home, ".keystone"))
}

func TestNewFilesystemCache_CustomDir(t *testing.T) {
	dir := t.TempDir()
	c, err := NewFilesystemCache(dir)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if c.CacheDir != dir {
		t.Errorf("expected cache dir %q, got %q", dir, c.CacheDir)
	}
}

func TestSetAndGet(t *testing.T) {
	dir := t.TempDir()
	c, err := NewFilesystemCache(dir)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	key := "abc123checksum"
	spec := &domain.CachedSpec{
		Checksum: key,
		Endpoints: []domain.Endpoint{
			{Path: "/pets", Method: "get", Summary: "List pets", OperationID: "listPets"},
		},
		CachedAt: time.Now().Truncate(time.Second),
	}

	assertNil(t, c.Set(key, spec), "Set")

	got, err := c.Get(key)
	assertNil(t, err, "Get")

	assertEqual(t, "checksum", spec.Checksum, got.Checksum)
	assertEqual(t, "endpoints count", 1, len(got.Endpoints))
	assertEqual(t, "operationId", "listPets", got.Endpoints[0].OperationID)

	if !got.CachedAt.Equal(spec.CachedAt) {
		t.Errorf("cachedAt: expected %v, got %v", spec.CachedAt, got.CachedAt)
	}
}

func TestGet_Miss(t *testing.T) {
	dir := t.TempDir()
	c, err := NewFilesystemCache(dir)
	assertNil(t, err, "NewFilesystemCache")

	_, err = c.Get("nonexistent_key")
	assertErrorType[*domain.CacheMissError](t, err, "expected CacheMissError on miss")
}

func TestGet_CorruptFile(t *testing.T) {
	dir := t.TempDir()
	c, err := NewFilesystemCache(dir)
	assertNil(t, err, "NewFilesystemCache")

	key := "corrupt_key"
	path := filepath.Join(dir, key+".json")
	if err := os.WriteFile(path, []byte("not valid json"), 0644); err != nil {
		t.Fatalf("failed to write corrupt file: %v", err)
	}

	_, err = c.Get(key)
	assertErrorType[*domain.CacheCorruptError](t, err, "expected CacheCorruptError for invalid JSON")
}

func TestSet_Overwrite(t *testing.T) {
	dir := t.TempDir()
	c, err := NewFilesystemCache(dir)
	assertNil(t, err, "NewFilesystemCache")

	key := "same_key"
	first := &domain.CachedSpec{
		Checksum:  key,
		Endpoints: []domain.Endpoint{{Path: "/v1", Method: "get"}},
		CachedAt:  time.Now().Truncate(time.Second),
	}
	second := &domain.CachedSpec{
		Checksum:  key,
		Endpoints: []domain.Endpoint{{Path: "/v2", Method: "post"}},
		CachedAt:  time.Now().Truncate(time.Second),
	}

	assertNil(t, c.Set(key, first), "first Set")
	assertNil(t, c.Set(key, second), "second Set")

	got, err := c.Get(key)
	assertNil(t, err, "Get after overwrite")
	assertEqual(t, "overwritten path", "/v2", got.Endpoints[0].Path)
}

func TestDelete(t *testing.T) {
	dir := t.TempDir()
	c, err := NewFilesystemCache(dir)
	assertNil(t, err, "NewFilesystemCache")

	key := "delete_me"
	spec := &domain.CachedSpec{Checksum: key, CachedAt: time.Now()}
	assertNil(t, c.Set(key, spec), "Set")
	assertNil(t, c.Delete(key), "Delete")

	_, err = c.Get(key)
	assertErrorType[*domain.CacheMissError](t, err, "expected CacheMissError after delete")
}

func TestDelete_Idempotent(t *testing.T) {
	dir := t.TempDir()
	c, err := NewFilesystemCache(dir)
	assertNil(t, err, "NewFilesystemCache")

	// Deleting a nonexistent key should not error
	assertNil(t, c.Delete("never_existed"), "idempotent delete")
}

func TestClear(t *testing.T) {
	dir := t.TempDir()
	c, err := NewFilesystemCache(dir)
	assertNil(t, err, "NewFilesystemCache")

	keys := []string{"key1", "key2", "key3"}
	for _, k := range keys {
		assertNil(t, c.Set(k, &domain.CachedSpec{Checksum: k, CachedAt: time.Now()}), "Set "+k)
	}

	assertNil(t, c.Clear(), "Clear")

	for _, k := range keys {
		_, err := c.Get(k)
		assertErrorType[*domain.CacheMissError](t, err, "expected CacheMissError after clear for "+k)
	}
}

// ---------- helpers ----------

func assertNil(t *testing.T, err error, msg string) {
	t.Helper()
	if err != nil {
		t.Fatalf("%s: unexpected error: %v", msg, err)
	}
}

func assertEqual[T comparable](t *testing.T, name string, expected, actual T) {
	t.Helper()
	if expected != actual {
		t.Errorf("%s: expected %v, got %v", name, expected, actual)
	}
}

func assertErrorType[T error](t *testing.T, err error, msg string) {
	t.Helper()
	if err == nil {
		t.Fatalf("%s: expected error, got nil", msg)
	}
	var target T
	if !errors.As(err, &target) {
		t.Fatalf("%s: expected %T, got %T: %v", msg, target, err, err)
	}
}
