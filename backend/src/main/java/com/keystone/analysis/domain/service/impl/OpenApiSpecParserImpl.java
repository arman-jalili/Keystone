// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.domain.service.impl;

import com.keystone.analysis.domain.model.ParsedEndpoint;
import com.keystone.analysis.domain.service.SpecParser;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Parses raw OpenAPI 3.0/3.1 specification content (JSON or YAML) into a list
 * of {@link ParsedEndpoint}s using the swagger-parser library.
 *
 * <p>Each path + method combination in the spec becomes a single endpoint.
 * Parameters (path, query, header) are extracted as key-type pairs. Response
 * types are extracted from the response content schemas (media types).
 */
@Service
public class OpenApiSpecParserImpl implements SpecParser {

    private static final Logger log = LoggerFactory.getLogger(OpenApiSpecParserImpl.class);

    private final OpenAPIParser parser;

    public OpenApiSpecParserImpl() {
        this.parser = new OpenAPIParser();
    }

    @Override
    public List<ParsedEndpoint> parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            log.warn("Empty spec content — returning empty endpoint list");
            return List.of();
        }

        try {
            ParseOptions options = new ParseOptions();
            options.setResolve(false);
            options.setFlatten(false);

            SwaggerParseResult result = parser.readContents(rawContent, List.of(), options);

            if (result == null) {
                log.warn("Swagger parser returned null result for spec content");
                return List.of();
            }

            List<String> messages = result.getMessages();
            if (messages != null && !messages.isEmpty()) {
                log.debug("Parser messages ({}): {}", messages.size(),
                        messages.size() > 5 ? messages.subList(0, 5) + "..." : messages);
            }

            OpenAPI openAPI = result.getOpenAPI();
            if (openAPI == null) {
                log.warn("Failed to parse OpenAPI spec — no OpenAPI model produced");
                return List.of();
            }

            if (openAPI.getPaths() == null || openAPI.getPaths().isEmpty()) {
                log.warn("OpenAPI spec has no paths defined");
                return List.of();
            }

            List<ParsedEndpoint> endpoints = new ArrayList<>();

            for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
                String path = pathEntry.getKey();
                PathItem pathItem = pathEntry.getValue();

                if (pathItem == null) {
                    continue;
                }

                // Extract path-level parameters
                Map<String, String> pathParams = extractParameters(pathItem.getParameters());

                // Process each HTTP method
                for (Map.Entry<PathItem.HttpMethod, Operation> methodEntry : pathItem.readOperationsMap().entrySet()) {
                    String httpMethod = methodEntry.getKey().name();
                    Operation operation = methodEntry.getValue();

                    if (operation == null) {
                        continue;
                    }

                    // Merge operation-level parameters with path-level parameters
                    Map<String, String> operationParams = new LinkedHashMap<>(pathParams);
                    if (operation.getParameters() != null) {
                        for (Parameter param : operation.getParameters()) {
                            if (param != null && param.getName() != null) {
                                operationParams.put(param.getName(), resolveSchemaType(param.getSchema()));
                            }
                        }
                    }

                    // Extract response types
                    Set<String> responseTypes = extractResponseTypes(operation);

                    endpoints.add(new ParsedEndpoint(
                            httpMethod,
                            path,
                            operation.getSummary() != null ? operation.getSummary() : "",
                            Collections.unmodifiableMap(operationParams),
                            Collections.unmodifiableSet(responseTypes),
                            operation.getDeprecated() != null && operation.getDeprecated()
                    ));
                }
            }

            log.debug("Parsed {} endpoints from OpenAPI spec", endpoints.size());
            return List.copyOf(endpoints);

        } catch (Exception e) {
            log.warn("Failed to parse OpenAPI spec: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Extracts parameter names and their schema types from a list of OpenAPI parameters.
     */
    private Map<String, String> extractParameters(List<Parameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Parameter param : parameters) {
            if (param != null && param.getName() != null) {
                result.put(param.getName(), resolveSchemaType(param.getSchema()));
            }
        }
        return result;
    }

    /**
     * Extracts response content types from an operation's responses.
     */
    private Set<String> extractResponseTypes(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null || responses.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> types = new java.util.LinkedHashSet<>();
        for (ApiResponse response : responses.values()) {
            if (response == null) {
                continue;
            }
            Content content = response.getContent();
            if (content != null) {
                for (Map.Entry<String, MediaType> mediaEntry : content.entrySet()) {
                    String mediaType = mediaEntry.getKey();
                    types.add(mediaType);
                    // Also extract the schema type name if available
                    if (mediaEntry.getValue() != null) {
                        Schema<?> schema = mediaEntry.getValue().getSchema();
                        if (schema != null && schema.getName() != null) {
                            types.add(schema.getName());
                        }
                    }
                }
            }
        }
        return types;
    }

    /**
     * Resolves a human-readable type string from a Schema object.
     */
    private String resolveSchemaType(Schema<?> schema) {
        if (schema == null) {
            return "string";
        }
        if (schema.getType() != null) {
            return schema.getType();
        }
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            return ref.substring(ref.lastIndexOf('/') + 1);
        }
        if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
            return "object (allOf)";
        }
        if (schema.getOneOf() != null && !schema.getOneOf().isEmpty()) {
            return "object (oneOf)";
        }
        if (schema.getAnyOf() != null && !schema.getAnyOf().isEmpty()) {
            return "object (anyOf)";
        }
        return "object";
    }
}
