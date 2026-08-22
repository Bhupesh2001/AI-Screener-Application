package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Primary
@Component
@Slf4j
public class IndianApiPriceDataSource implements PriceDataSource {

    private final IndianApiClient apiClient;

    public IndianApiPriceDataSource(IndianApiClient apiClient) {
        this.apiClient = apiClient;
        log.info("IndianApiPriceDataSource bean initialized.");
    }

    @Override
    @Cacheable(value = "price", key = "#symbol", unless = "#result == null")
    public Optional<PriceSnapshot> fetchSnapshot(String symbol) {
        log.info("Fetching price for symbol: {}", symbol);
        try {
            JsonNode root = apiClient.getStockData(symbol);
            return parsePriceSnapshot(symbol, root);
        } catch (Exception e) {
            log.error("Failed to fetch price for {}: {}", symbol, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Parse price data from an already-fetched JsonNode.
     * This method is called by DiscoveryPipeline to avoid redundant API calls.
     */
    public Optional<PriceSnapshot> parseFromNode(String symbol, JsonNode root) {
        log.debug("Parsing price from cached node for: {}", symbol);
        return parsePriceSnapshot(symbol, root);
    }

    private Optional<PriceSnapshot> parsePriceSnapshot(String symbol, JsonNode root) {
        try {
            // Correct field name: "currentPrice" (capital P)
            JsonNode currentPriceNode = root.path("currentPrice");
            BigDecimal currentPrice = getBigDecimal(currentPriceNode.path("NSE"));
            if (currentPrice == null) {
                currentPrice = getBigDecimal(currentPriceNode.path("BSE"));
            }

            BigDecimal week52High = getBigDecimal(root.path("yearHigh"));
            BigDecimal week52Low = getBigDecimal(root.path("yearLow"));

            if (currentPrice == null) {
                log.warn("No current price found for {}", symbol);
                return Optional.empty();
            }

            log.info("Successfully fetched price for {}", symbol);
            return Optional.of(new PriceSnapshot(
                    symbol,
                    currentPrice,
                    week52High,
                    week52Low,
                    null, null, null, null, null, null, null, null, null, null, null
            ));
        } catch (Exception e) {
            log.error("Error parsing price for {}: {}", symbol, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private BigDecimal getBigDecimal(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        try {
            String text = node.asText().trim();
            if (text.isEmpty()) return null;
            return new BigDecimal(text.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}