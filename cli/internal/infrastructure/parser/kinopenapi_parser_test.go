package parser

import (
	"errors"
	"os"
	"path/filepath"
	"testing"

	"github.com/arman-jalili/keystone-cli/internal/domain"
)

var testdataDir string

func init() {
	testdataDir = filepath.Join("testdata")
}

func TestParse_ValidYAML(t *testing.T) {
	p := NewKinOpenAPIParser()
	path := filepath.Join(testdataDir, "valid_openapi.yaml")

	doc, err := p.Parse(path)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if doc == nil {
		t.Fatal("expected non-nil document")
	}

	assertEqual(t, "title", "Pet Store API", doc.Title)
	assertEqual(t, "apiVersion", "1.0.0", doc.APIVersion)
	assertEqual(t, "OpenAPI version", "3.0.3", doc.Version)
	assertEqual(t, "endpoint count", 3, len(doc.Endpoints))

	if doc.Checksum == "" {
		t.Error("expected non-empty checksum")
	}

	foundListPets := false
	foundGetPet := false
	for _, ep := range doc.Endpoints {
		switch ep.OperationID {
		case "listPets":
			foundListPets = true
			assertEqual(t, "listPets.method", "get", ep.Method)
			assertEqual(t, "listPets.path", "/pets", ep.Path)
		case "getPet":
			foundGetPet = true
			if !ep.Deprecated {
				t.Error("expected getPet to be deprecated")
			}
		}
	}
	if !foundListPets {
		t.Error("expected to find listPets endpoint")
	}
	if !foundGetPet {
		t.Error("expected to find getPet endpoint")
	}
}

func TestParse_ValidJSON(t *testing.T) {
	p := NewKinOpenAPIParser()
	path := filepath.Join(testdataDir, "valid_openapi.json")

	doc, err := p.Parse(path)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	assertEqual(t, "title", "Pet Store API", doc.Title)
	assertEqual(t, "endpoint count", 2, len(doc.Endpoints))
}

func TestParse_InvalidSpec(t *testing.T) {
	p := NewKinOpenAPIParser()
	path := filepath.Join(testdataDir, "invalid_openapi.yaml")

	_, err := p.Parse(path)
	if err == nil {
		t.Fatal("expected error for invalid spec, got nil")
	}

	var specErr *domain.SpecParseError
	if !errors.As(err, &specErr) {
		t.Errorf("expected *domain.SpecParseError, got %T", err)
	}
}

func TestParse_FileNotFound(t *testing.T) {
	p := NewKinOpenAPIParser()

	_, err := p.Parse("/nonexistent/path.yaml")
	if err == nil {
		t.Fatal("expected error for nonexistent file, got nil")
	}

	var specErr *domain.SpecParseError
	if !errors.As(err, &specErr) {
		t.Errorf("expected *domain.SpecParseError, got %T", err)
	}
}

func TestParseFromBytes_ValidYAML(t *testing.T) {
	data, err := os.ReadFile(filepath.Join(testdataDir, "valid_openapi.yaml"))
	if err != nil {
		t.Fatalf("failed to read test fixture: %v", err)
	}

	p := NewKinOpenAPIParser()
	doc, err := p.ParseFromBytes(data)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	assertEqual(t, "title", "Pet Store API", doc.Title)
	assertEqual(t, "endpoint count", 3, len(doc.Endpoints))

	if doc.Checksum == "" {
		t.Error("expected non-empty checksum")
	}
}

func TestParseFromBytes_InvalidData(t *testing.T) {
	p := NewKinOpenAPIParser()

	_, err := p.ParseFromBytes([]byte("this is not valid openapi"))
	if err == nil {
		t.Fatal("expected error for invalid data, got nil")
	}

	var specErr *domain.SpecParseError
	if !errors.As(err, &specErr) {
		t.Errorf("expected *domain.SpecParseError, got %T", err)
	}
}

func TestParseFromBytes_EmptyData(t *testing.T) {
	p := NewKinOpenAPIParser()

	_, err := p.ParseFromBytes([]byte{})
	if err == nil {
		t.Fatal("expected error for empty data, got nil")
	}
}

func TestChecksumConsistency(t *testing.T) {
	p := NewKinOpenAPIParser()
	path := filepath.Join(testdataDir, "valid_openapi.yaml")

	doc1, err := p.Parse(path)
	if err != nil {
		t.Fatalf("first parse failed: %v", err)
	}

	doc2, err := p.Parse(path)
	if err != nil {
		t.Fatalf("second parse failed: %v", err)
	}

	if doc1.Checksum != doc2.Checksum {
		t.Errorf("checksums should match: %q != %q", doc1.Checksum, doc2.Checksum)
	}
}

func TestEndpointDetails(t *testing.T) {
	p := NewKinOpenAPIParser()
	path := filepath.Join(testdataDir, "valid_openapi.yaml")

	doc, err := p.Parse(path)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	for _, ep := range doc.Endpoints {
		if ep.OperationID == "createPet" {
			assertEqual(t, "createPet.summary", "Create a pet", ep.Summary)
			assertEqual(t, "createPet.method", "post", ep.Method)
			if ep.Deprecated {
				t.Error("expected createPet to NOT be deprecated")
			}
		}
	}
}

func assertEqual[T comparable](t *testing.T, name string, expected, actual T) {
	t.Helper()
	if expected != actual {
		t.Errorf("%s: expected %v, got %v", name, expected, actual)
	}
}
