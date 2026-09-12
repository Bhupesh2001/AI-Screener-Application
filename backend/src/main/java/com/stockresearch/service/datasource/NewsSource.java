package com.stockresearch.service.datasource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Abstraction over news providers (Google News RSS today; other APIs later).
 */
public interface NewsSource {

    List<NewsItem> fetchRecentNews(String companySymbol, String companyName);

    /**
     * Optional hook for implementations that cache their own underlying
     * data. Called before a manual "refresh" so the caller can force
     * genuinely fresh data instead of silently getting a cached response -
     * without this, a manual refresh triggered soon after a previous fetch
     * could quietly return stale data with no indication anything went
     * wrong. Default no-op for implementations with nothing to invalidate.
     */
    default void evictCache(String companySymbol) {
        // no-op by default
    }

    record NewsItem(
            String headline,
            String summary,
            String url,
            String sourceName,
            LocalDateTime publishedAt
    ) {}
}
