// backend/src/main/java/com/stockresearch/service/datasource/SectorIndexSource.java
package com.stockresearch.service.datasource;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Abstraction over "wherever we get sector-level index performance from" -
 * NSE's sectoral/thematic indices today. Used by SectorTailwindScoreRule to
 * compute a real tailwind signal instead of the static hardcoded tiering
 * this replaces.
 */
public interface SectorIndexSource {

    /**
     * Looks up performance for a specific index by its display name (e.g.
     * "NIFTY BANK"). Empty if the index doesn't exist on the source, or the
     * source couldn't be reached this cycle.
     */
    Optional<SectorIndexPerformance> getPerformance(String indexName);

    record SectorIndexPerformance(
            String indexName,
            BigDecimal currentValue,
            BigDecimal changePercent30d,
            BigDecimal changePercent365d
    ) {}
}
