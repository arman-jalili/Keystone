// Canonical Reference: .pi/architecture/modules/dependency-graph.md
// Module: dependency-graph
package com.keystone.graph.domain.exception;

/**
 * Thrown when a service referenced in a {@code keystone.yml} declaration
 * does not exist in the graph.
 *
 * <p>This typically occurs when a consumer declares a dependency on a service
 * that has not been registered yet. The parser should skip the unknown dependency,
 * log the error, and continue processing remaining declarations.
 */
public class UnknownServiceException extends RuntimeException {

    public UnknownServiceException(String message) {
        super(message);
    }

    public UnknownServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with a descriptive message.
     *
     * @param consumerName The name of the service that declared the dependency
     * @param unknownServiceName The name of the referenced service that was not found
     */
    public static UnknownServiceException forDependency(String consumerName, String unknownServiceName) {
        return new UnknownServiceException(
                "Consumer '" + consumerName + "' depends on unknown service: " + unknownServiceName);
    }
}
