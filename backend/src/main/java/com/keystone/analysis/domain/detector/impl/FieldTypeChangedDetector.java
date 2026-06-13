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
 * Detects changes to field types or parameter types.
 *
 * <p>Changing a field's type (e.g. from string to integer) is a
 * {@link ChangeSeverity#BREAKING} change because existing clients
 * will break if they expect the old type.
 */
@Component
public class FieldTypeChangedDetector implements ChangeDetector {

    @Override
    public String getName() {
        return "FieldTypeChanged";
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

        // Compare parameter types
        for (var baseEntry : base.parameters().entrySet()) {
            String paramName = baseEntry.getKey();
            String baseType = baseEntry.getValue();

            String targetType = target.parameters().get(paramName);
            if (targetType != null && !baseType.equals(targetType)) {
                changes.add(new Change(
                        UUID.randomUUID(),
                        ChangeSeverity.BREAKING,
                        target.endpointKey() + ".parameters." + paramName + ".type",
                        baseType,
                        targetType,
                        "Parameter '" + paramName + "' type changed from '"
                                + baseType + "' to '" + targetType + "' in "
                                + target.method() + " " + target.path(),
                        getName()));
            }
        }

        return changes;
    }
}
