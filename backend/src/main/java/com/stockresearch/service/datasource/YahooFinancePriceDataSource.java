// backend/src/main/java/com/stockresearch/service/datasource/YahooFinancePriceDataSource.java
package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Optional;

@Primary
@Component
public class YahooFinancePriceDataSource implements PriceDataSource {

    private static final Logger log = LoggerFactory.getLogger(YahooFinancePriceDataSource.class);
    private static final String CHART_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public YahooFinancePriceDataSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
    }

    @Override
    @Cacheable(value = "price", key = "#symbol", unless = "#result == null")
    public Optional<PriceSnapshot> fetchSnapshot(String symbol) {
        String yahooSymbol = symbol + ".NS";
        try {
            String json = webClient.get()
                    .uri(CHART_URL + yahooSymbol)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null) {
                log.warn("No response from Yahoo Finance for {}", yahooSymbol);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(json);
            JsonNode result = root.path("chart").path("result").get(0);
            if (result.isMissingNode()) {
                log.warn("No chart result for {}", yahooSymbol);
                return Optional.empty();
            }

            JsonNode meta = result.path("meta");
            BigDecimal currentPrice = getBigDecimal(meta.path("regularMarketPrice"));
            BigDecimal week52High = getBigDecimal(meta.path("fiftyTwoWeekHigh"));
            BigDecimal week52Low = getBigDecimal(meta.path("fiftyTwoWeekLow"));

            // Market cap and PE are not available from the chart endpoint.
            // We'll set them to null; they will be filled by the fundamentals source.
            return Optional.of(new PriceSnapshot(
                    symbol,
                    currentPrice,
                    week52High,
                    week52Low,
                    null, // marketCapCr
                    null, // peRatio
                    null, // revenueGrowthPct
                    null, // profitGrowthPct
                    null, // operatingMarginPct
                    null, // debtToEquity
                    null, // roce
                    null, // roe
                    null, // operatingCashFlowCr
                    null, // promoterHoldingPct
                    null  // institutionalHoldingPct
            ));

        } catch (Exception e) {
            log.error("Failed to fetch Yahoo Finance data for {}: {}", yahooSymbol, e.getMessage());
            return Optional.empty();
        }
    }

    private BigDecimal getBigDecimal(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        String text = node.asText().trim();
        if (text.isEmpty()) return null;
        try {
            return new BigDecimal(text.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}