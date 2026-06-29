// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md
// Implements: OpenAPI/Swagger configuration for API documentation
package com.keystone.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation configuration.
 *
 * <p>Exposes Swagger UI at {@code /swagger-ui.html} and the
 * OpenAPI spec at {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public OpenAPI keystoneOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Keystone API")
                        .description("OpenAPI Specification Governance Platform — REST API")
                        .version("0.1.0")
                        .contact(new Contact().name("Keystone Team").email("keystone@example.com"))
                        .license(new License().name("Internal").url("https://github.com/arman-jalili/Keystone")))
                .servers(List.of(new Server()
                        .url(
                                activeProfile.equals("prod")
                                        ? "https://api.keystone.example.com"
                                        : "http://localhost:8080")
                        .description(activeProfile.equals("prod") ? "Production" : "Local Development")));
    }
}
