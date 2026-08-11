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
 * Handles Google's Gemini generateContent API. Shape differs from both
 * OpenAI and Claude: API key is passed as a query parameter, "system"
 * content uses systemInstruction, and content blocks use "parts"/"text".
 */
@Component
public class GeminiAiClient implements AiClient {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AppSettings.AiProvider provider() {
        return AppSettings.AiProvider.GEMINI;
    }

    @Override
    public AiResponse complete(AiRequest request, AppSettings settings) {
        String baseUrl = settings.getBaseUrl() != null && !settings.getBaseUrl().isBlank()
                ? settings.getBaseUrl() : DEFAULT_BASE_URL;
        String model = settings.getModelName() != null && !settings.getModelName().isBlank()
                ? settings.getModelName() : "gemini-2.0-flash";

        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", request.systemPrompt()))
                ),
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", request.userPrompt())))
                ),
                "generationConfig", Map.of(
                        "temperature", settings.getTemperature() != null ? settings.getTemperature() : 0.3,
                        "maxOutputTokens", settings.getMaxTokens() != null ? settings.getMaxTokens() : 1500
                )
        );

        try {
            String rawResponse = client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", settings.getApiKey())
                            .build(model))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(60));

            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                return AiResponse.failure("Gemini API returned no candidates. Raw: " + truncate(rawResponse));
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            StringBuilder text = new StringBuilder();
            for (JsonNode part : parts) {
                text.append(part.path("text").asText());
            }

            JsonNode usage = root.path("usageMetadata");
            Integer promptTokens = usage.path("promptTokenCount").isMissingNode() ? null : usage.path("promptTokenCount").asInt();
            Integer completionTokens = usage.path("candidatesTokenCount").isMissingNode() ? null : usage.path("candidatesTokenCount").asInt();

            return AiResponse.ok(text.toString(), promptTokens, completionTokens);

        } catch (Exception e) {
            return AiResponse.failure("Gemini API request failed: " + e.getMessage());
        }
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
