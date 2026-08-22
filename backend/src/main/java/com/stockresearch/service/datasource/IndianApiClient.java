package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class IndianApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    // In-memory cache with 1-hour TTL
    private final Cache<String, JsonNode> responseCache;

    public IndianApiClient(ObjectMapper objectMapper,
                           @Value("${indianapi.api-key}") String apiKey,
                           @Value("${indianapi.base-url:https://stock.indianapi.in}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                .build();

        // Cache with 1-hour expiration
        this.responseCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();

        log.info("IndianApiClient bean initialized with baseUrl: {}, apiKey present: {}", baseUrl, apiKey != null);
    }

    @PostConstruct
    public void init() {
        log.info("IndianApiClient bean is alive! API key present: {}", apiKey != null);
    }

    /**
     * Fetches stock data from IndianAPI, with caching.
     * Subsequent calls for the same symbol within 1 hour return the cached response.
     */
    public JsonNode getStockData(String symbol) {
        // Check cache first
        JsonNode cached = responseCache.getIfPresent(symbol);
        if (cached != null) {
            log.info("Cache hit for symbol: {}", symbol);
            return cached;
        }

        log.info("Calling IndianAPI for symbol: {}", symbol);
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/stock")
                            .queryParam("name", symbol)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                log.warn("Empty response for symbol: {}", symbol);
                return null;
            }

            log.info("Received response for {} (length: {})", symbol, response.length());
            JsonNode root = objectMapper.readTree(response);

            // Store in cache
            responseCache.put(symbol, root);
            log.info("Cached response for symbol: {}", symbol);

            return root;
        } catch (Exception e) {
            log.error("IndianAPI call failed for {}: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("IndianAPI request failed", e);
        }
    }

    /**
     * Clears the cache for a specific symbol (useful for manual refresh).
     */
    public void clearCache(String symbol) {
        responseCache.invalidate(symbol);
        log.info("Cache cleared for symbol: {}", symbol);
    }

    /**
     * Clears the entire cache.
     */
    public void clearAllCache() {
        responseCache.invalidateAll();
        log.info("All cache cleared");
    }

    public JsonNode getHistoricalStats(String symbol, String stats) {
        // This method is kept for future use; it does NOT use caching currently.
        // If you want to cache historical stats as well, add a separate cache.
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