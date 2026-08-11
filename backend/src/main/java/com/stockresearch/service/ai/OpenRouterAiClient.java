package com.stockresearch.service.ai;

import com.stockresearch.domain.AppSettings;
import org.springframework.stereotype.Component;

/**
 * OpenRouter uses an OpenAI-compatible request/response shape, so this class
 * simply reuses OpenAiCompatibleAiClient's HTTP logic with a different
 * default base URL, default model, and provider identity.
 */
@Component
public class OpenRouterAiClient extends OpenAiCompatibleAiClient {

    @Override
    public AppSettings.AiProvider provider() {
        return AppSettings.AiProvider.OPENROUTER;
    }

    @Override
    protected String defaultBaseUrl() {
        return "https://openrouter.ai/api/v1";
    }

    @Override
    protected String defaultModel() {
        return "openai/gpt-4o-mini";
    }
}
