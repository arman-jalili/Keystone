package uploader

import (
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/arman-jalili/keystone-cli/internal/application/dto"
	"github.com/arman-jalili/keystone-cli/internal/domain"
)

func TestUpload_Success(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assertEqual(t, "method", http.MethodPost, r.Method)
		assertEqual(t, "path", "/api/v1/ingestion/audit", r.URL.Path)
		assertEqual(t, "content-type", "application/json", r.Header.Get("Content-Type"))
		assertEqual(t, "authorization", "Bearer test-token", r.Header.Get("Authorization"))

		var payload dto.AuditUploadPayload
		if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
			t.Errorf("invalid payload: %v", err)
		}
		if payload.Result == nil {
			t.Error("expected non-nil result in payload")
		}
		if payload.Context == nil {
			t.Error("expected non-nil context in payload")
		}

		w.WriteHeader(http.StatusAccepted)
		json.NewEncoder(w).Encode(map[string]string{"status": "accepted"})
	}))
	defer server.Close()

	u := NewHTTPUploader(server.URL, "test-token")
	err := u.Upload(sampleResult(), sampleContext())
	assertNil(t, err)
}

func TestUpload_SuccessNoAuth(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("Authorization") != "" {
			t.Error("expected no Authorization header when token is empty")
		}
		w.WriteHeader(http.StatusAccepted)
	}))
	defer server.Close()

	u := NewHTTPUploader(server.URL, "")
	err := u.Upload(sampleResult(), sampleContext())
	assertNil(t, err)
}

func TestUpload_ServerErrorRetry(t *testing.T) {
	attempts := 0
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		attempts++
		w.WriteHeader(http.StatusInternalServerError)
	}))
	defer server.Close()

	u := NewHTTPUploader(server.URL, "token")
	err := u.Upload(sampleResult(), sampleContext())
	assertUploadFailed(t, err)
	if attempts != 3 {
		t.Errorf("expected 3 attempts (retry), got %d", attempts)
	}
}

func TestUpload_ClientErrorNoRetry(t *testing.T) {
	attempts := 0
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		attempts++
		w.WriteHeader(http.StatusBadRequest)
	}))
	defer server.Close()

	u := NewHTTPUploader(server.URL, "token")
	err := u.Upload(sampleResult(), sampleContext())
	if err == nil {
		t.Fatal("expected error for 400")
	}
	if attempts != 1 {
		t.Errorf("expected 1 attempt (no retry on 400), got %d", attempts)
	}
}

func TestUpload_TooManyRequestsRetries(t *testing.T) {
	attempts := 0
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		attempts++
		w.WriteHeader(http.StatusTooManyRequests)
	}))
	defer server.Close()

	u := NewHTTPUploader(server.URL, "token")
	_ = u.Upload(sampleResult(), sampleContext())
	if attempts != 3 {
		t.Errorf("expected 3 attempts (retry on 429), got %d", attempts)
	}
}

func TestUpload_EventuallySucceedsAfterRetry(t *testing.T) {
	attempts := 0
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		attempts++
		if attempts < 3 {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}
		w.WriteHeader(http.StatusAccepted)
	}))
	defer server.Close()

	u := NewHTTPUploader(server.URL, "token")
	err := u.Upload(sampleResult(), sampleContext())
	assertNil(t, err)
	assertEqual(t, "attempts", 3, attempts)
}

func TestUpload_EmptyServerURL(t *testing.T) {
	u := NewHTTPUploader("", "token")
	err := u.Upload(sampleResult(), sampleContext())
	if err == nil {
		t.Fatal("expected error for empty server URL")
	}
}

func TestUpload_NetworkError(t *testing.T) {
	u := NewHTTPUploader("http://localhost:1", "token")
	err := u.Upload(sampleResult(), sampleContext())
	assertUploadFailed(t, err)
}

// ---------- helpers ----------

func sampleResult() *domain.DiffResult {
	return &domain.DiffResult{
		Verdict: domain.VerdictPass,
		Changes: []domain.Change{
			{
				Severity:    domain.SeverityAdditive,
				Path:        "/pets",
				Method:      "get",
				Description: "Added GET /pets endpoint",
			},
		},
		AnalysisMs: 5,
	}
}

func sampleContext() *domain.AnalysisContext {
	return &domain.AnalysisContext{
		SpecPath:     "./openapi.yaml",
		GitCommitSHA: "abc123",
		BranchName:   "feature/test",
		CIJobID:      "job-42",
		CacheHit:     true,
	}
}

func assertNil(t *testing.T, err error) {
	t.Helper()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func assertEqual[T comparable](t *testing.T, name string, expected, actual T) {
	t.Helper()
	if expected != actual {
		t.Errorf("%s: expected %v, got %v", name, expected, actual)
	}
}

func assertUploadFailed(t *testing.T, err error) {
	t.Helper()
	if err == nil {
		t.Fatal("expected error, got nil")
	}
	var ufe *domain.UploadFailedError
	if !errors.As(err, &ufe) {
		t.Fatalf("expected *domain.UploadFailedError, got %T: %v", err, err)
	}
}
