package com.keystone.analysis.domain.service.impl;

import com.keystone.analysis.domain.detector.ChangeDetector;
import com.keystone.analysis.domain.service.DetectorRegistry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link DetectorRegistry}.
 *
 * <p>Detectors can be registered programmatically or auto-discovered
 * via Spring component scanning. Built-in detectors annotated with
 * {@code @Component} are automatically registered.
 */
@Component
public class DetectorRegistryImpl implements DetectorRegistry {

    private final Map<String, ChangeDetector> detectors = new ConcurrentHashMap<>();

    public DetectorRegistryImpl(List<ChangeDetector> autoDetected) {
        for (ChangeDetector detector : autoDetected) {
            detectors.put(detector.getName(), detector);
        }
    }

    @Override
    public List<ChangeDetector> getAllDetectors() {
        return List.copyOf(detectors.values());
    }

    @Override
    public Optional<ChangeDetector> getByName(String name) {
        return Optional.ofNullable(detectors.get(name));
    }

    @Override
    public void register(ChangeDetector detector) {
        detectors.put(detector.getName(), detector);
    }

    @Override
    public boolean unregister(String name) {
        return detectors.remove(name) != null;
    }
}
