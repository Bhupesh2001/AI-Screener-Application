// backend/src/main/java/com/stockresearch/service/datasource/GoogleNewsRssNewsSource.java
package com.stockresearch.service.datasource;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Real implementation of NewsSource backed by Google News' public RSS search
 * feed (news.google.com/rss/search) - free, keyless, no documented rate
 * limit, per the real-data integration plan section E.
 *
 * URL shape (verified against current docs/examples, not fetched live from
 * this environment - see class-level caveat below):
 *   https://news.google.com/rss/search?q={query}&hl=en-IN&gl=IN&ceid=IN:en
 *
 * Response is plain RSS 2.0 XML. Per item, the fields that matter here:
 *   - title: Google appends " - PublisherName" to the actual headline; we
 *     strip that suffix so NewsItem.headline is just the headline, since
 *     sourceName is already carried separately.
 *   - link: a news.google.com/rss/articles/... REDIRECT url, not the
 *     original publisher URL. Still usable as a "read more" link, just
 *     worth knowing it's not a direct publisher link.
 *   - pubDate: RFC-822 format (e.g. "Fri, 14 Aug 2026 10:30:00 GMT").
 *   - source: a child <source> element carrying the publisher name.
 *   - description: HTML markup (a link + styling), not plain text - we
 *     strip tags via jsoup (already a dependency from the fundamentals
 *     scraper) rather than surface raw HTML into News.summary.
 *
 * CAVEAT: I verified the URL format, query semantics (including the
 * "when:Nd" recency modifier), and general item shape against current
 * (2026) documentation and code examples, but could not fetch a live raw
 * XML sample directly from this environment to confirm byte-for-byte. This
 * implementation parses defensively (try/catch per item, never throws out
 * of fetchRecentNews) so a shape mismatch degrades to fewer/no results
 * rather than breaking the pipeline - but a live smoke test is still
 * warranted before trusting this in production, same as Phase 1/2.
 *
 * NOTE on scope: this genuinely improves GovernmentTailwindScoreRule (which
 * keyword-matches against recentNews directly). It does NOT feed
 * RecentEventsScoreRule, which only reads recentEvents - News and Event are
 * intentionally separate concepts here (see News.java's Javadoc), and
 * DiscoveryPipeline never converts one into the other. RecentEventsScoreRule
 * is already on real data via Phase 2's NSE announcements.
 */
@Primary
@Component
class GoogleNewsRssNewsSource implements NewsSource {

    private static final Logger log = LoggerFactory.getLogger(GoogleNewsRssNewsSource.class);

    private static final String RSS_SEARCH_URL = "https://news.google.com/rss/search";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    // Scope results to roughly the last month - recent enough to matter for
    // scoring, without pulling years of history for well-known companies.
    private static final String RECENCY_MODIFIER = " when:30d";

    private final WebClient webClient;

    public GoogleNewsRssNewsSource() {
        this.webClient = WebClient.builder()
                .baseUrl(RSS_SEARCH_URL)
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    @Override
    @Cacheable(value = "news", key = "#companySymbol", unless = "#result == null or #result.isEmpty()")
    public List<NewsItem> fetchRecentNews(String companySymbol, String companyName) {
        // Company names read more naturally in news prose than exchange
        // symbols (e.g. "Rail Vikas Nigam" vs "RVNL"), so that's the primary
        // query - matching the plan's own example. Falling back to the
        // symbol if the name yields nothing is a reasonable future addition
        // if smaller/lesser-covered companies turn up empty in practice.
        String query = "\"" + companyName + "\" stock" + RECENCY_MODIFIER;

        try {
            String xml = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("q", URLEncoder.encode(query, StandardCharsets.UTF_8))
                            .queryParam("hl", "en-IN")
                            .queryParam("gl", "IN")
                            .queryParam("ceid", "IN:en")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xml == null || xml.isBlank()) {
                log.warn("Empty response from Google News RSS for {}", companyName);
                return List.of();
            }

            List<NewsItem> items = parseFeed(xml);
            log.info("Fetched {} news items for {} from Google News RSS", items.size(), companyName);
            return items;

        } catch (Exception e) {
            log.warn("Failed to fetch/parse Google News RSS for {}: {}", companyName, e.getMessage());
            return List.of();
        }
    }

    private List<NewsItem> parseFeed(String xml) {
        List<NewsItem> result = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Defensive baseline for any XML parser fed content from the
            // public internet - not strictly required for Google's own feed,
            // but cheap insurance against external-entity-style XML abuse.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList itemNodes = doc.getElementsByTagName("item");
            for (int i = 0; i < itemNodes.getLength(); i++) {
                try {
                    NewsItem item = parseItem((Element) itemNodes.item(i));
                    if (item != null) {
                        result.add(item);
                    }
                } catch (Exception e) {
                    // one malformed item shouldn't drop the rest of the feed
                    log.debug("Skipping unparseable RSS item: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Google News RSS XML: {}", e.getMessage());
        }

        return result;
    }

    // News columns: headline VARCHAR(1024), summary VARCHAR(4000),
    // url VARCHAR(1024), sourceName VARCHAR(128). Headline/summary/source
    // are low-risk in practice (news prose is short), but Google's redirect
    // URLs (news.google.com/rss/articles/CBMi...) embed base64 tracking data
    // and can run long - truncating all four defensively costs nothing and
    // keeps this consistent with NseAnnouncementSource's same guard.
    private static final int MAX_HEADLINE_LENGTH = 1000;
    private static final int MAX_SUMMARY_LENGTH = 3990;
    private static final int MAX_URL_LENGTH = 1000;
    private static final int MAX_SOURCE_NAME_LENGTH = 120;

    private NewsItem parseItem(Element item) {
        String rawTitle = childText(item, "title");
        if (rawTitle == null || rawTitle.isBlank()) {
            return null; // no headline, nothing useful to keep
        }

        String sourceName = truncate(childText(item, "source"), MAX_SOURCE_NAME_LENGTH);
        String headline = truncate(stripSourceSuffix(rawTitle, sourceName), MAX_HEADLINE_LENGTH);

        String link = truncate(childText(item, "link"), MAX_URL_LENGTH);
        String descriptionHtml = childText(item, "description");
        String summary = descriptionHtml != null
                ? truncate(Jsoup.parse(descriptionHtml).text(), MAX_SUMMARY_LENGTH)
                : null;

        LocalDateTime publishedAt = parsePubDate(childText(item, "pubDate"));
        if (publishedAt == null) {
            // Same reasoning as NseAnnouncementSource: without a date we
            // can't place this in a timeline or dedupe it reliably later.
            return null;
        }

        return new NewsItem(headline, summary, link, sourceName, publishedAt);
    }

    /**
     * Google formats item titles as "Actual Headline - Publisher Name".
     * Since we already carry the publisher separately via <source>, strip
     * that redundant suffix when it's present and matches.
     */
    private String stripSourceSuffix(String title, String sourceName) {
        if (sourceName == null || sourceName.isBlank()) return title;
        String suffix = " - " + sourceName;
        if (title.endsWith(suffix)) {
            return title.substring(0, title.length() - suffix.length()).trim();
        }
        return title;
    }

    private LocalDateTime parsePubDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) return null;
        try {
            return ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.debug("Unparseable pubDate '{}': {}", pubDate, e.getMessage());
            return null;
        }
    }

    private String childText(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        if (children.getLength() == 0) return null;
        Node node = children.item(0);
        String text = node.getTextContent();
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "…";
    }
}
