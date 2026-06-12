package dto

import "github.com/arman-jalili/keystone-cli/internal/domain"

// AnalyzeResponse is returned after the full analysis pipeline completes.
type AnalyzeResponse struct {
	// Verdict from the local diff engine.
	Verdict domain.Verdict `json:"verdict" validate:"required"`

	// Changes discovered by the diff engine.
	Changes []domain.Change `json:"changes" validate:"required"`

	// AnalysisMs is the wall-clock time for diff computation.
	AnalysisMs int64 `json:"analysisMs" validate:"gte=0"`

	// CacheHit indicates whether a cached previous spec was found.
	CacheHit bool `json:"cacheHit"`

	// UploadSucceeded indicates whether the async upload completed.
	UploadSucceeded bool `json:"uploadSucceeded"`

	// Summary provides a short human-readable summary of the result.
	Summary string `json:"summary"`
}

// AuditUploadResponse is the response from the Keystone server after
// a successful audit upload.
type AuditUploadResponse struct {
	// Status is the server-side status (e.g., "accepted", "duplicate").
	Status string `json:"status" validate:"required"`
}
