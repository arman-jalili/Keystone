// Package parser provides an implementation of the SpecParser interface using
// github.com/getkin/kin-openapi to parse and validate OpenAPI 3.x specifications.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#spec-parser
//
// This is the infrastructure adapter. It translates between the kin-openapi
// library types and the domain types defined in internal/domain/.
package parser

import (
	"context"
	"fmt"
	"os"
	"strings"

	"github.com/getkin/kin-openapi/openapi3"

	"github.com/arman-jalili/keystone-cli/internal/domain"
)

// KinOpenAPIParser implements interfaces.SpecParser using the kin-openapi library.
type KinOpenAPIParser struct {
	loader *openapi3.Loader
}

// NewKinOpenAPIParser creates a new parser with default settings.
// External refs are disallowed by default to prevent security issues.
func NewKinOpenAPIParser() *KinOpenAPIParser {
	loader := openapi3.NewLoader()
	loader.IsExternalRefsAllowed = false
	if loader.Context == nil {
		loader.Context = context.Background()
	}
	return &KinOpenAPIParser{loader: loader}
}

// Parse loads, parses, and validates an OpenAPI 3.x spec from the given file path.
func (p *KinOpenAPIParser) Parse(path string) (*domain.SpecDocument, error) {
	doc, err := p.loader.LoadFromFile(path)
	if err != nil {
		return nil, &domain.SpecParseError{
			Path:    path,
			Details: []error{fmt.Errorf("load from file: %w", err)},
		}
	}

	if err := doc.Validate(p.loader.Context); err != nil {
		return nil, &domain.SpecParseError{
			Path:    path,
			Details: []error{fmt.Errorf("validation: %w", err)},
		}
	}

	return convertToSpecDocument(path, doc)
}

// ParseFromBytes parses a spec from raw byte data (e.g., from stdin or git diff output).
func (p *KinOpenAPIParser) ParseFromBytes(data []byte) (*domain.SpecDocument, error) {
	doc, err := p.loader.LoadFromData(data)
	if err != nil {
		return nil, &domain.SpecParseError{
			Path:    "<data>",
			Details: []error{fmt.Errorf("load from data: %w", err)},
		}
	}

	if err := doc.Validate(p.loader.Context); err != nil {
		return nil, &domain.SpecParseError{
			Path:    "<data>",
			Details: []error{fmt.Errorf("validation: %w", err)},
		}
	}

	return convertFromDoc(doc, "<data>", data)
}

// convertToSpecDocument reads raw file data and transforms an openapi3.T into a domain.SpecDocument.
func convertToSpecDocument(path string, doc *openapi3.T) (*domain.SpecDocument, error) {
	raw, err := os.ReadFile(path)
	if err != nil {
		return nil, &domain.SpecParseError{
			Path:    path,
			Details: []error{fmt.Errorf("read file: %w", err)},
		}
	}
	return convertFromDoc(doc, path, raw)
}

func convertFromDoc(doc *openapi3.T, sourcePath string, rawData []byte) (*domain.SpecDocument, error) {
	spec := &domain.SpecDocument{
		Version:    doc.OpenAPI,
		Title:      doc.Info.Title,
		APIVersion: doc.Info.Version,
		Endpoints:  extractEndpoints(doc),
	}

	if rawData != nil {
		spec.Checksum = domain.ChecksumBytes(rawData)
	}

	return spec, nil
}

// extractEndpoints iterates over all paths and operations in the spec.
func extractEndpoints(doc *openapi3.T) []domain.Endpoint {
	var endpoints []domain.Endpoint

	for _, path := range doc.Paths.InMatchingOrder() {
		pathItem := doc.Paths.Value(path)
		if pathItem == nil {
			continue
		}
		for method, op := range pathItem.Operations() {
			endpoint := domain.Endpoint{
				Path:        path,
				Method:      strings.ToLower(method),
				OperationID: op.OperationID,
				Deprecated:  op.Deprecated,
			}
			if op.Summary != "" {
				endpoint.Summary = op.Summary
			}
			endpoints = append(endpoints, endpoint)
		}
	}

	return endpoints
}
