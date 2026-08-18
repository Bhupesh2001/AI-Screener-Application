// backend/src/main/java/com/stockresearch/service/datasource/PythonFundamentalsSource.java
package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Optional;

@Primary
@Component
@Slf4j
public class PythonFundamentalsSource implements FundamentalsDataSource {

    private static final String SERVICE_URL = "http://localhost:5004/api/fundamentals/";
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public PythonFundamentalsSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(SERVICE_URL)
                .build();
    }

    @Override
    @Cacheable(value = "fundamentals", key = "#symbol", unless = "#result == null")
    public Optional<FundamentalsSnapshot> fetchFundamentals(String symbol) {
        try {
            String json = webClient.get()
                    .uri("/" + symbol)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null) return Optional.empty();

            JsonNode root = objectMapper.readTree(json);
            return Optional.of(new FundamentalsSnapshot(
                    getBigDecimal(root.path("revenueGrowthPct")),
                    getBigDecimal(root.path("profitGrowthPct")),
                    getBigDecimal(root.path("operatingMarginPct")),
                    getBigDecimal(root.path("debtToEquity")),
                    getBigDecimal(root.path("roce")),
                    getBigDecimal(root.path("roe")),
                    getBigDecimal(root.path("promoterHoldingPct")),
                    getBigDecimal(root.path("institutionalHoldingPct"))
            ));
        } catch (Exception e) {
            log.error("Failed to fetch from Python fundamentals service for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    private BigDecimal getBigDecimal(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}