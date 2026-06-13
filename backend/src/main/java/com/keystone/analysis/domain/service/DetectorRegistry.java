// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.domain.service;

import com.keystone.analysis.domain.detector.ChangeDetector;
import java.util.List;

/**
 * Registry interface for loading and managing {@link ChangeDetector} implementations.
 *
 * <p>Both built-in and custom detectors are registered here. The registry
 * is consumed by {@link DiffOrchestrator} to retrieve all detectors for
 * analysis. Implementations may use Spring's dependency injection to
 * auto-discover {@code @Component} detectors, or provide a manual registry.
 */
public interface DetectorRegistry {

    /**
     * Returns all registered detectors.
     *
     * @return list of registered detectors (never null, may be empty)
     */
    List<ChangeDetector> getAllDetectors();

    /**
     * Finds a detector by its name.
     *
     * @param name the detector name
     * @return the detector, or empty if not found
     */
    java.util.Optional<ChangeDetector> getByName(String name);

    /**
     * Registers a detector, replacing any existing detector with the same name.
     *
     * @param detector the detector to register
     */
    void register(ChangeDetector detector);

    /**
     * Removes a detector by name.
     *
     * @param name the detector name to remove
     * @return true if a detector was removed
     */
    boolean unregister(String name);
}
