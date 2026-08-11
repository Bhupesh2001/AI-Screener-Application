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
public class NewsDto {
    private Long id;
    private Long companyId;
    private String companySymbol;
    private String headline;
    private String summary;
    private String url;
    private String sourceName;
    private LocalDateTime publishedAt;
    private Boolean matchesGovernmentTheme;
}
