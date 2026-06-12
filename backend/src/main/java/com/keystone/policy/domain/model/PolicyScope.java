package com.keystone.policy.domain.model;

import java.util.Objects;
import java.util.Set;

/**
 * Defines the target scope for a policy rule.
 *
 * <p>Specifies which endpoints, paths, operations, or response codes
 * a policy applies to. A policy with no scope constraints applies to
 * all spec elements.
 *
 * @param pathPatterns  Ant-style path patterns the policy applies to (e.g. "/api/v1/**")
 * @param operations    HTTP methods the policy applies to (empty = all methods)
 * @param tags          OpenAPI operation tags the policy targets (empty = all tags)
 * @param excludePaths  Path patterns to exclude from policy evaluation
 */
public record PolicyScope(
    Set<String> pathPatterns,
    Set<HttpOperation> operations,
    Set<String> tags,
    Set<String> excludePaths
) {
    public PolicyScope {
        pathPatterns = pathPatterns == null ? Set.of() : Set.copyOf(pathPatterns);
        operations = operations == null ? Set.of() : Set.copyOf(operations);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        excludePaths = excludePaths == null ? Set.of() : Set.copyOf(excludePaths);
    }

    public static PolicyScope all() {
        return new PolicyScope(Set.of("/**"), Set.of(), Set.of(), Set.of());
    }

    public boolean appliesToAll() {
        return pathPatterns.contains("/**") && operations.isEmpty() && tags.isEmpty();
    }

    /** HTTP operation methods that policies can target. */
    public enum HttpOperation {
        GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
    }
}
