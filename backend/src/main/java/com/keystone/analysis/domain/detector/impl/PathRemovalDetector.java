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
 * Detects API operations that were removed from the specification.
 *
 * <p>An operation is considered removed when it exists in the base spec
 * but is absent from the target spec (indicated by a null target endpoint).
 * This is classified as {@link ChangeSeverity#BREAKING}.
 */
@Component
public class PathRemovalDetector implements ChangeDetector {

    @Override
    public String getName() {
        return "PathRemoval";
    }

    @Override
    public ChangeSeverity getSeverity() {
        return ChangeSeverity.BREAKING;
    }

    @Override
    public List<Change> detect(ParsedEndpoint base, ParsedEndpoint target) {
        List<Change> changes = new ArrayList<>();

        if (base == null) {
            return changes;
        }

        // If target is null, the endpoint was removed
        if (target == null) {
            changes.add(new Change(
                    UUID.randomUUID(),
                    ChangeSeverity.BREAKING,
                    base.endpointKey(),
                    base.path(),
                    null,
                    "API operation " + base.method() + " " + base.path() + " was removed",
                    getName()));
        }

        return changes;
    }
}
