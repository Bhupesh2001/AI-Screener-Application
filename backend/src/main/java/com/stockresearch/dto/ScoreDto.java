package com.stockresearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Full score for a company at a point in time: total + category breakdown +
 * human-readable reasons. Every category has an explanation, per spec.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreDto {
    private Long snapshotId;
    private Integer totalScore;
    private LocalDateTime computedAt;

    /** category name -> sub-score (0-100) */
    private Map<String, Integer> categoryScores;

    private List<ScoreReasonDto> reasons;
}
