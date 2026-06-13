// Canonical Reference: .pi/INDEX.md
// Module: keystone-server
package com.keystone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Keystone Server — OpenAPI specification governance platform.
 *
 * <p>Entry point for the Spring Boot application. Scans all Keystone
 * modules for Spring-managed components.
 */
@SpringBootApplication(
        scanBasePackages = {
            "com.keystone.ingestion",
            "com.keystone.analysis",
            "com.keystone.policy",
            "com.keystone.graph",
            "com.keystone.notification",
            "com.keystone.dashboard"
        })
public class KeystoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeystoneApplication.class, args);
    }
}
