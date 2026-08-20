package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
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
        log.info("Fetching fundamentals for symbol: {}", symbol);
        try {
            JsonNode root = apiClient.getStockData(symbol);

            // --- 1. Get key metrics from keyMetrics object ---
            JsonNode keyMetrics = root.path("keyMetrics");
            JsonNode margins = keyMetrics.path("margins");
            JsonNode financialStrength = keyMetrics.path("financialstrength");
            JsonNode mgmtEffectiveness = keyMetrics.path("mgmtEffectiveness");

            // Operating margin (trailing 12 months)
            BigDecimal operatingMargin = findMetricValue(margins, "operatingMarginTrailing12Month");

            // Debt to equity (most recent fiscal year)
            BigDecimal debtToEquity = findMetricValue(financialStrength, "totalDebtPerTotalEquityMostRecentFiscalYear");

            // ROCE (return on investment, most recent fiscal year)
            BigDecimal roce = findMetricValue(mgmtEffectiveness, "returnOnInvestmentMostRecentFiscalYear");

            // ROE (return on average equity, most recent fiscal year)
            BigDecimal roe = findMetricValue(mgmtEffectiveness, "returnOnAverageEquityMostRecentFiscalYear)");
            // Also try fallback:
            if (roe == null) {
                roe = findMetricValue(mgmtEffectiveness, "returnOnAverageEquityTrailing12Month");
            }

            // --- 2. Revenue and profit growth: compute from annual financials (latest vs previous) ---
            JsonNode financials = root.path("financials");
            BigDecimal revenueGrowth = null;
            BigDecimal profitGrowth = null;

            // Collect all annual reports
            List<JsonNode> annualReports = new ArrayList<>();
            for (JsonNode fin : financials) {
                if ("Annual".equals(fin.path("Type").asText())) {
                    annualReports.add(fin);
                }
            }

            // Sort by FiscalYear descending (most recent first)
            annualReports.sort((a, b) -> {
                int fyA = a.path("FiscalYear").asInt(0);
                int fyB = b.path("FiscalYear").asInt(0);
                return Integer.compare(fyB, fyA);
            });

            if (annualReports.size() >= 2) {
                JsonNode latest = annualReports.get(0);
                JsonNode previous = annualReports.get(1);

                JsonNode latestInc = latest.path("stockFinancialMap").path("INC");
                JsonNode prevInc = previous.path("stockFinancialMap").path("INC");

                BigDecimal latestRevenue = findIncomeValue(latestInc, "TotalRevenue");
                BigDecimal prevRevenue = findIncomeValue(prevInc, "TotalRevenue");
                BigDecimal latestProfit = findIncomeValue(latestInc, "NetIncome");
                BigDecimal prevProfit = findIncomeValue(prevInc, "NetIncome");

                if (latestRevenue != null && prevRevenue != null && prevRevenue.compareTo(BigDecimal.ZERO) != 0) {
                    revenueGrowth = latestRevenue.subtract(prevRevenue)
                            .divide(prevRevenue.abs(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }
                if (latestProfit != null && prevProfit != null && prevProfit.compareTo(BigDecimal.ZERO) != 0) {
                    profitGrowth = latestProfit.subtract(prevProfit)
                            .divide(prevProfit.abs(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }
            }

            // --- 3. Shareholding pattern (latest promoter and institutional) ---
            JsonNode shareholding = root.path("shareholding");
            BigDecimal promoterHolding = null;
            BigDecimal institutionalHolding = null;
            if (shareholding.isArray()) {
                // Look for "Promoter" and "FII" or "MF" categories
                for (JsonNode cat : shareholding) {
                    String displayName = cat.path("displayName").asText();
                    JsonNode categories = cat.path("categories");
                    if (categories.isArray() && !categories.isEmpty()) {
                        // Get the latest entry (last in array, assuming chronological order)
                        JsonNode latestHolding = categories.get(categories.size() - 1);
                        BigDecimal pct = getBigDecimal(latestHolding.path("percentage"));
                        if ("Promoter".equalsIgnoreCase(displayName)) {
                            promoterHolding = pct;
                        } else if ("FII".equalsIgnoreCase(displayName) || "MF".equalsIgnoreCase(displayName)) {
                            // We might combine FII + MF for total institutional
                            if (institutionalHolding == null) institutionalHolding = BigDecimal.ZERO;
                            if (pct != null) institutionalHolding = institutionalHolding.add(pct);
                        }
                    }
                }
            }

            // --- 4. Market Cap and PE ratio from stockDetailsReusableData ---
            JsonNode details = root.path("stockDetailsReusableData");
            BigDecimal marketCapCr = getBigDecimal(details.path("marketCap"));
            BigDecimal peRatio = getBigDecimal(details.path("pPerEBasicExcludingExtraordinaryItemsTTM"));

            log.info("Successfully fetched fundamentals for {}", symbol);
            log.info("Parsed {}: revenueGrowth={}, profitGrowth={}, operatingMargin={}, debtToEquity={}, roce={}, roe={}, promoter={}, institutional={}, marketCapCr={}, peRatio={}",
                    symbol, revenueGrowth, profitGrowth, operatingMargin, debtToEquity, roce, roe,
                    promoterHolding, institutionalHolding, marketCapCr, peRatio);

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
            log.error("Failed to fetch fundamentals for {}: {}", symbol, e.getMessage(), e);
            return Optional.empty();
        }
    }

    // Helper: find a metric value by key inside an array of objects like "margins"
    private BigDecimal findMetricValue(JsonNode array, String key) {
        if (!array.isArray()) return null;
        for (JsonNode item : array) {
            if (key.equals(item.path("key").asText())) {
                return getBigDecimal(item.path("value"));
            }
        }
        return null;
    }

    // Helper: find a value from income statement array (INC)
    private BigDecimal findIncomeValue(JsonNode incArray, String key) {
        if (!incArray.isArray()) return null;
        for (JsonNode item : incArray) {
            if (key.equals(item.path("key").asText())) {
                return getBigDecimal(item.path("value"));
            }
        }
        return null;
    }

    private BigDecimal getBigDecimal(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        String text = node.asText().trim();
        if (text.isEmpty()) return null;
        try {
            // Remove commas if present
            text = text.replace(",", "");
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}