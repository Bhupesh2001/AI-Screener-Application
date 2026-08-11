package com.stockresearch.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A detected corporate event: order win, capacity expansion, promoter buying,
 * block deal, dividend, quarterly result, etc. This single table backs both
 * the Event Center module and Stage 4 (Event Detection) of the discovery
 * engine. EventType is intentionally a flat enum rather than a sub-table
 * hierarchy - keeps queries simple for a single-user tool.
 */
@Entity
@Table(name = "event", indexes = {
        @Index(name = "idx_event_company", columnList = "company_id"),
        @Index(name = "idx_event_time", columnList = "eventDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventType type;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(length = 4000)
    private String description;

    /** Optional: monetary value associated with the event, e.g. order size in Cr. */
    private java.math.BigDecimal valueCr;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Column(length = 1024)
    private String sourceUrl;

    @Column(length = 64)
    private String source; // e.g. "NSE", "BSE", "Google News"

    /** How much this event contributed to the Event Score - set by scoring engine. */
    private Integer scoreImpact;

    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    protected void onCreate() {
        this.detectedAt = LocalDateTime.now();
    }

    public enum EventType {
        LARGE_ORDER,
        GOVERNMENT_CONTRACT,
        EXPORT_ORDER,
        CAPACITY_EXPANSION,
        NEW_FACTORY,
        PLANT_COMMISSIONING,
        ACQUISITION,
        NEW_PRODUCT,
        JOINT_VENTURE,
        STRATEGIC_PARTNERSHIP,
        PROMOTER_BUYING,
        PROMOTER_SELLING,
        INSTITUTIONAL_BUYING,
        CREDIT_RATING_UPGRADE,
        CREDIT_RATING_DOWNGRADE,
        PATENT,
        GOVERNMENT_APPROVAL,
        PLI_PARTICIPATION,
        MANAGEMENT_GUIDANCE_UP,
        EARNINGS_SURPRISE,
        BLOCK_DEAL,
        BULK_DEAL,
        DIVIDEND,
        BONUS,
        SPLIT,
        QUARTERLY_RESULTS,
        OTHER
    }
}
