package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Real implementation of NewsSource backed by the "recentNews" array
 * already present inside IndianAPI's /stock response - the same response
 * DiscoveryPipeline fetches once per company for price/fundamentals. This
 * means news costs ZERO additional API calls: parseFromNode() reads
 * directly from the already-fetched JsonNode, and even fetchRecentNews()
 * (the standalone interface path) relies on IndianApiClient's existing
 * 1-hour cache rather than issuing its own request, since it calls
 * getStockData() with the same symbol runForCompany() already fetched
 * moments earlier.
 *
 * Response shape confirmed against a real /stock sample (Tata Steel), not
 * assumed:
 *   recentNews: [{ headline, date, summary, url, ... }, ...]
 *   - date is ISO-8601 with a "+0000"-style offset (no colon), e.g.
 *     "2026-08-26T13:00:02+0000" - this needs the "Z" pattern letter, not
 *     DateTimeFormatter.ISO_OFFSET_DATE_TIME, which requires a colon in the
 *     offset and would throw on this exact format.
 *   - headline AND summary both sometimes carry embedded HTML (e.g.
 *     "<span class='webrupee'>₹</span>890 crore") - stripped via jsoup,
 *     same approach already used in GoogleNewsRssNewsSource for the same
 *     underlying problem.
 *   - url is a RELATIVE path (e.g. "/companies/news/...html"), not
 *     absolute. All 10/10 items in the verified sample resolved against
 *     livemint.com, so that's used as the default base - but this is
 *     evidence from one sample, not a documented guarantee, so an
 *     already-absolute url is used as-is rather than blindly prefixed.
 *
 * IMPORTANT CHARACTERISTIC, not a bug: this feed is not narrowly scoped to
 * the company itself. The verified sample for Tata Steel included generic
 * Sensex/Nifty market roundups, news about Tata Sons (the parent holding
 * company's leadership), a Tata-group-affiliated football club sale, and
 * even a competitor's (Jindal Steel) capacity expansion. Treat this as
 * "market/group/sector context loosely associated with the company", not
 * "news specifically about this company's own fundamentals" - genuinely
 * useful context for a human reading the company page, but broader than a
 * company-only search would return. No filtering is applied here to narrow
 * this down; that's a deliberate choice to avoid silently deciding on the
 * user's behalf what counts as "relevant enough".
 */
@Primary
@Component
@RequiredArgsConstructor
@Slf4j
public class IndianApiNewsSource implements NewsSource {

    private final IndianApiClient apiClient;

    private static final String DEFAULT_BASE_URL = "https://www.livemint.com";

    // Handles "2026-08-26T13:00:02+0000" - the "Z" pattern letter (as
    // opposed to ISO_OFFSET_DATE_TIME) accepts a +HHMM offset without a
    // colon, matching this exact format.
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    // News columns: headline VARCHAR(1024), summary VARCHAR(4000),
    // url VARCHAR(1024), sourceName VARCHAR(128) - truncate defensively,
    // same discipline as every other source in this app.
    private static final int MAX_HEADLINE_LENGTH = 1000;
    private static final int MAX_SUMMARY_LENGTH = 3990;
    private static final int MAX_URL_LENGTH = 1000;
    private static final int MAX_SOURCE_NAME_LENGTH = 120;

    @Override
    public void evictCache(String companySymbol) {
        apiClient.evictCache(companySymbol);
    }

    @Override
    public List<NewsItem> fetchRecentNews(String companySymbol, String companyName) {
        try {
            JsonNode root = apiClient.getStockData(companySymbol);
            return parseFromNode(companySymbol, root);
        } catch (Exception e) {
            log.warn("Failed to fetch news for {}: {}", companySymbol, e.getMessage());
            return List.of();
        }
    }

    /**
     * Parses news from an already-fetched JsonNode - the path
     * DiscoveryPipeline actually uses, avoiding any additional network call
     * (not even a cache lookup, since the node is already in memory).
     */
    public List<NewsItem> parseFromNode(String companySymbol, JsonNode root) {
        List<NewsItem> result = new ArrayList<>();
        if (root == null) {
            return result;
        }

        JsonNode newsArray = root.path("recentNews");
        if (!newsArray.isArray()) {
            log.debug("No recentNews array in /stock response for {}", companySymbol);
            return result;
        }

        for (JsonNode item : newsArray) {
            try {
                NewsItem parsed = parseNewsItem(item);
                if (parsed != null) {
                    result.add(parsed);
                }
            } catch (Exception e) {
                log.debug("Skipping unparseable news item for {}: {}", companySymbol, e.getMessage());
            }
        }

        log.info("Parsed {} news items for {} from /stock recentNews", result.size(), companySymbol);
        return result;
    }

    private NewsItem parseNewsItem(JsonNode item) {
        String rawHeadline = item.path("headline").asText(null);
        if (rawHeadline == null || rawHeadline.isBlank()) {
            return null;
        }

        String headline = truncate(stripHtml(rawHeadline), MAX_HEADLINE_LENGTH);

        String rawSummary = item.path("summary").asText(null);
        String summary = rawSummary != null ? truncate(stripHtml(rawSummary), MAX_SUMMARY_LENGTH) : null;

        String url = resolveUrl(item.path("url").asText(null));
        String sourceName = deriveSourceName(url);

        LocalDateTime publishedAt = parseDate(item.path("date").asText(null));
        if (publishedAt == null) {
            // Without a reliable date this can't be placed in a timeline or
            // deduped against future refreshes - skip, same discipline as
            // every other source in this app.
            return null;
        }

        return new NewsItem(
                headline,
                summary,
                truncate(url, MAX_URL_LENGTH),
                sourceName != null ? truncate(sourceName, MAX_SOURCE_NAME_LENGTH) : null,
                publishedAt
        );
    }

    private String resolveUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return null;
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            return rawUrl;
        }
        return DEFAULT_BASE_URL + (rawUrl.startsWith("/") ? rawUrl : "/" + rawUrl);
    }

    private String deriveSourceName(String url) {
        if (url == null) return null;
        if (url.contains("livemint.com")) return "Livemint";
        try {
            java.net.URI uri = java.net.URI.create(url);
            return uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return OffsetDateTime.parse(dateStr, DATE_FORMAT).toLocalDateTime();
        } catch (Exception e) {
            log.debug("Could not parse news date '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }

    private String stripHtml(String text) {
        if (text == null) return null;
        return Jsoup.parse(text).text();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "…";
    }
}
