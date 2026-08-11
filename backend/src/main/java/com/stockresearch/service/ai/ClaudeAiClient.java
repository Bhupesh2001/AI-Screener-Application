package com.stockresearch.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockresearch.domain.AppSettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Handles the Anthropic Messages API (api.anthropic.com/v1/messages), which
 * has a different shape from OpenAI's: system prompt is a top-level field
 * (not a message), and the API key goes in an x-api-key header with an
 * anthropic-version header required.
 */
@Component
public class ClaudeAiClient implements AiClient {

    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AppSettings.AiProvider provider() {
        return AppSettings.AiProvider.CLAUDE;
    }

    @Override
    public AiResponse complete(AiRequest request, AppSettings settings) {
        String baseUrl = settings.getBaseUrl() != null && !settings.getBaseUrl().isBlank()
                ? settings.getBaseUrl() : DEFAULT_BASE_URL;
        String model = settings.getModelName() != null && !settings.getModelName().isBlank()
                ? settings.getModelName() : "claude-sonnet-4-6";

        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", settings.getApiKey())
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", settings.getMaxTokens() != null ? settings.getMaxTokens() : 1500,
                "temperature", settings.getTemperature() != null ? settings.getTemperature() : 0.3,
                "system", request.systemPrompt(),
                "messages", List.of(
                        Map.of("role", "user", "content", request.userPrompt())
                )
        );

        try {
            String rawResponse = client.post()
                    .uri("/messages")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(60));

            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode content = root.path("content");
            if (!content.isArray() || content.isEmpty()) {
                return AiResponse.failure("Claude API returned no content. Raw: " + truncate(rawResponse));
            }

            StringBuilder text = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    text.append(block.path("text").asText());
                }
            }

            JsonNode usage = root.path("usage");
            Integer inputTokens = usage.path("input_tokens").isMissingNode() ? null : usage.path("input_tokens").asInt();
            Integer outputTokens = usage.path("output_tokens").isMissingNode() ? null : usage.path("output_tokens").asInt();

            return AiResponse.ok(text.toString(), inputTokens, outputTokens);

        } catch (Exception e) {
            return AiResponse.failure("Claude API request failed: " + e.getMessage());
        }
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
