package com.stockresearch.service;

import com.stockresearch.domain.Company;
import com.stockresearch.domain.News;
import com.stockresearch.dto.NewsDto;
import com.stockresearch.repository.CompanyRepository;
import com.stockresearch.repository.NewsRepository;
import com.stockresearch.service.datasource.NewsSource;
import com.stockresearch.util.GovernmentTailwindKeywords;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     */
    @Transactional
    public List<NewsDto> refreshNewsForCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        log.info("Manually refreshing news for {}", company.getSymbol());

        // Force genuinely fresh data - the old @CacheEvict(value = "news",
        // key = "#companyId") here was silently ineffective for two
        // independent reasons: it targeted a Spring-managed cache that the
        // active IndianApiNewsSource never touches at all (it piggybacks on
        // IndianApiClient's own internal cache instead), AND even for
        // GoogleNewsRssNewsSource - which does use that cache - it's keyed
        // by symbol, not company ID, so the eviction key never matched
        // either. Delegating to the active source's own evictCache()
        // (see NewsSource) lets whichever implementation is active
        // invalidate whatever it actually needs to.
        newsSource.evictCache(company.getSymbol());

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

    private boolean containsGovernmentKeyword(NewsSource.NewsItem n) {
        String haystack = (n.headline() == null ? "" : n.headline())
                + " " + (n.summary() == null ? "" : n.summary());
        return GovernmentTailwindKeywords.matches(haystack);
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