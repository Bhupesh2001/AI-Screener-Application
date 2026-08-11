package com.stockresearch.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * A single explanatory line attached to a ScoreSnapshot, e.g.
 * "+ New ₹850 crore order" or "⚠ Valuation slightly above historical average".
 *
 * This is what powers the "Why is this stock interesting?" card and the
 * "Why did the score change?" feature - both explicitly required so users
 * never see a bare number.
 */
@Entity
@Table(name = "score_reason")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "score_snapshot_id", nullable = false)
    private ScoreSnapshot scoreSnapshot;

    @Column(nullable = false, length = 512)
    private String text; // e.g. "Revenue growth accelerated for 3 quarters"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReasonType type; // POSITIVE, NEGATIVE, NEUTRAL

    @Column(nullable = false, length = 64)
    private String category; // e.g. "Revenue Growth", "Order Book"

    public enum ReasonType {
        POSITIVE, NEGATIVE, NEUTRAL
    }
}
