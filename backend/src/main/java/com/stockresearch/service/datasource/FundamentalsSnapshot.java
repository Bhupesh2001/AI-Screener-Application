// backend/src/main/java/com/stockresearch/service/datasource/FundamentalsSnapshot.java
package com.stockresearch.service.datasource;

import java.math.BigDecimal;

public record FundamentalsSnapshot(
        // Annual growth & margins
        BigDecimal revenueGrowthPct,
        BigDecimal profitGrowthPct,
        BigDecimal operatingMarginPct,
        BigDecimal debtToEquity,
        BigDecimal roce,
        BigDecimal roe,
        // Shareholding
        BigDecimal promoterHoldingPct,
        BigDecimal institutionalHoldingPct
) {}