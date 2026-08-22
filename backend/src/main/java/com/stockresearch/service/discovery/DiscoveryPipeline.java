package com.stockresearch.service.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockresearch.domain.*;
import com.stockresearch.exceptions.RateLimitExceededException;
import com.stockresearch.repository.*;
import com.stockresearch.service.datasource.*;
import com.stockresearch.service.scoring.ScoringEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DiscoveryPipeline {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryPipeline.class);

    private final CompanyRepository companyRepository;
    private final EventRepository eventRepository;
    private final NewsRepository newsRepository;
    private final ScoreSnapshotRepository scoreSnapshotRepository;
    private final PriceDataSource priceDataSource;
    private final FundamentalsDataSource fundamentalsDataSource;
    private final NewsSource newsSource;
    private final AnnouncementSource announcementSource;
    private final FundamentalScreeningStage screeningStage;
    private final ScoringEngine scoringEngine;
    private final EventClassifier eventClassifier;

    // Cast to our specific implementations for the parseFromNode methods
    private final IndianApiPriceDataSource indianApiPriceDataSource;
    private final IndianApiFundamentalsSource indianApiFundamentalsSource;
    private final IndianApiAnnouncementSource indianApiAnnouncementSource;
    private final IndianApiClient indianApiClient;

    public DiscoveryPipeline(
            CompanyRepository companyRepository,
            EventRepository eventRepository,
            NewsRepository newsRepository,
            ScoreSnapshotRepository scoreSnapshotRepository,
            PriceDataSource priceDataSource,
            FundamentalsDataSource fundamentalsDataSource,
            NewsSource newsSource,
            AnnouncementSource announcementSource,
            FundamentalScreeningStage screeningStage,
            ScoringEngine scoringEngine,
            EventClassifier eventClassifier,
            IndianApiPriceDataSource indianApiPriceDataSource,
            IndianApiFundamentalsSource indianApiFundamentalsSource,
            IndianApiAnnouncementSource indianApiAnnouncementSource,
            IndianApiClient indianApiClient
    ) {
        this.companyRepository = companyRepository;
        this.eventRepository = eventRepository;
        this.newsRepository = newsRepository;
        this.scoreSnapshotRepository = scoreSnapshotRepository;
        this.priceDataSource = priceDataSource;
        this.fundamentalsDataSource = fundamentalsDataSource;
        this.newsSource = newsSource;
        this.announcementSource = announcementSource;
        this.screeningStage = screeningStage;
        this.scoringEngine = scoringEngine;
        this.eventClassifier = eventClassifier;
        this.indianApiPriceDataSource = indianApiPriceDataSource;
        this.indianApiFundamentalsSource = indianApiFundamentalsSource;
        this.indianApiAnnouncementSource = indianApiAnnouncementSource;
        this.indianApiClient = indianApiClient;
    }

    @Transactional
    public List<DiscoveryResult> runForAllCompanies(AppSettings settings) {
        // Get companies ordered by last update (oldest first)
        List<Company> orderedUniverse = companyRepository.findAllOrderByLastScoreAsc();
        List<DiscoveryResult> results = new ArrayList<>();
        int processed = 0;
        int total = orderedUniverse.size();

        for (Company c : orderedUniverse) {
            try {
                DiscoveryResult result = runForCompany(c, settings);
                results.add(result);
                processed++;
            } catch (RateLimitExceededException e) {
                // We hit the rate limit – stop processing
                log.warn("Rate limit hit after processing {} companies. Remaining: {}", processed, total - processed);
                break;
            } catch (Exception e) {
                log.error("Error processing {}: {}", c.getSymbol(), e.getMessage());
                results.add(DiscoveryResult.excluded(c, "Error: " + e.getMessage()));
            }
            if (processed % 50 == 0) {
                log.info("Processed {} / {} companies", processed, total);
            }
        }
        log.info("Refresh complete :: {} = processed, {} = remaining (rate limited or errors)", processed, total - processed);
        return results;
    }

    public DiscoveryResult runForCompany(Company company, AppSettings settings) {
        // --- OPTIMIZATION: Fetch JSON once and reuse ---
        JsonNode root = null;
        try {
            root = indianApiClient.getStockData(company.getSymbol());
        } catch (RateLimitExceededException e) {
            // Propagate to stop the whole batch
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch stock data for {}: {}", company.getSymbol(), e.getMessage());
            return runForCompanyWithSeparateFetches(company, settings);
        }

        // --- Refresh fundamentals/price from the cached JSON ---
        refreshFundamentalsFromNode(company, root);

        // --- Stage 4: Event Detection - parse from cached JSON ---
        List<Event> newEvents = detectNewEventsFromNode(company, root);

        // --- Fetch/refresh news (still separate, as it's a different source) ---
        List<News> newNews = detectNews(company);

        // --- Stage 2: Fundamental Screening ---
        Optional<String> exclusionReason = screeningStage.checkExclusion(company, settings);
        if (exclusionReason.isPresent()) {
            log.debug("Excluding {} from discovery: {}", company.getSymbol(), exclusionReason.get());
            return DiscoveryResult.excluded(company, exclusionReason.get());
        }

        // --- Stages 3, 5, 6, 7: Scoring ---
        List<Event> recentEvents = eventRepository.findByCompanyIdOrderByEventDateDesc(company.getId());
        List<News> recentNews = newsRepository.findByCompanyIdOrderByPublishedAtDesc(company.getId());
        ScoringEngine.ComputedScore score = scoringEngine.compute(company, recentEvents, recentNews);

        // --- Stage 9 + 10: persist score ---
        saveSnapshot(company, score);

        return DiscoveryResult.included(company, score);
    }

    /**
     * Fallback method if the single-fetch approach fails.
     * Uses the original separate fetch logic.
     */
    private DiscoveryResult runForCompanyWithSeparateFetches(Company company, AppSettings settings) {
        log.info("Using fallback (separate fetches) for {}", company.getSymbol());
        refreshFundamentals(company);
        List<Event> newEvents = detectNewEvents(company);
        List<News> newNews = detectNews(company);

        Optional<String> exclusionReason = screeningStage.checkExclusion(company, settings);
        if (exclusionReason.isPresent()) {
            return DiscoveryResult.excluded(company, exclusionReason.get());
        }

        List<Event> recentEvents = eventRepository.findByCompanyIdOrderByEventDateDesc(company.getId());
        List<News> recentNews = newsRepository.findByCompanyIdOrderByPublishedAtDesc(company.getId());
        ScoringEngine.ComputedScore score = scoringEngine.compute(company, recentEvents, recentNews);
        saveSnapshot(company, score);
        return DiscoveryResult.included(company, score);
    }

    /**
     * Refresh fundamentals from a pre-fetched JsonNode.
     * This eliminates the need for separate API calls.
     */
    private void refreshFundamentalsFromNode(Company company, JsonNode root) {
        // Price data from node
        indianApiPriceDataSource.parseFromNode(company.getSymbol(), root).ifPresent(snap -> {
            company.setCurrentPrice(snap.currentPrice());
            company.setWeek52High(snap.week52High());
            company.setWeek52Low(snap.week52Low());
        });

        // Fundamentals from node
        indianApiFundamentalsSource.parseFromNode(company.getSymbol(), root).ifPresent(fund -> {
            company.setRevenueGrowthPct(fund.revenueGrowthPct());
            company.setProfitGrowthPct(fund.profitGrowthPct());
            company.setOperatingMarginPct(fund.operatingMarginPct());
            company.setDebtToEquity(fund.debtToEquity());
            company.setRoce(fund.roce());
            company.setRoe(fund.roe());
            company.setPromoterHoldingPct(fund.promoterHoldingPct());
            company.setInstitutionalHoldingPct(fund.institutionalHoldingPct());
            company.setMarketCapCr(fund.marketCapCr());
            company.setPeRatio(fund.peRatio());
        });

        company.setLastRefreshedAt(LocalDateTime.now());
        companyRepository.save(company);
    }

    /**
     * Detect new events from a pre-fetched JsonNode.
     */
    private List<Event> detectNewEventsFromNode(Company company, JsonNode root) {
        List<AnnouncementSource.RawAnnouncement> raw =
                indianApiAnnouncementSource.parseFromNode(company.getSymbol(), root);
        return raw.stream()
                .filter(a -> !eventAlreadyExists(company, a))
                .map(a -> {
                    Event event = eventClassifier.classify(company, a);
                    return eventRepository.save(event);
                })
                .toList();
    }

    // --- Original methods (kept for fallback and news) ---

    private void refreshFundamentals(Company company) {
        priceDataSource.fetchSnapshot(company.getSymbol()).ifPresent(snap -> {
            company.setCurrentPrice(snap.currentPrice());
            company.setWeek52High(snap.week52High());
            company.setWeek52Low(snap.week52Low());
        });

        fundamentalsDataSource.fetchFundamentals(company.getSymbol()).ifPresent(fund -> {
            company.setRevenueGrowthPct(fund.revenueGrowthPct());
            company.setProfitGrowthPct(fund.profitGrowthPct());
            company.setOperatingMarginPct(fund.operatingMarginPct());
            company.setDebtToEquity(fund.debtToEquity());
            company.setRoce(fund.roce());
            company.setRoe(fund.roe());
            company.setPromoterHoldingPct(fund.promoterHoldingPct());
            company.setInstitutionalHoldingPct(fund.institutionalHoldingPct());
            company.setMarketCapCr(fund.marketCapCr());
            company.setPeRatio(fund.peRatio());
        });

        company.setLastRefreshedAt(LocalDateTime.now());
        companyRepository.save(company);
    }

    private List<Event> detectNewEvents(Company company) {
        List<AnnouncementSource.RawAnnouncement> raw = announcementSource.fetchRecentAnnouncements(company.getSymbol());
        return raw.stream()
                .filter(a -> !eventAlreadyExists(company, a))
                .map(a -> {
                    Event event = eventClassifier.classify(company, a);
                    return eventRepository.save(event);
                })
                .toList();
    }

    private boolean eventAlreadyExists(Company company, AnnouncementSource.RawAnnouncement a) {
        return eventRepository.findByCompanyIdOrderByEventDateDesc(company.getId()).stream()
                .anyMatch(e -> e.getTitle().equals(a.title()) && e.getEventDate().equals(a.announcementDate()));
    }

    private List<News> detectNews(Company company) {
        return newsSource.fetchRecentNews(company.getSymbol(), company.getName()).stream()
                .filter(n -> !newsAlreadyExists(company, n))
                .map(n -> newsRepository.save(News.builder()
                        .company(company)
                        .headline(n.headline())
                        .summary(n.summary())
                        .url(n.url())
                        .sourceName(n.sourceName())
                        .publishedAt(n.publishedAt())
                        .matchesGovernmentTheme(containsGovernmentKeyword(n))
                        .build()))
                .toList();
    }

    private boolean newsAlreadyExists(Company company, NewsSource.NewsItem n) {
        return newsRepository.findByCompanyIdOrderByPublishedAtDesc(company.getId()).stream()
                .anyMatch(existing -> existing.getHeadline().equals(n.headline()));
    }

    private static final List<String> GOV_KEYWORDS = List.of(
            "pli", "defense", "defence", "railway", "power grid", "renewable", "solar",
            "semiconductor", "electronics", "ev", "telecom", "infrastructure", "atmanirbhar"
    );

    private boolean containsGovernmentKeyword(NewsSource.NewsItem n) {
        String haystack = ((n.headline() == null ? "" : n.headline())
                + " " + (n.summary() == null ? "" : n.summary())).toLowerCase();
        return GOV_KEYWORDS.stream().anyMatch(haystack::contains);
    }

    private void saveSnapshot(Company company, ScoringEngine.ComputedScore score) {
        Map<String, Integer> cat = score.categoryScores();
        ScoreSnapshot snapshot = ScoreSnapshot.builder()
                .company(company)
                .totalScore(score.totalScore())
                .sectorTailwindScore(cat.get("Sector Tailwind"))
                .governmentPolicyScore(cat.get("Government Policy"))
                .revenueGrowthScore(cat.get("Revenue Growth"))
                .profitGrowthScore(cat.get("Profit Growth"))
                .marginExpansionScore(cat.get("Margin Expansion"))
                .debtReductionScore(cat.get("Debt Reduction"))
                .roceScore(cat.get("ROCE"))
                .roeScore(cat.get("ROE"))
                .orderBookScore(cat.get("Order Book"))
                .capacityExpansionScore(cat.get("Capacity Expansion"))
                .newProductsScore(cat.get("New Products"))
                .exportsScore(cat.get("Exports"))
                .promoterBuyingScore(cat.get("Promoter Buying"))
                .institutionalBuyingScore(cat.get("Institutional Buying"))
                .technicalBreakoutScore(cat.get("Technical Breakout"))
                .valuationScore(cat.get("Valuation"))
                .recentEventsScore(cat.get("Recent Events"))
                .computedAt(LocalDateTime.now())
                .build();

        for (ScoreReason reason : score.reasons()) {
            reason.setScoreSnapshot(snapshot);
            snapshot.getReasons().add(reason);
        }

        scoreSnapshotRepository.save(snapshot);
    }
}