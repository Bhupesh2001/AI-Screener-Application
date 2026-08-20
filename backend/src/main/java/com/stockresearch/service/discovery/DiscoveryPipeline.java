package com.stockresearch.service.discovery;

import com.stockresearch.domain.*;
import com.stockresearch.repository.*;
import com.stockresearch.service.datasource.AnnouncementSource;
import com.stockresearch.service.datasource.FundamentalsDataSource;
import com.stockresearch.service.datasource.NewsSource;
import com.stockresearch.service.datasource.PriceDataSource;
import com.stockresearch.service.scoring.ScoringEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates the full discovery pipeline described in the spec:
 *   Stage 1 (Initial Universe)     -> CompanyRepository (companies already seeded/stored)
 *   Stage 2 (Fundamental Screening) -> FundamentalScreeningStage
 *   Stage 3 (Growth Detection)      -> captured inside ScoringEngine's growth-related rules
 *   Stage 4 (Event Detection)       -> AnnouncementSource -> Event entities
 *   Stage 5 (Govt Tailwind)         -> GovernmentTailwindScoreRule (within ScoringEngine)
 *   Stage 6 (Sector Strength)       -> SectorTailwindScoreRule (within ScoringEngine)
 *   Stage 7 (Technical Confirmation)-> TechnicalBreakoutScoreRule (within ScoringEngine)
 *   Stage 8 (AI Validation)         -> handled on-demand via ResearchService, not in this
 *                                      automatic pipeline (AI calls cost money/time, so
 *                                      they're user-triggered rather than run for every
 *                                      company on every refresh)
 *   Stage 9 (Final Ranking)         -> ScoreSnapshot persisted, sorted by totalScore
 *   Stage 10 (Reasoning)            -> ScoreReason entities attached to every snapshot
 * This class is what the scheduled background job (see scheduler package)
 * calls periodically, and what a manual "Refresh Now" button would call too.
 */
@Service
public class DiscoveryPipeline {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryPipeline.class);

    private final CompanyRepository companyRepository;
    private final EventRepository eventRepository;
    private final NewsRepository newsRepository;
    private final ScoreSnapshotRepository scoreSnapshotRepository;
    private final PriceDataSource priceDataSource;
    private final FundamentalsDataSource fundamentalsDataSource; // NEW
    private final NewsSource newsSource;
    private final AnnouncementSource announcementSource;
    private final FundamentalScreeningStage screeningStage;
    private final ScoringEngine scoringEngine;
    private final EventClassifier eventClassifier;

    public DiscoveryPipeline(
            CompanyRepository companyRepository,
            EventRepository eventRepository,
            NewsRepository newsRepository,
            ScoreSnapshotRepository scoreSnapshotRepository,
            PriceDataSource priceDataSource,
            FundamentalsDataSource fundamentalsDataSource, // NEW
            NewsSource newsSource,
            AnnouncementSource announcementSource,
            FundamentalScreeningStage screeningStage,
            ScoringEngine scoringEngine,
            EventClassifier eventClassifier
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
    }

    /** Runs the full pipeline for every company currently in the universe (Stage 1). */
    @Transactional
    public List<DiscoveryResult> runForAllCompanies(AppSettings settings) {
        List<Company> universe = companyRepository.findAll();
        return universe.stream().map(c -> runForCompany(c, settings)).toList();
    }

    public DiscoveryResult runForCompany(Company company, AppSettings settings) {
        // --- Refresh fundamentals/price from data source ---
        refreshFundamentals(company);

        // --- Stage 4: Event Detection - pull fresh announcements, classify, persist ---
        List<Event> newEvents = detectNewEvents(company);

        // --- Fetch/refresh news (also feeds Stage 5 Government Tailwind detection) ---
        List<News> newNews = detectNews(company);

        // --- Stage 2: Fundamental Screening ---
        Optional<String> exclusionReason = screeningStage.checkExclusion(company, settings);
        if (exclusionReason.isPresent()) {
            log.debug("Excluding {} from discovery: {}", company.getSymbol(), exclusionReason.get());
            return DiscoveryResult.excluded(company, exclusionReason.get());
        }

        // --- Stages 3, 5, 6, 7 all happen inside the scoring engine's rules ---
        List<Event> recentEvents = eventRepository.findByCompanyIdOrderByEventDateDesc(company.getId());
        List<News> recentNews = newsRepository.findByCompanyIdOrderByPublishedAtDesc(company.getId());
        ScoringEngine.ComputedScore score = scoringEngine.compute(company, recentEvents, recentNews);

        // --- Stage 9 + 10: persist the ranked score with full reasoning ---
        saveSnapshot(company, score);

        return DiscoveryResult.included(company, score);
    }

    private void refreshFundamentals(Company company) {
        // Price (from IndianApiPriceDataSource)
        priceDataSource.fetchSnapshot(company.getSymbol()).ifPresent(snap -> {
            company.setCurrentPrice(snap.currentPrice());
            company.setWeek52High(snap.week52High());
            company.setWeek52Low(snap.week52Low());
        });

        // Fundamentals (from IndianApiFundamentalsSource)
        fundamentalsDataSource.fetchFundamentals(company.getSymbol()).ifPresent(fund -> {
            company.setRevenueGrowthPct(fund.revenueGrowthPct());
            company.setProfitGrowthPct(fund.profitGrowthPct());
            company.setOperatingMarginPct(fund.operatingMarginPct());
            company.setDebtToEquity(fund.debtToEquity());
            company.setRoce(fund.roce());
            company.setRoe(fund.roe());
            company.setPromoterHoldingPct(fund.promoterHoldingPct());
            company.setInstitutionalHoldingPct(fund.institutionalHoldingPct());
            company.setMarketCapCr(fund.marketCapCr());   // NEW
            company.setPeRatio(fund.peRatio());           // NEW
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
