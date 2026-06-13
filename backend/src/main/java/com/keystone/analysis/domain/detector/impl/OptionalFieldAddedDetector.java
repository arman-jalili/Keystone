// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.domain.detector.impl;

import com.keystone.analysis.domain.detector.ChangeDetector;
import com.keystone.analysis.domain.model.Change;
import com.keystone.analysis.domain.model.ChangeSeverity;
import com.keystone.analysis.domain.model.ParsedEndpoint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Detects new optional fields added to an API operation.
 *
 * <p>Adding an optional field is a {@link ChangeSeverity#ADDITIVE} change
 * because it provides new functionality without breaking existing clients.
 */
@Component
public class OptionalFieldAddedDetector implements ChangeDetector {

    @Override
    public String getName() {
        return "OptionalFieldAdded";
    }

    @Override
    public ChangeSeverity getSeverity() {
        return ChangeSeverity.ADDITIVE;
    }

    @Override
    public List<Change> detect(ParsedEndpoint base, ParsedEndpoint target) {
        List<Change> changes = new ArrayList<>();

        if (base == null || target == null) {
            return changes;
        }

        // Detect new response types in target that weren't in base
        for (String targetType : target.responseTypes()) {
            if (!base.responseTypes().contains(targetType)) {
                changes.add(new Change(
                        UUID.randomUUID(),
                        ChangeSeverity.ADDITIVE,
                        target.endpointKey() + ".responses." + targetType,
                        null,
                        targetType,
                        "New response type '" + targetType + "' added to " + target.method() + " " + target.path(),
                        getName()));
            }
        }

        return changes;
    }
}
