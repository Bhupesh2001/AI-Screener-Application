package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
//@Primary
public class NewsApiNewsSource implements NewsSource {

    private static final Logger log = LoggerFactory.getLogger(NewsApiNewsSource.class);

    private final WebClient webClient;

    public NewsApiNewsSource(
            @Value("${newsapi.api-key}") String apiKey,
            @Value("${newsapi.base-url:https://newsapi.org/v2}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Api-Key", apiKey)
                .build();
        log.info("NewsApiNewsSource initialized with baseUrl: {}", baseUrl);
    }

    @Override
    @Cacheable(value = "news", key = "#companySymbol", unless = "#result == null or #result.isEmpty()")
    public List<NewsItem> fetchRecentNews(String companySymbol, String companyName) {
        String query = "(" + companyName + " OR " + companySymbol + ")";
        String fromDate = Instant.now().minusSeconds(30 * 24 * 60 * 60L)
                .atOffset(ZoneOffset.UTC)
                .toLocalDate().toString();

        log.info("Fetching news for {} with query: {}", companySymbol, query);

        try {
            JsonNode root = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/everything")
                            .queryParam("q", query)
                            .queryParam("from", fromDate)
                            .queryParam("sortBy", "relevancy")
                            .queryParam("language", "en")
                            .queryParam("pageSize", 30)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (root == null) {
                log.warn("Received null response from NewsAPI for {}", companySymbol);
                return List.of();
            }

            log.info("NewsAPI response status: {}, totalResults: {}",
                    root.path("status").asText(),
                    root.path("totalResults").asInt());

            return parseResponse(root);

        } catch (Exception e) {
            log.error("NewsAPI request failed for {}: {}", companySymbol, e.getMessage(), e);
            return List.of();
        }
    }

    private List<NewsItem> parseResponse(JsonNode root) {
        List<NewsItem> items = new ArrayList<>();
        JsonNode articles = root.path("articles");

        if (!articles.isArray()) {
            log.warn("Expected 'articles' array but got: {}", articles.getNodeType());
            return items;
        }

        int totalArticles = articles.size();
        log.info("Parsing {} articles from NewsAPI", totalArticles);

        for (JsonNode article : articles) {
            try {
                String title = article.path("title").asText();
                if (title == null || title.isBlank()) {
                    log.debug("Skipping article with missing title");
                    continue;
                }

                String description = article.path("description").asText();
                String url = article.path("url").asText();
                String sourceName = article.path("source").path("name").asText();
                String publishedAt = article.path("publishedAt").asText();

                LocalDateTime date = parseDate(publishedAt);
                if (date == null) {
                    log.debug("Skipping article with unparseable date: {}", publishedAt);
                    continue;
                }

                items.add(new NewsItem(
                        title,
                        description,
                        url,
                        sourceName != null ? sourceName : "Unknown",
                        date
                ));
            } catch (Exception e) {
                log.warn("Error parsing an article: {}", e.getMessage());
            }
        }

        log.info("Successfully parsed {} news items", items.size());
        return items;
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return Instant.parse(dateStr).atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.debug("Failed to parse date: {}", dateStr);
            return null;
        }
    }
}