// Package uploader provides an HTTP-based implementation of the Uploader interface.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#uploader
//
// This adapter sends LocalDiffResult payloads to the Keystone Spring Boot
// server's audit endpoint with retry and exponential backoff.
package uploader

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"time"

	"github.com/arman-jalili/keystone-cli/internal/application/dto"
	httppkg "github.com/arman-jalili/keystone-cli/internal/interfaces/http"
	"github.com/arman-jalili/keystone-cli/internal/domain"
)

// HTTPUploader implements interfaces.Uploader by sending HTTP POST requests
// to the Keystone server's audit endpoint.
type HTTPUploader struct {
	serverURL string
	apiToken  string
	client    *http.Client
}

// NewHTTPUploader creates a new HTTP uploader.
func NewHTTPUploader(serverURL string, apiToken string) *HTTPUploader {
	return &HTTPUploader{
		serverURL: serverURL,
		apiToken:  apiToken,
		client: &http.Client{
			Timeout: httppkg.UploadTimeout * time.Second,
		},
	}
}

// nonRetryableError is returned when the server returns a 4xx error
// (except 429) that should not be retried.
type nonRetryableError struct {
	Code   int
	Detail string
}

func (e *nonRetryableError) Error() string {
	return fmt.Sprintf("non-retryable upload error (HTTP %d): %s", e.Code, e.Detail)
}

// Upload transmits the diff result and analysis context to the server.
func (u *HTTPUploader) Upload(result *domain.DiffResult, ctx *domain.AnalysisContext) error {
	if u.serverURL == "" {
		return fmt.Errorf("upload: server URL is empty")
	}

	payload := dto.AuditUploadPayload{
		Result:  result,
		Context: ctx,
	}

	body, err := json.Marshal(payload)
	if err != nil {
		return fmt.Errorf("upload: marshal error: %w", err)
	}

	endpoint := u.serverURL + httppkg.AuditEndpoint
	var lastErr error

	for attempt := 1; attempt <= httppkg.MaxUploadRetries; attempt++ {
		err := u.doPost(endpoint, body)
		if err == nil {
			return nil // success
		}

		// Don't retry client errors (except 429 Too Many Requests)
		var nre *nonRetryableError
		if errors.As(err, &nre) {
			return err // return immediately, no retry
		}

		lastErr = err

		if attempt < httppkg.MaxUploadRetries {
			backoff := time.Duration(1<<uint(attempt-1)) * time.Second
			time.Sleep(backoff)
		}
	}

	return &domain.UploadFailedError{
		Attempts: httppkg.MaxUploadRetries,
		LastErr:  lastErr,
	}
}

// doPost performs a single HTTP POST request.
func (u *HTTPUploader) doPost(endpoint string, body []byte) error {
	req, err := http.NewRequest(http.MethodPost, endpoint, bytes.NewReader(body))
	if err != nil {
		return fmt.Errorf("create request: %w", err)
	}

	req.Header.Set(httppkg.HeaderContentType, httppkg.ContentTypeJSON)
	if u.apiToken != "" {
		req.Header.Set(httppkg.HeaderAuthorization, httppkg.AuthSchemeBearer+" "+u.apiToken)
	}

	resp, err := u.client.Do(req)
	if err != nil {
		return fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 200 && resp.StatusCode < 300 {
		return nil // success
	}

	// Read error response body for diagnostic info
	respBody, _ := io.ReadAll(resp.Body)
	errMsg := fmt.Sprintf("server returned %d", resp.StatusCode)
	if len(respBody) > 0 {
		errMsg = fmt.Sprintf("%s: %s", errMsg, string(respBody))
	}

	// 4xx client errors (except 429) are non-retryable
	if resp.StatusCode >= 400 && resp.StatusCode < 500 && resp.StatusCode != http.StatusTooManyRequests {
		return &nonRetryableError{Code: resp.StatusCode, Detail: errMsg}
	}

	return fmt.Errorf("retryable error: %s", errMsg)
}
