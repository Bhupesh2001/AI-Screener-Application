package com.stockresearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Filter parameters for Stock Explorer search, matching the required filter
 * set: Market Cap, PE, ROCE, Debt, Sector, Revenue Growth, Profit Growth, Score.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyFilterRequest {
    private String sector;
    private BigDecimal minMarketCapCr;
    private BigDecimal maxMarketCapCr;
    private BigDecimal maxPe;
    private BigDecimal minRoce;
    private BigDecimal maxDebtToEquity;
    private BigDecimal minRevenueGrowthPct;
    private BigDecimal minProfitGrowthPct;
    private Integer minScore;
}
