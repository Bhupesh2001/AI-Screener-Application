package com.stockresearch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Master record for a listed company. This is the "Stage 1 - Initial Universe"
 * table from the discovery engine spec: it holds identity + slow-moving
 * fundamentals. Fast-moving data (scores, events, news) live in their own
 * tables and reference this one.
 */
@Entity
@Table(name = "company", indexes = {
        @Index(name = "idx_company_symbol", columnList = "symbol", unique = true),
        @Index(name = "idx_company_sector", columnList = "sector")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String symbol; // e.g. "RVNL", "BEL", "DEEPAKNTR"

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 16)
    private String exchange; // NSE / BSE

    @Column(nullable = false)
    private String sector; // e.g. Railway, Defense, Chemicals

    private String industry;

    @Column(name = "market_cap_cr")
    private BigDecimal marketCapCr; // in INR Crores

    // ---- Fundamental snapshot (refreshed periodically by background jobs) ----
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

    // ---- Technical snapshot ----
    private BigDecimal currentPrice;
    private BigDecimal week52High;
    private BigDecimal week52Low;

    @Column(name = "last_refreshed_at")
    private LocalDateTime lastRefreshedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
