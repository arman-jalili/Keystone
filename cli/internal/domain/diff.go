package domain

// Verdict summarises the overall impact of a spec diff.
//
// Contract: These values are serialised in the JSON upload payload and
// consumed by the Keystone server. Do not rename or remove without
// a coordinated server-side change.
type Verdict string

const (
	VerdictPass     Verdict = "PASS"     // No breaking or warning changes
	VerdictBreaking Verdict = "BREAKING" // At least one breaking change
	VerdictWarning  Verdict = "WARNING"  // Only non-breaking warnings
)

// Severity classifies an individual change.
type Severity string

const (
	SeverityAdditive Severity = "ADDITIVE" // New path/operation added
	SeverityBreaking Severity = "BREAKING" // Existing contract broken
	SeverityWarning  Severity = "WARNING"  // Deprecation, format change, etc.
	SeverityInfo     Severity = "INFO"     // Informational (e.g., description update)
)

// Change represents a single difference between two versions of a spec.
type Change struct {
	// Severity of this change.
	Severity Severity `json:"severity" validate:"required,oneof=ADDITIVE BREAKING WARNING INFO"`

	// Path is the URL path template affected.
	Path string `json:"path" validate:"required"`

	// Method is the HTTP method in lowercase, empty if path-level.
	Method string `json:"method,omitempty"`

	// Human-readable description of the change.
	Description string `json:"description" validate:"required"`
}

// DiffResult is the output of the local diff engine.
type DiffResult struct {
	// Verdict summarises the overall impact.
	Verdict Verdict `json:"verdict" validate:"required,oneof=PASS BREAKING WARNING"`

	// Individual changes that produced this verdict.
	Changes []Change `json:"changes" validate:"required"`

	// AnalysisMs is the wall-clock time spent computing the diff, in milliseconds.
	AnalysisMs int64 `json:"analysisMs" validate:"gte=0"`
}
