// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.domain.filter;

import com.keystone.ingestion.domain.exception.SpecParseException;
import com.keystone.ingestion.domain.exception.SpecParseException.ValidationError;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Validates OpenAPI 3.x spec content using the swagger-parser library.
 *
 * <p>Supports both YAML and JSON formats. Returns structured error details
 * on validation failure.
 */
@Component
public class SpecValidatorImpl implements SpecValidator {

    @Override
    public void validate(String content) throws SpecParseException {
        var options = new ParseOptions();
        options.setResolve(false);
        options.setFlatten(false);

        SwaggerParseResult result = new OpenAPIParser().readContents(content, List.of(), options);

        if (result.getOpenAPI() == null) {
            List<String> messages = result.getMessages();
            if (messages == null || messages.isEmpty()) {
                messages = List.of("Failed to parse OpenAPI specification");
            }
            List<ValidationError> errors = messages.stream()
                    .map(msg -> new ValidationError("content", msg))
                    .toList();
            throw new SpecParseException("OpenAPI spec validation failed", errors);
        }
    }
}
