// backend/src/main/java/com/stockresearch/service/datasource/ScreenerHtmlFundamentalsSource.java
package com.stockresearch.service.datasource;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Primary
@Component
public class ScreenerHtmlFundamentalsSource implements FundamentalsDataSource {

    private static final Logger log = LoggerFactory.getLogger(ScreenerHtmlFundamentalsSource.class);
    private static final String BASE_URL = "https://www.screener.in/company/";

    @Override
    @Cacheable(value = "fundamentals", key = "#symbol", unless = "#result == null")
    public Optional<FundamentalsSnapshot> fetchFundamentals(String symbol) {
        try {
            Document doc = Jsoup.connect(BASE_URL + symbol + "/")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(30000)
                    .get();

            // ---- Revenue Growth & Profit Growth from Profit & Loss table ----
            Element plTable = doc.selectFirst("table#profit-loss");
            BigDecimal revenueGrowth = null;
            BigDecimal profitGrowth = null;
            if (plTable != null) {
                revenueGrowth = getGrowthFromTable(plTable, "Revenue");
                profitGrowth = getGrowthFromTable(plTable, "Profit Before Tax"); // or "Net Profit"
            }

            // ---- Ratios: Operating Margin, Debt-to-Equity, ROCE, ROE ----
            Element ratiosTable = doc.selectFirst("table#ratios");
            BigDecimal margin = null, debtToEquity = null, roce = null, roe = null;
            if (ratiosTable != null) {
                margin = getRatioValue(ratiosTable, "Operating Profit Margin");
                debtToEquity = getRatioValue(ratiosTable, "Debt to Equity");
                roce = getRatioValue(ratiosTable, "ROCE");
                roe = getRatioValue(ratiosTable, "ROE");
            }

            // ---- Shareholding: Promoter & Institutional ----
            Element shareTable = doc.selectFirst("table#shareholding");
            BigDecimal promoterHolding = null, institutionalHolding = null;
            if (shareTable != null) {
                promoterHolding = getShareholdingValue(shareTable, "Promoter");
                institutionalHolding = getShareholdingValue(shareTable, "Institutional");
            }

            return Optional.of(new FundamentalsSnapshot(
                    revenueGrowth,
                    profitGrowth,
                    margin,
                    debtToEquity,
                    roce,
                    roe,
                    promoterHolding,
                    institutionalHolding
            ));

        } catch (IOException e) {
            log.error("Failed to scrape Screener.in for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extracts YoY growth from a table row identified by label.
     * Assumes the table has a header row with years, and the data row contains numbers.
     */
    private BigDecimal getGrowthFromTable(Element table, String label) {
        Elements rows = table.select("tr");
        Element dataRow = null;
        // Find the row where the first cell contains the label (case-insensitive)
        for (Element row : rows) {
            Element firstCell = row.selectFirst("td, th");
            if (firstCell != null && firstCell.text().trim().equalsIgnoreCase(label)) {
                dataRow = row;
                break;
            }
        }
        if (dataRow == null) return null;

        Elements cells = dataRow.select("td");
        if (cells.size() < 3) return null; // need at least label + 2 years

        // Last two columns are the latest and previous years (assuming order is left-to-right)
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

    /**
     * Extracts a ratio value from a row in the ratios table.
     * The row should have a label in the first cell and the latest value in the second cell.
     */
    private BigDecimal getRatioValue(Element table, String label) {
        Elements rows = table.select("tr");
        for (Element row : rows) {
            Element firstCell = row.selectFirst("td");
            if (firstCell != null && firstCell.text().trim().equalsIgnoreCase(label)) {
                Elements cells = row.select("td");
                if (cells.size() >= 2) {
                    return parseNumber(cells.get(1).text());
                }
            }
        }
        return null;
    }

    /**
     * Extracts the latest shareholding percentage for a given category.
     * The shareholding table has rows: "Promoter", "Institutional", etc.
     * The latest percentage is usually in the last cell of that row.
     */
    private BigDecimal getShareholdingValue(Element table, String category) {
        Elements rows = table.select("tr");
        for (Element row : rows) {
            Element firstCell = row.selectFirst("td");
            if (firstCell != null && firstCell.text().trim().equalsIgnoreCase(category)) {
                Elements cells = row.select("td");
                if (cells.size() > 1) {
                    // The last cell is the latest quarter
                    String value = cells.get(cells.size() - 1).text();
                    return parseNumber(value);
                }
            }
        }
        return null;
    }

    /**
     * Parses a string like "1,234.56 Cr", "12.34%", "0.56x" into a BigDecimal.
     * Removes commas, percentage signs, "x", "Cr", and other non-numeric suffixes.
     */
    private BigDecimal parseNumber(String text) {
        if (text == null || text.isEmpty()) return null;
        String cleaned = text.replace(",", "")
                .replace("%", "")
                .replace("x", "")
                .replace("Cr", "")
                .trim();
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}