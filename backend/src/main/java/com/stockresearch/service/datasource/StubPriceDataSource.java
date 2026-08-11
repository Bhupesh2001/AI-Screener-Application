package com.stockresearch.service.datasource;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stub implementation of PriceDataSource. Returns realistic, deterministic-ish
 * fundamentals for the 3 seeded demo companies (RVNL, BEL, DEEPAKNTR), with
 * small random jitter each call to simulate periodic refreshes moving scores.
 *
 * TO REPLACE WITH REAL YAHOO FINANCE INTEGRATION:
 * Implement a new class (e.g. YahooFinancePriceDataSource) implementing
 * PriceDataSource, annotate it @Primary (or use a Spring profile), and this
 * stub will stop being picked up. No other code needs to change.
 */
@Component
public class StubPriceDataSource implements PriceDataSource {

    private static final Map<String, PriceSnapshot> BASE_DATA = Map.of(
            "RVNL", new PriceSnapshot(
                    "RVNL", bd(385), bd(647), bd(210), bd(80500), bd(78),
                    bd(24.5), bd(38.2), bd(14.8), bd(0.35), bd(18.2), bd(21.4),
                    bd(1250), bd(72.84), bd(15.2)
            ),
            "BEL", new PriceSnapshot(
                    "BEL", bd(298), bd(340), bd(178), bd(218000), bd(42),
                    bd(18.7), bd(22.1), bd(28.4), bd(0.02), bd(31.5), bd(28.9),
                    bd(3400), bd(51.14), bd(28.6)
            ),
            "DEEPAKNTR", new PriceSnapshot(
                    "DEEPAKNTR", bd(2140), bd(2450), bd(1780), bd(28200), bd(35),
                    bd(9.2), bd(12.8), bd(17.6), bd(0.28), bd(15.8), bd(14.2),
                    bd(410), bd(51.86), bd(19.4)
            )
    );

    @Override
    public Optional<PriceSnapshot> fetchSnapshot(String symbol) {
        PriceSnapshot base = BASE_DATA.get(symbol.toUpperCase());
        if (base == null) {
            return Optional.empty();
        }
        return Optional.of(jitter(base));
    }

    /** Applies small random variation so repeated refreshes look "live". */
    private PriceSnapshot jitter(PriceSnapshot base) {
        double factor = ThreadLocalRandom.current().nextDouble(0.98, 1.02);
        return new PriceSnapshot(
                base.symbol(),
                base.currentPrice().multiply(bd(factor)).setScale(2, java.math.RoundingMode.HALF_UP),
                base.week52High(),
                base.week52Low(),
                base.marketCapCr(),
                base.peRatio(),
                base.revenueGrowthPct(),
                base.profitGrowthPct(),
                base.operatingMarginPct(),
                base.debtToEquity(),
                base.roce(),
                base.roe(),
                base.operatingCashFlowCr(),
                base.promoterHoldingPct(),
                base.institutionalHoldingPct()
        );
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }
}
