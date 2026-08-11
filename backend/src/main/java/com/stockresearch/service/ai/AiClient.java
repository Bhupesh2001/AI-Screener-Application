package com.stockresearch.service.ai;

import com.stockresearch.domain.AppSettings;

/**
 * Abstraction over "call an LLM and get text back". A single implementation
 * (OpenAiCompatibleAiClient) covers OpenAI, OpenRouter, and any other
 * OpenAI-compatible endpoint out of the box. Claude and Gemini use slightly
 * different request/response shapes, so they get their own thin adapters -
 * but all three implement this same interface, so the rest of the app
 * (PromptBuilder, ResearchService) never needs to know which provider is
 * active.
 */
public interface AiClient {

    /** Which AppSettings.AiProvider this client handles. */
    AppSettings.AiProvider provider();

    AiResponse complete(AiRequest request, AppSettings settings);

    record AiRequest(
            String systemPrompt,
            String userPrompt
    ) {}

    record AiResponse(
            boolean success,
            String text,
            Integer promptTokens,
            Integer completionTokens,
            String errorMessage
    ) {
        public static AiResponse ok(String text, Integer promptTokens, Integer completionTokens) {
            return new AiResponse(true, text, promptTokens, completionTokens, null);
        }

        public static AiResponse failure(String errorMessage) {
            return new AiResponse(false, null, null, null, errorMessage);
        }
    }
}
