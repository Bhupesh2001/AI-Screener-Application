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
    private static final String QUOTE_SUMMARY_URL = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/";
    private static final String QUOTE_SUMMARY_MODULES = "?modules=price,summaryDetail,defaultKeyStatistics";

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

            log.debug("Yahoo chart response for {}: {}", yahooSymbol, json);

            // Market cap and trailing PE come from a separate endpoint (quoteSummary),
            // since the chart endpoint above only carries price/OHLC data. This call
            // is best-effort: if Yahoo blocks or reshapes it, we still return the
            // price/52-week data fetched above rather than failing the whole snapshot.
            BigDecimal marketCapCr = null;
            BigDecimal peRatio = null;
            try {
                String summaryJson = webClient.get()
                        .uri(QUOTE_SUMMARY_URL + yahooSymbol + QUOTE_SUMMARY_MODULES)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (summaryJson != null) {
                    log.debug("Yahoo quoteSummary response for {}: {}", yahooSymbol, summaryJson);
                    JsonNode summaryRoot = objectMapper.readTree(summaryJson);
                    JsonNode summaryResult = summaryRoot.path("quoteSummary").path("result").get(0);

                    if (summaryResult != null && !summaryResult.isMissingNode()) {
                        // marketCap comes back in raw INR (Yahoo doesn't know about
                        // "crores" - that's an Indian convention). 1 crore = 10,000,000.
                        BigDecimal marketCapRaw = getBigDecimal(
                                summaryResult.path("price").path("marketCap").path("raw"));
                        if (marketCapRaw != null) {
                            marketCapCr = marketCapRaw.divide(BigDecimal.valueOf(10_000_000), 2, java.math.RoundingMode.HALF_UP);
                        }

                        peRatio = getBigDecimal(
                                summaryResult.path("summaryDetail").path("trailingPE").path("raw"));
                        if (peRatio == null) {
                            // defaultKeyStatistics sometimes has trailingPE when
                            // summaryDetail doesn't (varies by ticker).
                            peRatio = getBigDecimal(
                                    summaryResult.path("defaultKeyStatistics").path("trailingPE").path("raw"));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch Yahoo quoteSummary (market cap/PE) for {}: {}", yahooSymbol, e.getMessage());
                // fall through - marketCapCr/peRatio stay null, rest of the snapshot is still useful
            }

            log.info("Parsed {}: price={}, high={}, low={}, marketCapCr={}, pe={}",
                    yahooSymbol, currentPrice, week52High, week52Low, marketCapCr, peRatio);

            return Optional.of(new PriceSnapshot(
                    symbol,
                    currentPrice,
                    week52High,
                    week52Low,
                    marketCapCr,
                    peRatio,
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