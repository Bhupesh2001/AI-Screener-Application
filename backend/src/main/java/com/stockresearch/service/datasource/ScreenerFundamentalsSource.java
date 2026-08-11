//// backend/src/main/java/com/stockresearch/service/datasource/ScreenerFundamentalsSource.java
//package com.stockresearch.service.datasource;
//
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.context.annotation.Primary;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.io.ByteArrayInputStream;
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.Optional;
//
//@Primary
//@Component
//public class ScreenerFundamentalsSource implements FundamentalsDataSource {
//
//    private static final Logger log = LoggerFactory.getLogger(ScreenerFundamentalsSource.class);
//    private static final String EXPORT_URL = "https://www.screener.in/api/company/{symbol}/download/?type=excel";
//
//    private final WebClient webClient;
//
//    public ScreenerFundamentalsSource() {
//        this.webClient = WebClient.builder()
//                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
//                .defaultHeader("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
//                .build();
//    }
//
//    @Override
//    @Cacheable(value = "fundamentals", key = "#symbol", unless = "#result == null")
//    public Optional<FundamentalsSnapshot> fetchFundamentals(String symbol) {
//        try {
//            byte[] excelBytes = webClient.get()
//                    .uri(EXPORT_URL, symbol)
//                    .retrieve()
//                    .bodyToMono(byte[].class)
//                    .block();
//
//            if (excelBytes == null || excelBytes.length == 0) {
//                log.warn("No Excel data from Screener.in for {}", symbol);
//                return Optional.empty();
//            }
//
//            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
//                // ---- Extract from "Ratios" sheet ----
//                Sheet ratiosSheet = workbook.getSheet("Ratios");
//                if (ratiosSheet == null) {
//                    log.warn("No 'Ratios' sheet in Screener export for {}", symbol);
//                    return Optional.empty();
//                }
//                // Find the row with "ROCE" and "ROE", etc.
//                // We'll search for known labels.
//                BigDecimal roce = findRatio(ratiosSheet, "ROCE");
//                BigDecimal roe = findRatio(ratiosSheet, "ROE");
//                BigDecimal debtToEquity = findRatio(ratiosSheet, "Debt to equity");
//                BigDecimal margin = findRatio(ratiosSheet, "Operating profit margin");
//
//                // ---- Extract growth from "Profit & Loss" sheet ----
//                Sheet plSheet = workbook.getSheet("Profit & Loss");
//                BigDecimal revenueGrowth = null;
//                BigDecimal profitGrowth = null;
//                if (plSheet != null) {
//                    revenueGrowth = calculateGrowth(plSheet, "Revenue");
//                    profitGrowth = calculateGrowth(plSheet, "Profit");
//                }
//
//                // ---- Shareholding from "Shareholding" sheet ----
//                Sheet shareSheet = workbook.getSheet("Shareholding");
//                BigDecimal promoterHolding = null;
//                BigDecimal institutionalHolding = null;
//                if (shareSheet != null) {
//                    promoterHolding = findLatestShareholding(shareSheet, "Promoter");
//                    institutionalHolding = findLatestShareholding(shareSheet, "Institutional");
//                }
//
//                return Optional.of(new FundamentalsSnapshot(
//                        revenueGrowth,
//                        profitGrowth,
//                        margin,
//                        debtToEquity,
//                        roce,
//                        roe,
//                        promoterHolding,
//                        institutionalHolding
//                ));
//            }
//
//        } catch (Exception e) {
//            log.error("Failed to fetch/parse fundamentals from Screener.in for {}: {}", symbol, e.getMessage());
//            return Optional.empty();
//        }
//    }
//
//    /**
//     * Finds a ratio value by label in the "Ratios" sheet.
//     * Assumes labels are in column A and values in column B (latest year).
//     */
//    private BigDecimal findRatio(Sheet sheet, String label) {
//        for (Row row : sheet) {
//            Cell labelCell = row.getCell(0);
//            if (labelCell != null && labelCell.getStringCellValue().trim().equalsIgnoreCase(label)) {
//                Cell valueCell = row.getCell(1);
//                if (valueCell != null) {
//                    return getNumericValue(valueCell);
//                }
//            }
//        }
//        return null;
//    }
//
//    /**
//     * Calculates YoY growth from the Profit & Loss sheet.
//     * Looks for the row containing the label (e.g., "Revenue") and compares the last two columns.
//     */
//    private BigDecimal calculateGrowth(Sheet sheet, String label) {
//        Row headerRow = sheet.getRow(0);
//        if (headerRow == null) return null;
//        int latestCol = -1, prevCol = -1;
//        // Find columns for latest and previous year (assume last two columns are years)
//        int lastCellNum = headerRow.getLastCellNum();
//        if (lastCellNum < 3) return null; // need at least 3 columns (label + 2 years)
//        latestCol = lastCellNum - 1;
//        prevCol = lastCellNum - 2;
//
//        for (Row row : sheet) {
//            Cell labelCell = row.getCell(0);
//            if (labelCell != null && labelCell.getStringCellValue().trim().equalsIgnoreCase(label)) {
//                Cell latestCell = row.getCell(latestCol);
//                Cell prevCell = row.getCell(prevCol);
//                if (latestCell != null && prevCell != null) {
//                    BigDecimal latestVal = getNumericValue(latestCell);
//                    BigDecimal prevVal = getNumericValue(prevCell);
//                    if (latestVal != null && prevVal != null && prevVal.compareTo(BigDecimal.ZERO) != 0) {
//                        return latestVal.subtract(prevVal)
//                                .divide(prevVal.abs(), 4, RoundingMode.HALF_UP)
//                                .multiply(BigDecimal.valueOf(100));
//                    }
//                }
//                break;
//            }
//        }
//        return null;
//    }
//
//    /**
//     * Finds the latest shareholding percentage for a given category (Promoter / Institutional).
//     * Assumes the shareholding sheet has rows like "Promoter" with columns for each quarter.
//     */
//    private BigDecimal findLatestShareholding(Sheet sheet, String category) {
//        for (Row row : sheet) {
//            Cell labelCell = row.getCell(0);
//            if (labelCell != null && labelCell.getStringCellValue().trim().equalsIgnoreCase(category)) {
//                // Find the last non-empty cell in this row (latest quarter)
//                int lastCell = row.getLastCellNum() - 1;
//                for (int i = lastCell; i >= 1; i--) {
//                    Cell cell = row.getCell(i);
//                    if (cell != null && cell.getCellType() != CellType.BLANK) {
//                        return getNumericValue(cell);
//                    }
//                }
//            }
//        }
//        return null;
//    }
//
//    private BigDecimal getNumericValue(Cell cell) {
//        if (cell == null) return null;
//        switch (cell.getCellType()) {
//            case NUMERIC:
//                return BigDecimal.valueOf(cell.getNumericCellValue());
//            case STRING:
//                try {
//                    return new BigDecimal(cell.getStringCellValue().replace(",", "").trim());
//                } catch (NumberFormatException e) {
//                    return null;
//                }
//            default:
//                return null;
//        }
//    }
//}