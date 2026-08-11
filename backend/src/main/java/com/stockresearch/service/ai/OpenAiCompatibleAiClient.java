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
 * Handles the OpenAI chat completions API. Shares its request-building logic
 * with OpenRouterAiClient via the protected helper methods below, since
 * OpenRouter uses an identical request/response shape - only the base URL
 * and default model differ.
 */
@Component
public class OpenAiCompatibleAiClient implements AiClient {

    protected static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1";

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AppSettings.AiProvider provider() {
        return AppSettings.AiProvider.OPENAI;
    }

    @Override
    public AiResponse complete(AiRequest request, AppSettings settings) {
        String baseUrl = settings.getBaseUrl() != null && !settings.getBaseUrl().isBlank()
                ? settings.getBaseUrl()
                : defaultBaseUrl();
        String model = settings.getModelName() != null && !settings.getModelName().isBlank()
                ? settings.getModelName()
                : defaultModel();
        return callChatCompletionsApi(request, settings, baseUrl, model);
    }

    /** Override in subclasses (e.g. OpenRouter) that point at a different default endpoint. */
    protected String defaultBaseUrl() {
        return DEFAULT_OPENAI_BASE_URL;
    }

    /** Override in subclasses that want a different default model name. */
    protected String defaultModel() {
        return "gpt-4o-mini";
    }

    /**
     * Shared request/response logic for any provider exposing an OpenAI-shaped
     * /chat/completions endpoint. Extra headers (e.g. OpenRouter's optional
     * attribution headers) can be layered on by overriding extraHeaders().
     */
    protected AiResponse callChatCompletionsApi(AiRequest request, AppSettings settings, String baseUrl, String model) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + settings.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        extraHeaders().forEach(builder::defaultHeader);
        WebClient client = builder.build();

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", settings.getTemperature() != null ? settings.getTemperature() : 0.3,
                "max_tokens", settings.getMaxTokens() != null ? settings.getMaxTokens() : 1500,
                "messages", List.of(
                        Map.of("role", "system", "content", request.systemPrompt()),
                        Map.of("role", "user", "content", request.userPrompt())
                )
        );

        try {
            String rawResponse = client.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(60));

            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return AiResponse.failure("AI provider returned no choices. Raw: " + truncate(rawResponse));
            }

            String text = choices.get(0).path("message").path("content").asText();
            JsonNode usage = root.path("usage");
            Integer promptTokens = usage.path("prompt_tokens").isMissingNode() ? null : usage.path("prompt_tokens").asInt();
            Integer completionTokens = usage.path("completion_tokens").isMissingNode() ? null : usage.path("completion_tokens").asInt();

            return AiResponse.ok(text, promptTokens, completionTokens);

        } catch (Exception e) {
            return AiResponse.failure("AI request failed: " + e.getMessage());
        }
    }

    protected Map<String, String> extraHeaders() {
        return Map.of();
    }

    protected String truncate(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
