package com.stockresearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Powers the "Why did the score change?" panel:
 *   Previous Score: 74
 *   Current Score: 83
 *   Reasons: [+ New order, + Revenue growth accelerated, ...]
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreChangeDto {
    private Integer previousScore;
    private Integer currentScore;
    private Integer delta;
    private List<ScoreReasonDto> reasons;
}
