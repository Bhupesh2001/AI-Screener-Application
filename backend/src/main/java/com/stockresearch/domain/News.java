package com.stockresearch.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A news article related to a company, pulled from RSS feeds / news APIs.
 * Distinct from Event: News is raw unstructured text; Event is a structured,
 * classified fact extracted (by rules or AI) from news/announcements.
 */
@Entity
@Table(name = "news", indexes = {
        @Index(name = "idx_news_company", columnList = "company_id"),
        @Index(name = "idx_news_time", columnList = "publishedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 1024)
    private String headline;

    @Column(length = 4000)
    private String summary;

    @Column(length = 1024)
    private String url;

    @Column(length = 128)
    private String sourceName; // e.g. "Moneycontrol", "Economic Times"

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    /** Whether this article matched a government-tailwind keyword (Stage 5). */
    private Boolean matchesGovernmentTheme;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fetchedAt;

    @PrePersist
    protected void onCreate() {
        this.fetchedAt = LocalDateTime.now();
    }
}
