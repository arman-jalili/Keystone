package com.keystone.analysis.domain.model;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Value object representing a parsed API endpoint for change detection.
 *
 * <p>Produced by the spec parser and passed to each
 * {@link com.keystone.analysis.domain.detector.ChangeDetector} for analysis.
 * Contains the structured representation of a single endpoint extracted from
 * an OpenAPI specification.
 *
 * @param method      HTTP method (GET, POST, PUT, DELETE, PATCH, etc.)
 * @param path        The endpoint path template (e.g. "/api/v1/users/{id}")
 * @param summary     Optional human-readable endpoint summary
 * @param parameters  Map of parameter name to its type info
 * @param responseTypes Set of response content types or schema types
 * @param deprecated  Whether this endpoint is marked as deprecated
 */
public record ParsedEndpoint(
    String method,
    String path,
    String summary,
    Map<String, String> parameters,
    Set<String> responseTypes,
    boolean deprecated
) {
    public ParsedEndpoint {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");
        Objects.requireNonNull(responseTypes, "responseTypes must not be null");
        if (method.isBlank()) throw new IllegalArgumentException("method must not be blank");
        if (path.isBlank()) throw new IllegalArgumentException("path must not be blank");
    }

    /**
     * Returns the unique endpoint key combining method and path.
     */
    public String endpointKey() {
        return method.toUpperCase() + " " + path;
    }
}
