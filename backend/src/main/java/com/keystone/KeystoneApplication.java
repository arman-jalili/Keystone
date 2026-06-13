// Canonical Reference: .pi/INDEX.md
// Module: keystone-server
package com.keystone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Keystone Server — OpenAPI specification governance platform.
 *
 * <p>Entry point for the Spring Boot application. Scans the
 * {@code com.keystone.ingestion}, {@code com.keystone.analysis},
 * and {@code com.keystone.policy} packages and their sub-packages
 * for Spring-managed components.
 */
@SpringBootApplication(scanBasePackages = {"com.keystone.ingestion", "com.keystone.analysis", "com.keystone.policy"})
public class KeystoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeystoneApplication.class, args);
    }
}
