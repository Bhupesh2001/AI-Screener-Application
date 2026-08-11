package com.stockresearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregate payload for the Dashboard module: top scores, latest events,
 * sector heatmap, recently improved scores, watchlist, market overview.
 * One endpoint, one round trip - appropriate for a single-user dashboard.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {
    private List<CompanySummaryDto> topScoring;
    private List<EventDto> latestEvents;
    private List<SectorHeatmapEntryDto> sectorHeatmap;
    private List<CompanySummaryDto> recentlyImproved;
    private List<CompanySummaryDto> watchlist;
    private MarketOverviewDto marketOverview;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorHeatmapEntryDto {
        private String sector;
        private Integer averageScore;
        private Integer companyCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketOverviewDto {
        private Integer totalCompaniesTracked;
        private Integer totalEventsLast7Days;
        private Integer companiesAboveThreshold;
        private String topSector;
    }
}
