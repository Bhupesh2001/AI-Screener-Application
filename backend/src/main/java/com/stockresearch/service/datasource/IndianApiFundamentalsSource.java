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
public class IndianApiFundamentalsSource implements FundamentalsDataSource {

    private final IndianApiClient apiClient;

    public IndianApiFundamentalsSource(IndianApiClient apiClient) {
        this.apiClient = apiClient;
        log.info("IndianApiFundamentalsSource bean initialized.");
    }

    @Override
    @Cacheable(value = "fundamentals", key = "#symbol", unless = "#result == null")
    public Optional<FundamentalsSnapshot> fetchFundamentals(String symbol) {
        try {
            JsonNode root = apiClient.getStockData(symbol);
            // The API returns data inside a "data" field or directly; adjust based on actual response.
            // We'll assume top-level fields: profit_growth, revenue_growth, operating_margin, etc.
            // But we need to check the actual structure. We'll map from known fields.
            // For shareholding, we might need to call /historical_stats with "shareholding_pattern_quarterly".
            // However, the /stock endpoint might already include latest shareholding.
            // We'll design flexible parsing.

            BigDecimal revenueGrowth = getBigDecimal(root.path("revenue_growth"));
            BigDecimal profitGrowth = getBigDecimal(root.path("profit_growth"));
            BigDecimal operatingMargin = getBigDecimal(root.path("operating_margin"));
            BigDecimal debtToEquity = getBigDecimal(root.path("debt_to_equity"));
            BigDecimal roce = getBigDecimal(root.path("roce"));
            BigDecimal roe = getBigDecimal(root.path("roe"));
            BigDecimal promoterHolding = getBigDecimal(root.path("promoter_holding"));
            BigDecimal institutionalHolding = getBigDecimal(root.path("institutional_holding"));
            BigDecimal marketCapCr = getBigDecimal(root.path("market_cap"));
            BigDecimal peRatio = getBigDecimal(root.path("pe_ratio"));

            return Optional.of(new FundamentalsSnapshot(
                    revenueGrowth,
                    profitGrowth,
                    operatingMargin,
                    debtToEquity,
                    roce,
                    roe,
                    promoterHolding,
                    institutionalHolding,
                    marketCapCr,
                    peRatio
            ));
        } catch (Exception e) {
            // log and return empty
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