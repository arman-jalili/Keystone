package com.keystone.ingestion.domain.filter;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keystone.ingestion.domain.exception.SpecParseException;
import org.junit.jupiter.api.Test;

class SpecValidatorImplTest {

    private final SpecValidatorImpl validator = new SpecValidatorImpl();

    @Test
    void validate_shouldAcceptValidOpenApi30Yaml() {
        String yaml =
                """
            openapi: "3.0.0"
            info:
              title: Test API
              version: "1.0.0"
            paths: {}
            """;
        assertThatNoException().isThrownBy(() -> validator.validate(yaml));
    }

    @Test
    void validate_shouldAcceptValidOpenApi31Json() {
        String json =
                """
            {
              "openapi": "3.1.0",
              "info": { "title": "Test", "version": "1.0" },
              "paths": {}
            }
            """;
        assertThatNoException().isThrownBy(() -> validator.validate(json));
    }

    @Test
    void validate_shouldThrowForInvalidYaml() {
        String invalid = "this is not valid openapi";
        assertThatThrownBy(() -> validator.validate(invalid)).isInstanceOf(SpecParseException.class);
    }

    @Test
    void validate_shouldThrowForEmptyContent() {
        assertThatThrownBy(() -> validator.validate("")).isInstanceOf(SpecParseException.class);
    }

    @Test
    void validate_shouldThrowForNullContent() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(Exception.class);
    }
}
