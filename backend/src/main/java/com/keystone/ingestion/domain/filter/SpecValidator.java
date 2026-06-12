package com.keystone.ingestion.domain.filter;

import com.keystone.ingestion.domain.exception.SpecParseException;

/**
 * Domain service interface for OpenAPI 3.x specification validation.
 *
 * <p>Validates that the incoming spec content conforms to the OpenAPI 3.x
 * specification. The implementation must handle both YAML and JSON formats
 * and return structured error details on failure.
 */
public interface SpecValidator {

    /**
     * Validates the given OpenAPI spec content.
     *
     * @param content the raw spec content (YAML or JSON)
     * @throws SpecParseException if the content is not valid OpenAPI 3.x
     */
    void validate(String content) throws SpecParseException;
}
