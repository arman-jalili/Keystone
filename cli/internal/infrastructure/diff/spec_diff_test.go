package diff

import (
	"fmt"
	"testing"
	"time"

	"github.com/arman-jalili/keystone-cli/internal/domain"
)

func TestDiff_NoChanges(t *testing.T) {
	e := NewSpecDiffEngine()
	base := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get", OperationID: "listPets"},
		{Path: "/pets", Method: "post", OperationID: "createPet"},
	})
	target := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get", OperationID: "listPets"},
		{Path: "/pets", Method: "post", OperationID: "createPet"},
	})

	result, err := e.Diff(base, target)
	assertNil(t, err)
	assertEqual(t, "verdict", domain.VerdictPass, result.Verdict)
	assertEqual(t, "changes count", 0, len(result.Changes))
}

func TestDiff_AddedEndpoint(t *testing.T) {
	e := NewSpecDiffEngine()
	base := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get"},
	})
	target := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get"},
		{Path: "/pets", Method: "post", Summary: "Create a pet"},
	})

	result, err := e.Diff(base, target)
	assertNil(t, err)
	assertEqual(t, "verdict", domain.VerdictPass, result.Verdict)
	assertEqual(t, "changes count", 1, len(result.Changes))
	assertEqual(t, "severity", domain.SeverityAdditive, result.Changes[0].Severity)
	assertEqual(t, "path", "/pets", result.Changes[0].Path)
	assertEqual(t, "method", "post", result.Changes[0].Method)
}

func TestDiff_RemovedEndpoint(t *testing.T) {
	e := NewSpecDiffEngine()
	base := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get"},
		{Path: "/pets/{id}", Method: "get"},
	})
	target := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get"},
	})

	result, err := e.Diff(base, target)
	assertNil(t, err)
	assertEqual(t, "verdict", domain.VerdictBreaking, result.Verdict)
	assertEqual(t, "changes count", 1, len(result.Changes))
	assertEqual(t, "severity", domain.SeverityBreaking, result.Changes[0].Severity)
	assertEqual(t, "path", "/pets/{id}", result.Changes[0].Path)
	assertEqual(t, "method", "get", result.Changes[0].Method)
}

func TestDiff_BreakingTakesPrecedence(t *testing.T) {
	e := NewSpecDiffEngine()
	base := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get"},
		{Path: "/old", Method: "delete"},
	})
	target := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get"},
		{Path: "/new", Method: "post"},
	})

	result, err := e.Diff(base, target)
	assertNil(t, err)
	assertEqual(t, "verdict", domain.VerdictBreaking, result.Verdict)
	assertEqual(t, "changes count", 2, len(result.Changes))
}

func TestDiff_NilBase(t *testing.T) {
	e := NewSpecDiffEngine()
	target := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get"},
		{Path: "/pets", Method: "post"},
	})

	result, err := e.Diff(nil, target)
	assertNil(t, err)
	assertEqual(t, "verdict", domain.VerdictPass, result.Verdict)
	assertEqual(t, "changes count", 2, len(result.Changes))
	for _, c := range result.Changes {
		assertEqual(t, "severity", domain.SeverityAdditive, c.Severity)
	}
}

func TestDiff_NilBaseWithNilEndpoints(t *testing.T) {
	e := NewSpecDiffEngine()
	target := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get"},
	})

	result, err := e.Diff(&domain.SpecDocument{}, target)
	assertNil(t, err)
	assertEqual(t, "verdict", domain.VerdictPass, result.Verdict)
	assertEqual(t, "changes count", 1, len(result.Changes))
}

func TestDiff_DeprecationWarning(t *testing.T) {
	e := NewSpecDiffEngine()
	base := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get", Deprecated: false},
	})
	target := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get", Deprecated: true},
	})

	result, err := e.Diff(base, target)
	assertNil(t, err)
	assertEqual(t, "verdict", domain.VerdictWarning, result.Verdict)
	assertEqual(t, "changes count", 1, len(result.Changes))
	assertEqual(t, "severity", domain.SeverityWarning, result.Changes[0].Severity)
}

func TestDiff_WarningAndAdditive(t *testing.T) {
	e := NewSpecDiffEngine()
	base := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get"},
	})
	target := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get", Deprecated: true},
		{Path: "/health", Method: "get"},
	})

	result, err := e.Diff(base, target)
	assertNil(t, err)
	assertEqual(t, "verdict", domain.VerdictWarning, result.Verdict)
	assertEqual(t, "changes count", 2, len(result.Changes))
}

func TestDiff_NilTarget(t *testing.T) {
	e := NewSpecDiffEngine()
	base := sampleSpec([]domain.Endpoint{
		{Path: "/pets", Method: "get"},
	})

	_, err := e.Diff(base, nil)
	if err == nil {
		t.Fatal("expected error for nil target")
	}
}

func TestDiff_AnalysisMsIsNonZero(t *testing.T) {
	e := NewSpecDiffEngine()
	base := sampleSpec([]domain.Endpoint{
		{Path: "/a", Method: "get"},
		{Path: "/b", Method: "post"},
		{Path: "/c", Method: "put"},
	})
	target := sampleSpec([]domain.Endpoint{
		{Path: "/a", Method: "get"},
	})

	result, err := e.Diff(base, target)
	assertNil(t, err)
	if result.AnalysisMs < 0 {
		t.Errorf("expected non-negative AnalysisMs, got %d", result.AnalysisMs)
	}
}

func TestDiff_LargeSpecPerformance(t *testing.T) {
	e := NewSpecDiffEngine()

	// Build a spec with 200 endpoints
	baseEndpoints := make([]domain.Endpoint, 0, 200)
	targetEndpoints := make([]domain.Endpoint, 0, 200)
	for i := 0; i < 100; i++ {
		p := fmt.Sprintf("/api/v%d", i)
		baseEndpoints = append(baseEndpoints,
			domain.Endpoint{Path: p, Method: "get"},
			domain.Endpoint{Path: p, Method: "post"},
		)
		targetEndpoints = append(targetEndpoints,
			domain.Endpoint{Path: p, Method: "get"},
			domain.Endpoint{Path: p, Method: "post"},
		)
	}

	base := sampleSpec(baseEndpoints)
	target := sampleSpec(targetEndpoints)

	result, err := e.Diff(base, target)
	assertNil(t, err)
	assertEqual(t, "changes count", 0, len(result.Changes))
	assertEqual(t, "verdict", domain.VerdictPass, result.Verdict)
}

// ---------- helpers ----------

func sampleSpec(endpoints []domain.Endpoint) *domain.SpecDocument {
	return &domain.SpecDocument{
		Title:      "Test Spec",
		Version:    "3.0.3",
		APIVersion: "1.0.0",
		Endpoints:  endpoints,
		Checksum:   "test-checksum",
		ParsedAt:   time.Now(),
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
