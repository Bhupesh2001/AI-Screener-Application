package com.stockresearch.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit log of every AI call made (research generation, validation, etc).
 * Useful for debugging prompts and for tracking token/cost usage over time.
 */
@Entity
@Table(name = "ai_history", indexes = {
        @Index(name = "idx_ai_history_company", columnList = "company_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company; // nullable - some AI calls aren't company-specific

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(nullable = false, length = 128)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiCallPurpose purpose;

    @Column(length = 8000)
    private String promptSummary; // truncated prompt for audit, not full raw text

    @Column(length = 8000)
    private String responseText;

    private Integer promptTokens;
    private Integer completionTokens;

    private Boolean success;

    @Column(length = 2000)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum AiCallPurpose {
        RESEARCH_SUMMARY, DISCOVERY_VALIDATION, OTHER
    }
}
