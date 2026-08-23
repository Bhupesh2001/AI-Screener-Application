package com.stockresearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private Long id;
    private Long companyId;
    private String companySymbol;
    private String companyName;
    private String type;
    private String title;
    private String description;
    private BigDecimal valueCr;
    private LocalDateTime eventDate;
    private String sourceUrl;
    private String source;
    private Integer scoreImpact;
    private LocalDateTime announcementDate;
}
