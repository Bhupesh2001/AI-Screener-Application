package com.stockresearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Powers the "Why is this stock interesting?" card:
 *   ★★★★☆ Interesting
 *   ✔ Revenue growth accelerated for 3 quarters
 *   ✔ New Cr order
 *   ⚠ Valuation slightly above historical average
 *
 * starRating is derived from totalScore (see ScoringEngine.toStarRating).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhyInterestingDto {
    private Long companyId;
    private String symbol;
    private Integer totalScore;
    private Integer starRating; // 1-5
    private String label; // e.g. "Interesting", "Highly Interesting", "Worth Watching"
    private List<ScoreReasonDto> positives;
    private List<ScoreReasonDto> cautions;
}
