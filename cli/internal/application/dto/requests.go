// Package dto defines input/output data transfer objects for the
// cli-orchestrator application layer.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#uploader
//
// These DTOs are the external-facing contract for data entering and
// leaving the application. They include struct tags for JSON serialisation
// and validation markers (the validate tag convention follows
// go-playground/validator).
package dto

import "github.com/arman-jalili/keystone-cli/internal/domain"

// AnalyzeRequest is the input to the analysis orchestration flow.
type AnalyzeRequest struct {
	// SpecPath is the path to the OpenAPI spec file to analyse.
	SpecPath string `json:"specPath" validate:"required,filepath"`

	// ServerURL is the optional Keystone server endpoint for audit upload.
	// If empty, the upload step is skipped.
	ServerURL string `json:"serverUrl,omitempty" validate:"omitempty,url"`

	// APIToken for authenticating with the Keystone server.
	APIToken string `json:"-"` // never serialised

	// CacheDir overrides the default cache directory (~/.keystone/cache).
	CacheDir string `json:"cacheDir,omitempty" validate:"omitempty,dirpath"`

	// GitCommitSHA identifies the commit being analysed.
	GitCommitSHA string `json:"gitCommitSha" validate:"required"`

	// BranchName identifies the branch being analysed.
	BranchName string `json:"branchName" validate:"required"`

	// CIJobID identifies the CI pipeline job.
	CIJobID string `json:"ciJobId,omitempty"`
}

// AuditUploadPayload is the JSON body sent to the Keystone server's
// audit endpoint.
//
// Contract: The JSON field names are part of the server contract and
// MUST NOT change without a coordinated server-side update.
type AuditUploadPayload struct {
	Result  *domain.DiffResult      `json:"result" validate:"required"`
	Context *domain.AnalysisContext `json:"context" validate:"required"`
}
