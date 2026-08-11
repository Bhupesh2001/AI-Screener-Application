package com.stockresearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreReasonDto {
    private String text;
    private String type; // POSITIVE, NEGATIVE, NEUTRAL
    private String category;
}
