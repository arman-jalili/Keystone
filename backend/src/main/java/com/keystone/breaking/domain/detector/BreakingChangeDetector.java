package com.keystone.breaking.domain.detector;

import com.keystone.breaking.domain.exception.DiffAnalysisException;
import com.keystone.breaking.domain.model.BreakingChange;

import java.util.List;

/**
 * Strategy interface for built-in and custom breaking change detectors.
 *
 * <p>Each implementation detects one category of breaking change between
 * two versions of an OpenAPI specification. Detectors are registered in
 * the {@link com.keystone.breaking.domain.service.DiffOrchestrator} and
 * executed as part of the analysis pipeline.
 *
 * <p>Built-in detectors (registered by default):
 * <ul>
 *   <li>{@code RemovedEndpointDetector} — detects removed API operations</li>
 *   <li>{@code ParameterChangeDetector} — detects added, removed, or changed parameters</li>
 *   <li>{@code ResponseSchemaChangeDetector} — detects removals and type changes in responses</li>
 *   <li>{@code EnumValueDetector} — detects removed or reordered enum values</li>
 *   <li>{@code SecuritySchemeDetector} — detects security requirement changes</li>
 *   <li>{@code ExtensionDetector} — detects changes to semantically meaningful extensions</li>
 * </ul>
 *
 * <p>Implementations must be stateless and thread-safe.
 */
public interface BreakingChangeDetector {

    /**
     * Returns a unique identifier for this detector type.
     *
     * <p>Used for registration, logging, and filtering in analysis results.
     * Should use kebab-case (e.g. "removed-endpoint", "parameter-change").
     *
     * @return the detector identifier
     */
    String getDetectorId();

    /**
     * Analyzes the difference between two OpenAPI specifications and
     * returns any breaking changes found.
     *
     * <p>The {@code oldSpec} and {@code newSpec} parameters represent the
     * parsed OpenAPI specification content. Detectors should handle
     * missing or null sections gracefully.
     *
     * @param oldSpec  the base (older) OpenAPI specification content
     * @param newSpec  the new (current) OpenAPI specification content
     * @return list of breaking changes found (empty list if none)
     * @throws DiffAnalysisException if the detector cannot process the specs
     */
    List<BreakingChange> detect(String oldSpec, String newSpec) throws DiffAnalysisException;
}
