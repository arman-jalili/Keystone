package com.keystone.breaking.domain.model;

/**
 * Categorised types of breaking changes that can be detected.
 *
 * <p>Each enum value corresponds to a specific {@link com.keystone.breaking.domain.detector.BreakingChangeDetector}
 * implementation. The {@code code} field provides a stable, machine-readable
 * identifier for use in API responses and event payloads.
 *
 * <p>New types may be added as custom detectors are registered, but
 * the built-in set defined here must remain stable per this contract.
 */
public enum ChangeType {

    /** An API operation was removed entirely. */
    ENDPOINT_REMOVED("ENDPOINT_REMOVED"),

    /** A query parameter was removed from an existing operation. */
    PARAMETER_REMOVED("PARAMETER_REMOVED"),

    /** A required query parameter was added to an existing operation. */
    PARAMETER_ADDED_REQUIRED("PARAMETER_ADDED_REQUIRED"),

    /** A parameter's type or format was changed in a way that breaks clients. */
    PARAMETER_TYPE_CHANGED("PARAMETER_TYPE_CHANGED"),

    /** A previously optional parameter was made required. */
    PARAMETER_MADE_REQUIRED("PARAMETER_MADE_REQUIRED"),

    /** An enum value was removed from a parameter or response schema. */
    ENUM_VALUE_REMOVED("ENUM_VALUE_REMOVED"),

    /** A response property was removed. */
    RESPONSE_PROPERTY_REMOVED("RESPONSE_PROPERTY_REMOVED"),

    /** A response property type was changed. */
    RESPONSE_PROPERTY_TYPE_CHANGED("RESPONSE_PROPERTY_TYPE_CHANGED"),

    /** A response property was made non-nullable (was previously nullable). */
    RESPONSE_PROPERTY_MADE_REQUIRED("RESPONSE_PROPERTY_MADE_REQUIRED"),

    /** The response status code for an operation was removed. */
    RESPONSE_STATUS_CODE_REMOVED("RESPONSE_STATUS_CODE_REMOVED"),

    /** The request body schema was changed in a breaking way. */
    REQUEST_BODY_TYPE_CHANGED("REQUEST_BODY_TYPE_CHANGED"),

    /** A required request body property was removed. */
    REQUEST_BODY_PROPERTY_REMOVED("REQUEST_BODY_PROPERTY_REMOVED"),

    /** A security scheme requirement was added or tightened. */
    SECURITY_SCHEME_CHANGED("SECURITY_SCHEME_CHANGED"),

    /** A spec extension that had semantic meaning was modified. */
    EXTENSION_BREAKING_CHANGE("EXTENSION_BREAKING_CHANGE");

    private final String code;

    ChangeType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
