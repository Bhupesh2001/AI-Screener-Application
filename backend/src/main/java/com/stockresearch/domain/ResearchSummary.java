package com.stockresearch.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * The structured output of "Generate AI Research" for a company. Stored so
 * the company page can show the last generated summary without re-calling
 * the AI on every page load. Fields mirror exactly what Stage 8 (AI
 * Validation) and Module 6 (AI Research) require.
 *
 * NOTE: This table intentionally has NO "recommendation" field. The AI is
 * only ever asked to summarize / assess improvement, never to recommend
 * buying or selling - that constraint is enforced in the prompt (see
 * ai/PromptBuilder) and there is deliberately nowhere in the schema to store
 * a buy/sell verdict even if a model ever produced one.
 */
@Entity
@Table(name = "research_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(length = 4000)
    private String businessOverview;

    @Column(length = 2000)
    private String strengths;

    @Column(length = 2000)
    private String weaknesses;

    @Column(length = 2000)
    private String growthDrivers;

    @Column(length = 2000)
    private String governmentTailwinds;

    @Column(length = 2000)
    private String risks;

    @Column(length = 2000)
    private String recentDevelopments;

    @Column(length = 1000)
    private String improvingAssessment; // "Is this company improving?" - descriptive, not a verdict

    @Column(length = 2000)
    private String futureMonitoringPoints;

    @Column(length = 32)
    private String confidenceLevel; // e.g. "High", "Medium", "Low" - AI's confidence in its own analysis

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        this.generatedAt = LocalDateTime.now();
    }
}
