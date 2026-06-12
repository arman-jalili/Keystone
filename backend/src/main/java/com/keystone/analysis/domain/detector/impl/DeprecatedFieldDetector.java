package com.keystone.analysis.domain.detector.impl;

import com.keystone.analysis.domain.detector.ChangeDetector;
import com.keystone.analysis.domain.model.Change;
import com.keystone.analysis.domain.model.ChangeSeverity;
import com.keystone.analysis.domain.model.ParsedEndpoint;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Detects newly deprecated fields and endpoints.
 *
 * <p>A field that was not deprecated in the base spec but is marked as
 * deprecated in the target spec is classified as
 * {@link ChangeSeverity#DEPRECATION}.
 */
@Component
public class DeprecatedFieldDetector implements ChangeDetector {

    @Override
    public String getName() {
        return "DeprecatedField";
    }

    @Override
    public ChangeSeverity getSeverity() {
        return ChangeSeverity.DEPRECATION;
    }

    @Override
    public List<Change> detect(ParsedEndpoint base, ParsedEndpoint target) {
        List<Change> changes = new ArrayList<>();

        if (base == null || target == null) {
            return changes;
        }

        // Check if endpoint became deprecated
        if (!base.deprecated() && target.deprecated()) {
            changes.add(new Change(
                    UUID.randomUUID(),
                    ChangeSeverity.DEPRECATION,
                    target.endpointKey(),
                    String.valueOf(base.deprecated()),
                    String.valueOf(target.deprecated()),
                    "API operation " + target.method() + " " + target.path()
                            + " is now deprecated",
                    getName()
            ));
        }

        return changes;
    }
}
