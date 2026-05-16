package com.example.taskmanager.ai;

import com.example.taskmanager.ai.dto.TaskSuggestionResponse;
import com.example.taskmanager.task.TaskPriority;
import com.example.taskmanager.task.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OpenAI-backed {@link AiClient} implementation for task suggestions.
 *
 * <p>This client uses the Responses API with strict structured output so the model returns JSON matching
 * {@link TaskSuggestionResponse}. Network, provider, refusal, and parsing failures are intentionally mapped
 * to {@link AiServiceUnavailableException} to avoid leaking provider internals through the REST API.</p>
 */
@Component
public class OpenAiClient implements AiClient {

    private static final String RESPONSES_PATH = "/responses";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;

    @Autowired
    public OpenAiClient(
            ObjectMapper objectMapper,
            @Value("${ai.openai.api-key:}") String apiKey,
            @Value("${ai.openai.model:gpt-5.4-mini}") String model,
            @Value("${ai.openai.max-output-tokens:1200}") int maxOutputTokens,
            @Value("${ai.openai.connect-timeout:5s}") Duration connectTimeout,
            @Value("${ai.openai.read-timeout:20s}") Duration readTimeout
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    OpenAiClient(ObjectMapper objectMapper, RestClient restClient, String apiKey, String model, int maxOutputTokens) {
        this.objectMapper = objectMapper;
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
    }

    /**
     * Calls OpenAI to transform natural-language task text into a structured suggestion.
     *
     * @param description plain-language task or reminder text
     * @return model-generated task suggestion
     * @throws AiServiceUnavailableException when configuration, network, provider, or parsing failures occur
     */
    @Override
    public TaskSuggestionResponse suggestTask(String description) {
        if (!StringUtils.hasText(apiKey)) {
            throw new AiServiceUnavailableException("OPENAI_API_KEY is not configured");
        }

        try {
            String responseBody = restClient.post()
                    .uri(RESPONSES_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(requestBody(description))
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw new AiServiceUnavailableException(openAiErrorMessage(response));
                    })
                    .requiredBody(String.class);

            String outputText = extractOutputText(responseBody);
            return parseTaskSuggestion(outputText);
        } catch (ResourceAccessException exception) {
            throw new AiServiceUnavailableException("AI provider request timed out or could not be reached");
        } catch (AiServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException | JacksonException | DateTimeParseException | IllegalArgumentException exception) {
            throw new AiServiceUnavailableException("AI provider returned an invalid task suggestion");
        }
    }

    /**
     * Builds the Responses API request body, including strict JSON schema output.
     */
    private Map<String, Object> requestBody(String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", instructions());
        body.put("input", description);
        body.put("max_output_tokens", maxOutputTokens);
        body.put("text", Map.of("format", responseFormat()));
        return body;
    }

    private String instructions() {
        return """
                Convert the user's plain-language reminder into a single personal task.
                Today's date is %s.
                Return concise values. Use JSON null for dueDate when no due date can be inferred.
                dueDate must be an ISO-8601 date string in yyyy-MM-dd format or JSON null.
                Choose priority from LOW, MEDIUM, HIGH.
                Always use TODO for status unless the user clearly says the task is already in progress or done.
                """.formatted(LocalDate.now());
    }

    private Map<String, Object> responseFormat() {
        return Map.of(
                "type", "json_schema",
                "name", "task_suggestion",
                "description", "A structured personal task suggestion",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "required", List.of("title", "description", "dueDate", "priority", "status"),
                        "properties", Map.of(
                                "title", Map.of(
                                        "type", "string",
                                        "description", "Short action-oriented task title"
                                ),
                                "description", Map.of(
                                        "type", List.of("string", "null"),
                                        "description", "Brief task details, or null if no useful details are present"
                                ),
                                "dueDate", Map.of(
                                        "type", List.of("string", "null"),
                                        "description", "ISO-8601 date in yyyy-MM-dd format, or null when unknown"
                                ),
                                "priority", Map.of(
                                        "type", "string",
                                        "enum", List.of("LOW", "MEDIUM", "HIGH")
                                ),
                                "status", Map.of(
                                        "type", "string",
                                        "enum", List.of("TODO", "IN_PROGRESS", "DONE")
                                )
                        )
                )
        );
    }

    /**
     * Extracts the model's JSON string from the Responses API envelope while detecting provider refusals.
     */
    private String extractOutputText(String responseBody) throws JacksonException {
        JsonNode root = objectMapper.readTree(responseBody);
        String responseError = root.path("error").path("message").asString("");
        if (StringUtils.hasText(responseError)) {
            throw new AiServiceUnavailableException("AI provider failed to generate a task suggestion");
        }

        ensureCompleted(root);
        ensureNotRefusal(root);

        String outputText = extractOutputTextFromOutputArray(root);
        if (StringUtils.hasText(outputText)) {
            return outputText;
        }

        String sdkOutputText = root.path("output_text").asString("");
        if (StringUtils.hasText(sdkOutputText)) {
            return sdkOutputText;
        }

        throw new AiServiceUnavailableException("AI provider response did not include output text");
    }

    private void ensureCompleted(JsonNode root) {
        String status = root.path("status").asString("");
        if (!StringUtils.hasText(status) || "completed".equals(status)) {
            return;
        }

        if ("incomplete".equals(status)) {
            String reason = root.path("incomplete_details").path("reason").asString("");
            if (StringUtils.hasText(reason)) {
                throw new AiServiceUnavailableException("AI provider response was incomplete: " + reason);
            }
            throw new AiServiceUnavailableException("AI provider response was incomplete");
        }

        throw new AiServiceUnavailableException("AI provider response was not completed");
    }

    private void ensureNotRefusal(JsonNode root) {
        String refusal = root.findValuesAsString("refusal")
                .stream()
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
        if (StringUtils.hasText(refusal)) {
            throw new AiServiceUnavailableException("AI provider refused to generate a task suggestion");
        }
    }

    private String extractOutputTextFromOutputArray(JsonNode root) {
        JsonNode output = root.path("output");
        if (!output.isArray()) {
            return null;
        }

        for (JsonNode outputItem : output) {
            JsonNode content = outputItem.path("content");
            if (!content.isArray()) {
                continue;
            }

            for (JsonNode contentItem : content) {
                if (!"output_text".equals(contentItem.path("type").asString(""))) {
                    continue;
                }

                String text = contentItem.path("text").asString("");
                if (StringUtils.hasText(text)) {
                    return text;
                }
            }
        }

        return null;
    }

    private TaskSuggestionResponse parseTaskSuggestion(String outputText) throws JacksonException {
        ProviderTaskSuggestion suggestion = objectMapper.readValue(outputText, ProviderTaskSuggestion.class);
        String title = cleanNullableText(suggestion.title());
        if (!StringUtils.hasText(title)) {
            throw new AiServiceUnavailableException("AI provider did not return a usable task suggestion");
        }

        return new TaskSuggestionResponse(
                title,
                cleanNullableText(suggestion.description()),
                normalizeDueDate(suggestion.dueDate()),
                normalizePriority(suggestion.priority()),
                normalizeStatus(suggestion.status())
        );
    }

    private LocalDate normalizeDueDate(String dueDate) {
        String cleanDueDate = cleanNullableText(dueDate);
        return cleanDueDate == null ? null : LocalDate.parse(cleanDueDate);
    }

    private TaskPriority normalizePriority(String priority) {
        String cleanPriority = cleanEnumValue(priority);
        return cleanPriority == null ? null : TaskPriority.valueOf(cleanPriority);
    }

    private TaskStatus normalizeStatus(String status) {
        String cleanStatus = cleanEnumValue(status);
        return cleanStatus == null ? null : TaskStatus.valueOf(cleanStatus);
    }

    private String cleanNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String cleanEnumValue(String value) {
        String cleanValue = cleanNullableText(value);
        if (cleanValue == null) {
            return null;
        }

        return cleanValue
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    /**
     * Creates a sanitized provider error message for API clients.
     */
    private String openAiErrorMessage(ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        String providerMessage = extractProviderErrorMessage(body);
        if (StringUtils.hasText(providerMessage)) {
            return "AI provider request failed: " + providerMessage;
        }

        return "AI provider request failed with status " + response.getStatusCode().value();
    }

    private String extractProviderErrorMessage(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }

        try {
            String message = objectMapper.readTree(body).path("error").path("message").asString("");
            return StringUtils.hasText(message) ? message : null;
        } catch (JacksonException exception) {
            return null;
        }
    }

    private record ProviderTaskSuggestion(
            String title,
            String description,
            String dueDate,
            String priority,
            String status
    ) {
    }
}
