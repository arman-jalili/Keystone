package com.keystone.analysis.domain.detector;

import com.keystone.analysis.domain.model.Change;
import com.keystone.analysis.domain.model.ChangeSeverity;
import com.keystone.analysis.domain.model.ParsedEndpoint;
import java.util.List;

/**
 * Strategy interface for detecting specific types of API changes.
 *
 * <p>Each implementation detects one category of change (breaking, additive,
 * deprecation, etc.) by comparing a base and target {@link ParsedEndpoint}.
 *
 * <p>Built-in detectors (registered by default):
 * <ul>
 *   <li>{@code PathRemovalDetector} — detects removed API operations</li>
 *   <li>{@code RequiredFieldAddedDetector} — detects new required fields</li>
 *   <li>{@code FieldRemovalDetector} — detects removed response fields</li>
 *   <li>{@code FieldTypeChangedDetector} — detects type changes in fields</li>
 *   <li>{@code OptionalFieldAddedDetector} — detects new optional fields (additive)</li>
 *   <li>{@code DeprecatedFieldDetector} — detects newly deprecated fields</li>
 * </ul>
 *
 * <p>Implementations must be stateless and thread-safe.
 */
public interface ChangeDetector {

    /**
     * Returns a unique name for this detector.
     *
     * <p>Used for registration, logging, and filtering in analysis results.
     *
     * @return the detector name (e.g. "PathRemoval", "FieldTypeChanged")
     */
    String getName();

    /**
     * Compares a base endpoint against a target endpoint and returns
     * any changes detected.
     *
     * @param base   the endpoint from the base (older) specification
     * @param target the endpoint from the target (newer) specification
     * @return list of changes found (empty list if none)
     */
    List<Change> detect(ParsedEndpoint base, ParsedEndpoint target);

    /**
     * Returns the default severity for changes detected by this detector.
     *
     * @return the change severity
     */
    ChangeSeverity getSeverity();
}
