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
        try {
            JsonNode root = apiClient.getStockData(symbol);
            BigDecimal currentPrice = getBigDecimal(root.path("current_price"));
            BigDecimal week52High = getBigDecimal(root.path("high_52_week"));
            BigDecimal week52Low = getBigDecimal(root.path("low_52_week"));

            // The rest of the fields (marketCap, PE, etc.) will come from fundamentals.
            return Optional.of(new PriceSnapshot(
                    symbol,
                    currentPrice,
                    week52High,
                    week52Low,
                    null, null, null, null, null, null, null, null, null, null, null
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private BigDecimal getBigDecimal(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        try {
            return new BigDecimal(node.asText().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}