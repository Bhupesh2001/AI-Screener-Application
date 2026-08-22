package com.stockresearch.service;

import com.stockresearch.domain.Company;
import com.stockresearch.domain.News;
import com.stockresearch.dto.NewsDto;
import com.stockresearch.repository.CompanyRepository;
import com.stockresearch.repository.NewsRepository;
import com.stockresearch.service.datasource.NewsSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final NewsRepository newsRepository;
    private final CompanyRepository companyRepository;
    private final NewsSource newsSource;

    @Transactional(readOnly = true)
    public List<NewsDto> getRecentNews(int limit) {
        return newsRepository.findAllByOrderByPublishedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NewsDto> getNewsForCompany(Long companyId) {
        return newsRepository.findByCompanyIdOrderByPublishedAtDesc(companyId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Manually refresh news for a specific company (on-demand).
     * This will fetch fresh data from the news source and save it.
     * The cache for this company’s news will be evicted.
     */
    @Transactional
    @CacheEvict(value = "news", key = "#companyId")
    public List<NewsDto> refreshNewsForCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        log.info("Manually refreshing news for {}", company.getSymbol());

        // Delete existing news for this company? Or keep history? 
        // We'll just add new ones; duplicates are filtered by headline in detectNews.
        List<NewsSource.NewsItem> items = newsSource.fetchRecentNews(company.getSymbol(), company.getName());

        List<News> savedNews = items.stream()
                .filter(n -> !newsAlreadyExists(company, n))
                .map(n -> {
                    News news = News.builder()
                            .company(company)
                            .headline(n.headline())
                            .summary(n.summary())
                            .url(n.url())
                            .sourceName(n.sourceName())
                            .publishedAt(n.publishedAt())
                            .matchesGovernmentTheme(containsGovernmentKeyword(n))
                            .build();
                    return newsRepository.save(news);
                })
                .collect(Collectors.toList());

        log.info("Saved {} new news items for {}", savedNews.size(), company.getSymbol());
        return savedNews.stream().map(this::toDto).collect(Collectors.toList());
    }

    // Copy these helpers from DiscoveryPipeline or extract to a shared utility
    private boolean newsAlreadyExists(Company company, NewsSource.NewsItem n) {
        return newsRepository.findByCompanyIdOrderByPublishedAtDesc(company.getId()).stream()
                .anyMatch(existing -> existing.getHeadline().equals(n.headline()));
    }

    private static final List<String> GOV_KEYWORDS = List.of(
            "pli", "defense", "defence", "railway", "power grid", "renewable", "solar",
            "semiconductor", "electronics", "telecom", "infrastructure", "atmanirbhar"
    );

    private boolean containsGovernmentKeyword(NewsSource.NewsItem n) {
        String haystack = ((n.headline() == null ? "" : n.headline())
                + " " + (n.summary() == null ? "" : n.summary())).toLowerCase();
        return GOV_KEYWORDS.stream().anyMatch(haystack::contains);
    }

    private NewsDto toDto(News news) {
        return NewsDto.builder()
                .id(news.getId())
                .companyId(news.getCompany().getId())
                .companySymbol(news.getCompany().getSymbol())
                .headline(news.getHeadline())
                .summary(news.getSummary())
                .url(news.getUrl())
                .sourceName(news.getSourceName())
                .publishedAt(news.getPublishedAt())
                .matchesGovernmentTheme(news.getMatchesGovernmentTheme())
                .build();
    }
}