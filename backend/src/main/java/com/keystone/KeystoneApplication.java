package com.keystone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Keystone Server — OpenAPI specification governance platform.
 *
 * <p>Entry point for the Spring Boot application. Scans the
 * {@code com.keystone.ingestion} package and its sub-packages
 * for Spring-managed components.
 */
@SpringBootApplication(scanBasePackages = "com.keystone.ingestion")
public class KeystoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeystoneApplication.class, args);
    }
}
