package com.stockresearch.service.ai;

import com.stockresearch.domain.AppSettings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Routes to the correct AiClient implementation based on the currently
 * configured AppSettings.aiProvider. Adding a new provider = add a new
 * AiClient @Component; no changes needed here.
 */
@Service
public class AiClientRouter {

    private final Map<AppSettings.AiProvider, AiClient> clientsByProvider;

    public AiClientRouter(List<AiClient> clients) {
        this.clientsByProvider = clients.stream()
                .collect(Collectors.toMap(AiClient::provider, c -> c));
    }

    public AiClient.AiResponse complete(AiClient.AiRequest request, AppSettings settings) {
        AiClient client = clientsByProvider.get(settings.getAiProvider());
        if (client == null) {
            return AiClient.AiResponse.failure(
                    "No AI client configured for provider: " + settings.getAiProvider()
                            + ". Local LLM support is planned but not yet implemented.");
        }
        if (settings.getApiKey() == null || settings.getApiKey().isBlank()) {
            if (settings.getAiProvider() != AppSettings.AiProvider.LOCAL) {
                return AiClient.AiResponse.failure(
                        "No API key configured for " + settings.getAiProvider()
                                + ". Add one in Settings to enable AI research.");
            }
        }
        return client.complete(request, settings);
    }
}
