// backend/src/main/java/com/stockresearch/service/datasource/ScreenerFundamentalsSource.java
package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Primary
@Component
class ScreenerFundamentalsSource implements FundamentalsDataSource {

    private static final Logger log = LoggerFactory.getLogger(ScreenerFundamentalsSource.class);
    private static final String API_URL = "https://www.screener.in/api/company/";
    private static final String HTML_URL = "https://www.screener.in/company/";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ScreenerFundamentalsSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    @Cacheable(value = "fundamentals", key = "#symbol", unless = "#result == null")
    public Optional<FundamentalsSnapshot> fetchFundamentals(String symbol) {
        // Try JSON API first
        Optional<FundamentalsSnapshot> fromJson = fetchFromJson(symbol);
        if (fromJson.isPresent()) {
            log.info("Fetched fundamentals for {} from JSON API", symbol);
            return fromJson;
        }

        // Fallback to HTML scraping
        log.info("JSON API failed, falling back to HTML scraping for {}", symbol);
        return fetchFromHtml(symbol);
    }

    private Optional<FundamentalsSnapshot> fetchFromJson(String symbol) {
        try {
            String json = webClient.get()
                    .uri(API_URL + symbol + "/")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null || json.isEmpty()) {
                log.warn("Empty response from Screener.in JSON API for {}", symbol);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(json);
            JsonNode consolidated = root.path("consolidated");
            if (consolidated.isMissingNode()) {
                consolidated = root.path("standalone");
                if (consolidated.isMissingNode()) {
                    log.warn("No consolidated/standalone data for {} in JSON", symbol);
                    return Optional.empty();
                }
            }

            // Extract ratios
            JsonNode ratios = consolidated.path("ratios");
            BigDecimal roce = getBigDecimal(ratios.path("roce"));
            BigDecimal roe = getBigDecimal(ratios.path("roe"));
            BigDecimal debtToEquity = getBigDecimal(ratios.path("debt_to_equity"));
            BigDecimal margin = getBigDecimal(ratios.path("operating_profit_margin"));

            // Growth from profit & loss (latest year vs previous)
            JsonNode profitLoss = consolidated.path("profit_loss");
            BigDecimal revenueGrowth = null;
            BigDecimal profitGrowth = null;
            if (profitLoss.isArray() && profitLoss.size() >= 2) {
                // Assuming latest year is first element
                JsonNode latest = profitLoss.get(0);
                JsonNode previous = profitLoss.get(1);
                BigDecimal latestRevenue = getBigDecimal(latest.path("revenue"));
                BigDecimal prevRevenue = getBigDecimal(previous.path("revenue"));
                if (latestRevenue != null && prevRevenue != null && prevRevenue.compareTo(BigDecimal.ZERO) != 0) {
                    revenueGrowth = latestRevenue.subtract(prevRevenue)
                            .divide(prevRevenue.abs(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }
                BigDecimal latestProfit = getBigDecimal(latest.path("profit"));
                BigDecimal prevProfit = getBigDecimal(previous.path("profit"));
                if (latestProfit != null && prevProfit != null && prevProfit.compareTo(BigDecimal.ZERO) != 0) {
                    profitGrowth = latestProfit.subtract(prevProfit)
                            .divide(prevProfit.abs(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }
            }

            // Shareholding
            JsonNode shareholding = consolidated.path("shareholding");
            BigDecimal promoter = getBigDecimal(shareholding.path("promoter").path("percentage"));
            BigDecimal institutional = getBigDecimal(shareholding.path("institutional").path("percentage"));

            return Optional.of(new FundamentalsSnapshot(
                    revenueGrowth,
                    profitGrowth,
                    margin,
                    debtToEquity,
                    roce,
                    roe,
                    promoter,
                    institutional
            ));

        } catch (Exception e) {
            log.warn("Failed to fetch fundamentals from JSON API for {}: {}", symbol, e.getMessage());
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

    // ---- HTML scraping fallback ----
    private Optional<FundamentalsSnapshot> fetchFromHtml(String symbol) {
        try {
            Document doc = Jsoup.connect(HTML_URL + symbol + "/")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(30000)
                    .get();

            // Try different table selectors – use class names if IDs fail
            Element plTable = doc.selectFirst("table#profit-loss, table[class*='financials']");
            BigDecimal revenueGrowth = null, profitGrowth = null;
            if (plTable != null) {
                revenueGrowth = getGrowthFromTable(plTable, "Revenue", "Revenue from Operations");
                profitGrowth = getGrowthFromTable(plTable, "Profit Before Tax", "Net Profit");
            }

            Element ratiosTable = doc.selectFirst("table#ratios, table[class*='ratios']");
            BigDecimal margin = null, debtToEquity = null, roce = null, roe = null;
            if (ratiosTable != null) {
                margin = getRatioValue(ratiosTable, "Operating Profit Margin", "Operating margin");
                debtToEquity = getRatioValue(ratiosTable, "Debt to Equity", "Debt/Equity");
                roce = getRatioValue(ratiosTable, "ROCE", "Return on Capital Employed");
                roe = getRatioValue(ratiosTable, "ROE", "Return on Equity");
            }

            Element shareTable = doc.selectFirst("table#shareholding, table[class*='shareholding']");
            BigDecimal promoter = null, institutional = null;
            if (shareTable != null) {
                promoter = getShareholdingValue(shareTable, "Promoter", "Promoter & Promoter Group");
                institutional = getShareholdingValue(shareTable, "Institutional", "Foreign Institutions", "DII");
            }

            if (revenueGrowth == null && profitGrowth == null && margin == null && roce == null) {
                log.warn("HTML scraping yielded no data for {}", symbol);
                return Optional.empty();
            }

            return Optional.of(new FundamentalsSnapshot(
                    revenueGrowth, profitGrowth, margin, debtToEquity, roce, roe, promoter, institutional
            ));
        } catch (Exception e) {
            log.error("HTML scraping failed for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    // ---- Helper methods for HTML parsing (modified to accept multiple labels) ----
    private BigDecimal getGrowthFromTable(Element table, String... labels) {
        Elements rows = table.select("tr");
        for (Element row : rows) {
            Element firstCell = row.selectFirst("td, th");
            if (firstCell == null) continue;
            String cellText = firstCell.text().trim();
            for (String label : labels) {
                if (cellText.equalsIgnoreCase(label) || cellText.toLowerCase().contains(label.toLowerCase())) {
                    Elements cells = row.select("td");
                    if (cells.size() < 3) return null;
                    int latestIdx = cells.size() - 1;
                    int prevIdx = latestIdx - 1;
                    BigDecimal latestVal = parseNumber(cells.get(latestIdx).text());
                    BigDecimal prevVal = parseNumber(cells.get(prevIdx).text());
                    if (latestVal != null && prevVal != null && prevVal.compareTo(BigDecimal.ZERO) != 0) {
                        return latestVal.subtract(prevVal)
                                .divide(prevVal.abs(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                    }
                    return null;
                }
            }
        }
        return null;
    }

    private BigDecimal getRatioValue(Element table, String... labels) {
        Elements rows = table.select("tr");
        for (Element row : rows) {
            Element firstCell = row.selectFirst("td");
            if (firstCell == null) continue;
            String cellText = firstCell.text().trim();
            for (String label : labels) {
                if (cellText.equalsIgnoreCase(label) || cellText.toLowerCase().contains(label.toLowerCase())) {
                    Elements cells = row.select("td");
                    if (cells.size() >= 2) {
                        return parseNumber(cells.get(1).text());
                    }
                }
            }
        }
        return null;
    }

    private BigDecimal getShareholdingValue(Element table, String... categories) {
        Elements rows = table.select("tr");
        for (Element row : rows) {
            Element firstCell = row.selectFirst("td");
            if (firstCell == null) continue;
            String cellText = firstCell.text().trim();
            for (String category : categories) {
                if (cellText.equalsIgnoreCase(category) || cellText.toLowerCase().contains(category.toLowerCase())) {
                    Elements cells = row.select("td");
                    if (cells.size() > 1) {
                        return parseNumber(cells.get(cells.size() - 1).text());
                    }
                }
            }
        }
        return null;
    }

    private BigDecimal parseNumber(String text) {
        if (text == null || text.isEmpty()) return null;
        String cleaned = text.replace(",", "")
                .replace("%", "")
                .replace("x", "")
                .replace("Cr", "")
                .replace("₹", "")
                .trim();
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}