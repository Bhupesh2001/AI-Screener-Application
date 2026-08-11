package com.stockresearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorSummaryDto {
    private String sector;
    private BigDecimal averageRevenueGrowthPct;
    private BigDecimal averageProfitGrowthPct;
    private Integer averageScore;
    private Integer recentNewsCount;
    private Integer highestScore;
    private List<CompanySummaryDto> bestPerforming; // top 5 by score
}
