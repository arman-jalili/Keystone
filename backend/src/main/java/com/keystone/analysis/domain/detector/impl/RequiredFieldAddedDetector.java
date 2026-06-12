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
 * Detects new required fields added to an API operation.
 *
 * <p>Adding a required field to a request body or parameter is a
 * {@link ChangeSeverity#BREAKING} change because existing clients
 * will not send the new required field.
 */
@Component
public class RequiredFieldAddedDetector implements ChangeDetector {

    @Override
    public String getName() {
        return "RequiredFieldAdded";
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

        // Detect new parameters in target that weren't in base
        for (var entry : target.parameters().entrySet()) {
            String paramName = entry.getKey();
            if (!base.parameters().containsKey(paramName)) {
                changes.add(new Change(
                        UUID.randomUUID(),
                        ChangeSeverity.BREAKING,
                        target.endpointKey() + ".parameters." + paramName,
                        null,
                        entry.getValue(),
                        "New required parameter '" + paramName + "' added to " + target.method() + " " + target.path(),
                        getName()));
            }
        }

        return changes;
    }
}
