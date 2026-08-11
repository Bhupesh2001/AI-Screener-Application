package com.stockresearch.service.datasource;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Abstraction over "wherever we get price/fundamental data from" - Yahoo
 * Finance today, something else tomorrow. Implementations should be swapped
 * via Spring configuration (@Primary / profiles), never by changing calling
 * code.
 */
public interface PriceDataSource {

    /**
     * Fetch the latest price + fundamental snapshot for a symbol.
     * Returns empty if the symbol can't be resolved by this source.
     */
    Optional<PriceSnapshot> fetchSnapshot(String symbol);

    record PriceSnapshot(
            String symbol,
            BigDecimal currentPrice,
            BigDecimal week52High,
            BigDecimal week52Low,
            BigDecimal marketCapCr,
            BigDecimal peRatio,
            BigDecimal revenueGrowthPct,
            BigDecimal profitGrowthPct,
            BigDecimal operatingMarginPct,
            BigDecimal debtToEquity,
            BigDecimal roce,
            BigDecimal roe,
            BigDecimal operatingCashFlowCr,
            BigDecimal promoterHoldingPct,
            BigDecimal institutionalHoldingPct
    ) {}
}
