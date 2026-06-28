// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.domain.service;

import com.keystone.analysis.domain.model.ParsedEndpoint;
import java.util.List;

/**
 * Parses raw OpenAPI specification content into a list of {@link ParsedEndpoint}s
 * for change detection.
 *
 * <p>Supports both JSON and YAML OpenAPI 3.0/3.1 formats. Each operation in the
 * spec is extracted as a single {@link ParsedEndpoint} containing its method,
 * path, parameters, response types, and deprecation status.
 */
public interface SpecParser {

    /**
     * Parse raw OpenAPI spec content into structured endpoints.
     *
     * @param rawContent the raw OpenAPI specification content (JSON or YAML)
     * @return list of parsed endpoints, or empty list if parsing fails
     */
    List<ParsedEndpoint> parse(String rawContent);
}
