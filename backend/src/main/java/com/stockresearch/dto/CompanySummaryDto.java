package com.stockresearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight company representation used in lists (dashboard top scores,
 * search results, watchlist, sector dashboard). Carries just enough data to
 * render a row/card without a second round trip.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanySummaryDto {
    private Long id;
    private String symbol;
    private String name;
    private String sector;
    private BigDecimal marketCapCr;
    private BigDecimal currentPrice;
    private Integer currentScore;
    private Integer previousScore;
    private Integer scoreChange;
}
