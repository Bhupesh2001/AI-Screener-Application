package com.stockresearch.service.datasource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Abstraction over news providers (Google News RSS today; other APIs later).
 */
public interface NewsSource {

    List<NewsItem> fetchRecentNews(String companySymbol, String companyName);

    record NewsItem(
            String headline,
            String summary,
            String url,
            String sourceName,
            LocalDateTime publishedAt
    ) {}
}
