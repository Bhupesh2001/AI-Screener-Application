package com.stockresearch.service;

import com.stockresearch.domain.Company;
import com.stockresearch.domain.Event;
import com.stockresearch.domain.ScoreSnapshot;
import com.stockresearch.dto.CompanySummaryDto;
import com.stockresearch.dto.DashboardDto;
import com.stockresearch.dto.EventDto;
import com.stockresearch.repository.CompanyRepository;
import com.stockresearch.repository.EventRepository;
import com.stockresearch.repository.ScoreSnapshotRepository;
import com.stockresearch.repository.WatchlistItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Backs the Dashboard module: aggregates top scores, latest events, sector heatmap, etc. */
@Service
public class DashboardService {

    private final CompanyRepository companyRepository;
    private final ScoreSnapshotRepository scoreSnapshotRepository;
    private final EventRepository eventRepository;
    private final WatchlistItemRepository watchlistItemRepository;

    public DashboardService(
            CompanyRepository companyRepository,
            ScoreSnapshotRepository scoreSnapshotRepository,
            EventRepository eventRepository,
            WatchlistItemRepository watchlistItemRepository
    ) {
        this.companyRepository = companyRepository;
        this.scoreSnapshotRepository = scoreSnapshotRepository;
        this.eventRepository = eventRepository;
        this.watchlistItemRepository = watchlistItemRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDto getDashboard(int minScoreThreshold) {
        List<ScoreSnapshot> latestPerCompany = scoreSnapshotRepository.findLatestForAllCompaniesOrderByScoreDesc();

        List<CompanySummaryDto> topScoring = latestPerCompany.stream()
                .limit(10)
                .map(this::toSummaryDto)
                .toList();

        List<EventDto> latestEvents = eventRepository.findAllByOrderByEventDateDesc(PageRequest.of(0, 15)).stream()
                .map(this::toEventDto)
                .toList();

        Map<String, List<ScoreSnapshot>> bySector = latestPerCompany.stream()
                .collect(Collectors.groupingBy(s -> s.getCompany().getSector()));

        List<DashboardDto.SectorHeatmapEntryDto> heatmap = bySector.entrySet().stream()
                .map(e -> DashboardDto.SectorHeatmapEntryDto.builder()
                        .sector(e.getKey())
                        .averageScore((int) Math.round(e.getValue().stream().mapToInt(ScoreSnapshot::getTotalScore).average().orElse(0)))
                        .companyCount(e.getValue().size())
                        .build())
                .sorted(Comparator.comparingInt(DashboardDto.SectorHeatmapEntryDto::getAverageScore).reversed())
                .toList();

        List<CompanySummaryDto> recentlyImproved = latestPerCompany.stream()
                .map(this::toSummaryDto)
                .filter(dto -> dto.getScoreChange() != null && dto.getScoreChange() > 0)
                .sorted(Comparator.comparingInt(CompanySummaryDto::getScoreChange).reversed())
                .limit(10)
                .toList();

        List<CompanySummaryDto> watchlist = watchlistItemRepository.findAll().stream()
                .map(w -> toSummaryDto(w.getCompany()))
                .toList();

        long eventsLast7Days = eventRepository.findSince(LocalDateTime.now().minusDays(7)).size();
        long aboveThreshold = latestPerCompany.stream().filter(s -> s.getTotalScore() >= minScoreThreshold).count();
        String topSector = heatmap.isEmpty() ? null : heatmap.get(0).getSector();

        DashboardDto.MarketOverviewDto marketOverview = DashboardDto.MarketOverviewDto.builder()
                .totalCompaniesTracked((int) companyRepository.count())
                .totalEventsLast7Days((int) eventsLast7Days)
                .companiesAboveThreshold((int) aboveThreshold)
                .topSector(topSector)
                .build();

        return DashboardDto.builder()
                .topScoring(topScoring)
                .latestEvents(latestEvents)
                .sectorHeatmap(heatmap)
                .recentlyImproved(recentlyImproved)
                .watchlist(watchlist)
                .marketOverview(marketOverview)
                .build();
    }

    @Transactional(readOnly = true)
    private CompanySummaryDto toSummaryDto(ScoreSnapshot latest) {
        Company company = latest.getCompany();
        List<ScoreSnapshot> history = scoreSnapshotRepository.findByCompanyIdOrderByComputedAtDesc(company.getId(), PageRequest.of(0, 2));
        Integer previous = history.size() > 1 ? history.get(1).getTotalScore() : null;

        return CompanySummaryDto.builder()
                .id(company.getId())
                .symbol(company.getSymbol())
                .name(company.getName())
                .sector(company.getSector())
                .marketCapCr(company.getMarketCapCr())
                .currentPrice(company.getCurrentPrice())
                .currentScore(latest.getTotalScore())
                .previousScore(previous)
                .scoreChange(previous != null ? latest.getTotalScore() - previous : null)
                .build();
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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
}
