package com.stockresearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchSummaryDto {
    private Long id;
    private String businessOverview;
    private String strengths;
    private String weaknesses;
    private String growthDrivers;
    private String governmentTailwinds;
    private String risks;
    private String recentDevelopments;
    private String improvingAssessment;
    private String futureMonitoringPoints;
    private String confidenceLevel;
    private LocalDateTime generatedAt;
}
