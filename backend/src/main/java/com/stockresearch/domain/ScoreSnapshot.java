package com.stockresearch.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single point-in-time score for a company. We keep every snapshot (not just
 * "current" vs "previous") so score history/trend charts are possible later
 * without a schema change. The most recent snapshot per company is the
 * "current" score; the one before it is the "previous" score used for delta
 * calculations ("Why did the score change?").
 */
@Entity
@Table(name = "score_snapshot", indexes = {
        @Index(name = "idx_score_company_time", columnList = "company_id, computedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private Integer totalScore; // 0-100

    // ---- Category sub-scores (each 0-100, weighted by ScoringEngine) ----
    private Integer sectorTailwindScore;
    private Integer governmentPolicyScore;
    private Integer revenueGrowthScore;
    private Integer profitGrowthScore;
    private Integer marginExpansionScore;
    private Integer debtReductionScore;
    private Integer roceScore;
    private Integer roeScore;
    private Integer orderBookScore;
    private Integer capacityExpansionScore;
    private Integer newProductsScore;
    private Integer exportsScore;
    private Integer promoterBuyingScore;
    private Integer institutionalBuyingScore;
    private Integer technicalBreakoutScore;
    private Integer valuationScore;
    private Integer recentEventsScore;

    @OneToMany(mappedBy = "scoreSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ScoreReason> reasons = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    protected void onCreate() {
        if (this.computedAt == null) {
            this.computedAt = LocalDateTime.now();
        }
    }
}
