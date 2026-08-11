package com.stockresearch.service;

import com.stockresearch.domain.*;
import com.stockresearch.dto.*;
import com.stockresearch.repository.*;
import com.stockresearch.service.scoring.ScoringEngine;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Backs Stock Explorer (search + filters) and the Company Detail page.
 * Converts entities to DTOs, joining in the latest/previous score snapshots,
 * recent events, recent news, and latest AI research.
 */
@Service
public class CompanyQueryService {

    private final CompanyRepository companyRepository;
    private final ScoreSnapshotRepository scoreSnapshotRepository;
    private final EventRepository eventRepository;
    private final NewsRepository newsRepository;
    private final ResearchSummaryRepository researchSummaryRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final ScoringEngine scoringEngine;

    public CompanyQueryService(
            CompanyRepository companyRepository,
            ScoreSnapshotRepository scoreSnapshotRepository,
            EventRepository eventRepository,
            NewsRepository newsRepository,
            ResearchSummaryRepository researchSummaryRepository,
            WatchlistItemRepository watchlistItemRepository,
            ScoringEngine scoringEngine
    ) {
        this.companyRepository = companyRepository;
        this.scoreSnapshotRepository = scoreSnapshotRepository;
        this.eventRepository = eventRepository;
        this.newsRepository = newsRepository;
        this.researchSummaryRepository = researchSummaryRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.scoringEngine = scoringEngine;
    }

    @Transactional(readOnly = true)
    public List<CompanySummaryDto> search(String query) {
        List<Company> matches = companyRepository.search(query);
        return matches.stream().map(this::toSummaryDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CompanySummaryDto> filter(CompanyFilterRequest req) {
        List<Company> matches = companyRepository.filter(
                req.getSector(),
                req.getMinMarketCapCr(),
                req.getMaxMarketCapCr(),
                req.getMaxPe(),
                req.getMinRoce(),
                req.getMaxDebtToEquity(),
                req.getMinRevenueGrowthPct(),
                req.getMinProfitGrowthPct()
        );

        return matches.stream()
                .map(this::toSummaryDto)
                .filter(dto -> req.getMinScore() == null || (dto.getCurrentScore() != null && dto.getCurrentScore() >= req.getMinScore()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CompanyDetailDto> getCompanyDetail(Long companyId) {
        return companyRepository.findById(companyId).map(this::toDetailDto);
    }

    public Optional<CompanyDetailDto> getCompanyDetailBySymbol(String symbol) {
        return companyRepository.findBySymbolIgnoreCase(symbol).map(this::toDetailDto);
    }

    public Optional<ScoreChangeDto> getScoreChange(Long companyId) {
        List<ScoreSnapshot> history = scoreSnapshotRepository.findByCompanyIdOrderByComputedAtDesc(companyId, PageRequest.of(0, 2));
        if (history.isEmpty()) return Optional.empty();

        ScoreSnapshot current = history.get(0);
        ScoreSnapshot previous = history.size() > 1 ? history.get(1) : null;

        return Optional.of(ScoreChangeDto.builder()
                .currentScore(current.getTotalScore())
                .previousScore(previous != null ? previous.getTotalScore() : null)
                .delta(previous != null ? current.getTotalScore() - previous.getTotalScore() : null)
                .reasons(toReasonDtos(current))
                .build());
    }

    public Optional<WhyInterestingDto> getWhyInteresting(Long companyId) {
        Optional<ScoreSnapshot> latest = scoreSnapshotRepository.findMostRecent(companyId);
        if (latest.isEmpty()) return Optional.empty();

        ScoreSnapshot snap = latest.get();
        Company company = snap.getCompany();

        List<ScoreReasonDto> positives = snap.getReasons().stream()
                .filter(r -> r.getType() == ScoreReason.ReasonType.POSITIVE)
                .map(this::toReasonDto)
                .toList();
        List<ScoreReasonDto> cautions = snap.getReasons().stream()
                .filter(r -> r.getType() != ScoreReason.ReasonType.POSITIVE)
                .map(this::toReasonDto)
                .toList();

        return Optional.of(WhyInterestingDto.builder()
                .companyId(company.getId())
                .symbol(company.getSymbol())
                .totalScore(snap.getTotalScore())
                .starRating(scoringEngine.toStarRating(snap.getTotalScore()))
                .label(scoringEngine.toLabel(snap.getTotalScore()))
                .positives(positives)
                .cautions(cautions)
                .build());
    }

    // ---- conversion helpers ----

    private CompanySummaryDto toSummaryDto(Company company) {
        List<ScoreSnapshot> history = scoreSnapshotRepository.findByCompanyIdOrderByComputedAtDesc(company.getId(), PageRequest.of(0, 2));
        Integer current = history.isEmpty() ? null : history.get(0).getTotalScore();
        Integer previous = history.size() > 1 ? history.get(1).getTotalScore() : null;

        return CompanySummaryDto.builder()
                .id(company.getId())
                .symbol(company.getSymbol())
                .name(company.getName())
                .sector(company.getSector())
                .marketCapCr(company.getMarketCapCr())
                .currentPrice(company.getCurrentPrice())
                .currentScore(current)
                .previousScore(previous)
                .scoreChange(current != null && previous != null ? current - previous : null)
                .build();
    }

    private CompanyDetailDto toDetailDto(Company company) {
        List<ScoreSnapshot> history = scoreSnapshotRepository.findByCompanyIdOrderByComputedAtDesc(company.getId(), PageRequest.of(0, 2));
        ScoreDto currentScoreDto = history.isEmpty() ? null : toScoreDto(history.get(0));
        ScoreDto previousScoreDto = history.size() > 1 ? toScoreDto(history.get(1)) : null;

        List<EventDto> events = eventRepository.findByCompanyIdOrderByEventDateDesc(company.getId()).stream()
                .map(this::toEventDto)
                .toList();

        List<NewsDto> news = newsRepository.findByCompanyIdOrderByPublishedAtDesc(company.getId()).stream()
                .map(this::toNewsDto)
                .toList();

        ResearchSummaryDto latestResearch = researchSummaryRepository.findMostRecentByCompanyId(company.getId())
                .map(this::toResearchDto)
                .orElse(null);

        boolean onWatchlist = watchlistItemRepository.existsByCompanyId(company.getId());

        return CompanyDetailDto.builder()
                .id(company.getId())
                .symbol(company.getSymbol())
                .name(company.getName())
                .exchange(company.getExchange())
                .sector(company.getSector())
                .industry(company.getIndustry())
                .marketCapCr(company.getMarketCapCr())
                .revenueGrowthPct(company.getRevenueGrowthPct())
                .profitGrowthPct(company.getProfitGrowthPct())
                .operatingMarginPct(company.getOperatingMarginPct())
                .promoterHoldingPct(company.getPromoterHoldingPct())
                .institutionalHoldingPct(company.getInstitutionalHoldingPct())
                .debtToEquity(company.getDebtToEquity())
                .roce(company.getRoce())
                .roe(company.getRoe())
                .peRatio(company.getPeRatio())
                .operatingCashFlowCr(company.getOperatingCashFlowCr())
                .currentPrice(company.getCurrentPrice())
                .week52High(company.getWeek52High())
                .week52Low(company.getWeek52Low())
                .lastRefreshedAt(company.getLastRefreshedAt())
                .currentScore(currentScoreDto)
                .previousScore(previousScoreDto)
                .recentEvents(events)
                .recentNews(news)
                .latestResearch(latestResearch)
                .onWatchlist(onWatchlist)
                .build();
    }

    private ScoreDto toScoreDto(ScoreSnapshot snap) {
        return ScoreDto.builder()
                .snapshotId(snap.getId())
                .totalScore(snap.getTotalScore())
                .computedAt(snap.getComputedAt())
                .categoryScores(java.util.Map.ofEntries(
                        java.util.Map.entry("Sector Tailwind", nz(snap.getSectorTailwindScore())),
                        java.util.Map.entry("Government Policy", nz(snap.getGovernmentPolicyScore())),
                        java.util.Map.entry("Revenue Growth", nz(snap.getRevenueGrowthScore())),
                        java.util.Map.entry("Profit Growth", nz(snap.getProfitGrowthScore())),
                        java.util.Map.entry("Margin Expansion", nz(snap.getMarginExpansionScore())),
                        java.util.Map.entry("Debt Reduction", nz(snap.getDebtReductionScore())),
                        java.util.Map.entry("ROCE", nz(snap.getRoceScore())),
                        java.util.Map.entry("ROE", nz(snap.getRoeScore())),
                        java.util.Map.entry("Order Book", nz(snap.getOrderBookScore())),
                        java.util.Map.entry("Capacity Expansion", nz(snap.getCapacityExpansionScore())),
                        java.util.Map.entry("New Products", nz(snap.getNewProductsScore())),
                        java.util.Map.entry("Exports", nz(snap.getExportsScore())),
                        java.util.Map.entry("Promoter Buying", nz(snap.getPromoterBuyingScore())),
                        java.util.Map.entry("Institutional Buying", nz(snap.getInstitutionalBuyingScore())),
                        java.util.Map.entry("Technical Breakout", nz(snap.getTechnicalBreakoutScore())),
                        java.util.Map.entry("Valuation", nz(snap.getValuationScore())),
                        java.util.Map.entry("Recent Events", nz(snap.getRecentEventsScore()))
                ))
                .reasons(toReasonDtos(snap))
                .build();
    }

    private Integer nz(Integer v) {
        return v == null ? 0 : v;
    }

    private List<ScoreReasonDto> toReasonDtos(ScoreSnapshot snap) {
        return snap.getReasons().stream().map(this::toReasonDto).toList();
    }

    private ScoreReasonDto toReasonDto(ScoreReason r) {
        return ScoreReasonDto.builder()
                .text(r.getText())
                .type(r.getType().name())
                .category(r.getCategory())
                .build();
    }

    private EventDto toEventDto(Event e) {
        return EventDto.builder()
                .id(e.getId())
                .companyId(e.getCompany().getId())
                .companySymbol(e.getCompany().getSymbol())
                .companyName(e.getCompany().getName())
                .type(e.getType().name())
                .title(e.getTitle())
                .description(e.getDescription())
                .valueCr(e.getValueCr())
                .eventDate(e.getEventDate())
                .sourceUrl(e.getSourceUrl())
                .source(e.getSource())
                .scoreImpact(e.getScoreImpact())
                .build();
    }

    private NewsDto toNewsDto(News n) {
        return NewsDto.builder()
                .id(n.getId())
                .companyId(n.getCompany().getId())
                .companySymbol(n.getCompany().getSymbol())
                .headline(n.getHeadline())
                .summary(n.getSummary())
                .url(n.getUrl())
                .sourceName(n.getSourceName())
                .publishedAt(n.getPublishedAt())
                .matchesGovernmentTheme(n.getMatchesGovernmentTheme())
                .build();
    }

    private ResearchSummaryDto toResearchDto(ResearchSummary r) {
        return ResearchSummaryDto.builder()
                .id(r.getId())
                .businessOverview(r.getBusinessOverview())
                .strengths(r.getStrengths())
                .weaknesses(r.getWeaknesses())
                .growthDrivers(r.getGrowthDrivers())
                .governmentTailwinds(r.getGovernmentTailwinds())
                .risks(r.getRisks())
                .recentDevelopments(r.getRecentDevelopments())
                .improvingAssessment(r.getImprovingAssessment())
                .futureMonitoringPoints(r.getFutureMonitoringPoints())
                .confidenceLevel(r.getConfidenceLevel())
                .generatedAt(r.getGeneratedAt())
                .build();
    }
}
