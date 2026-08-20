package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class IndianApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public IndianApiClient(ObjectMapper objectMapper,
                           @Value("${indianapi.api-key}") String apiKey,
                           @Value("${indianapi.base-url:https://stock.indianapi.in}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024)) // 5 MB
                .build();
        log.info("IndianApiClient bean initialized.");
    }

    public JsonNode getStockData(String symbol) {
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/stock")
                        .queryParam("name", symbol)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse IndianAPI response", e);
        }
    }

    public JsonNode getHistoricalStats(String symbol, String stats) {
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/historical_stats")
                        .queryParam("stock_name", symbol)
                        .queryParam("stats", stats)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse IndianAPI historical stats", e);
        }
    }
}