package com.stockresearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full company page payload: overview + financials + current score breakdown
 * + recent events + recent news + latest AI research summary. This is
 * intentionally one big DTO rather than many small endpoints, since the
 * Company Page needs all of it on load and this is a single-user app where
 * request fan-out isn't a real concern.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetailDto {
    private Long id;
    private String symbol;
    private String name;
    private String exchange;
    private String sector;
    private String industry;

    // Financials
    private BigDecimal marketCapCr;
    private BigDecimal revenueGrowthPct;
    private BigDecimal profitGrowthPct;
    private BigDecimal operatingMarginPct;
    private BigDecimal promoterHoldingPct;
    private BigDecimal institutionalHoldingPct;
    private BigDecimal debtToEquity;
    private BigDecimal roce;
    private BigDecimal roe;
    private BigDecimal peRatio;
    private BigDecimal operatingCashFlowCr;

    // Technical
    private BigDecimal currentPrice;
    private BigDecimal week52High;
    private BigDecimal week52Low;

    private LocalDateTime lastRefreshedAt;

    private ScoreDto currentScore;
    private ScoreDto previousScore;

    private List<EventDto> recentEvents;
    private List<NewsDto> recentNews;
    private ResearchSummaryDto latestResearch;

    private Boolean onWatchlist;
}
