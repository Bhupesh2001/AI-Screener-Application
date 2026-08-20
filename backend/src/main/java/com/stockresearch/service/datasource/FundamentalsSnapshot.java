package com.stockresearch.service.datasource;

import java.math.BigDecimal;

public record FundamentalsSnapshot(
        BigDecimal revenueGrowthPct,
        BigDecimal profitGrowthPct,
        BigDecimal operatingMarginPct,
        BigDecimal debtToEquity,
        BigDecimal roce,
        BigDecimal roe,
        BigDecimal promoterHoldingPct,
        BigDecimal institutionalHoldingPct,
        BigDecimal marketCapCr,
        BigDecimal peRatio
) {}