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
 * Detects response schema fields that were removed from an API operation.
 *
 * <p>Removing a field from the response schema is a {@link ChangeSeverity#BREAKING}
 * change because existing clients may depend on that field.
 */
@Component
public class FieldRemovalDetector implements ChangeDetector {

    @Override
    public String getName() {
        return "FieldRemoval";
    }

    @Override
    public ChangeSeverity getSeverity() {
        return ChangeSeverity.BREAKING;
    }

    @Override
    public List<Change> detect(ParsedEndpoint base, ParsedEndpoint target) {
        List<Change> changes = new ArrayList<>();

        if (base == null || target == null) {
            return changes;
        }

        // Detect response types present in base but missing in target
        for (String baseType : base.responseTypes()) {
            if (!target.responseTypes().contains(baseType)) {
                changes.add(new Change(
                        UUID.randomUUID(),
                        ChangeSeverity.BREAKING,
                        target.endpointKey() + ".responses." + baseType,
                        baseType,
                        null,
                        "Response type '" + baseType + "' was removed from " + target.method() + " " + target.path(),
                        getName()));
                continue;
            }
        }

        // Detect parameters that were removed
        for (var entry : base.parameters().entrySet()) {
            String paramName = entry.getKey();
            if (!target.parameters().containsKey(paramName)) {
                changes.add(new Change(
                        UUID.randomUUID(),
                        ChangeSeverity.BREAKING,
                        target.endpointKey() + ".parameters." + paramName,
                        entry.getValue(),
                        null,
                        "Parameter '" + paramName + "' was removed from " + target.method() + " " + target.path(),
                        getName()));
            }
        }

        return changes;
    }
}
