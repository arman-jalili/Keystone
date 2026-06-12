package domain

// AnalysisContext carries metadata about the CI environment in which the
// analysis was performed. It is attached to every audit upload so the
// server can correlate results with pipeline runs.
type AnalysisContext struct {
	// SpecPath is the file path of the spec that was analysed.
	SpecPath string `json:"specPath" validate:"required"`

	// GitCommitSHA of the HEAD commit at analysis time.
	GitCommitSHA string `json:"gitCommitSha" validate:"required"`

	// BranchName where the analysis ran.
	BranchName string `json:"branchName" validate:"required"`

	// CIJobID identifies the CI pipeline job.
	CIJobID string `json:"ciJobId"`

	// CacheHit indicates whether the previous spec was found in the local cache.
	CacheHit bool `json:"cacheHit"`
}
