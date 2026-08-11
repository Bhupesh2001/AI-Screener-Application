package com.stockresearch.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Single-row settings table (this is a single-user app, so no need for a
 * settings-per-user model). Row with id=1 is always the active settings.
 *
 * IMPORTANT: apiKey is stored as-is in the local DB. This is acceptable for a
 * personal local tool but the field is never logged and never returned in
 * full by the API (see SettingsController / SettingsResponse DTO, which
 * masks it).
 */
@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSettings {

    @Id
    private Long id; // always 1

    // ---- AI provider config ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private AiProvider aiProvider = AiProvider.OPENAI;

    @Column(length = 1024)
    private String apiKey;

    @Column(length = 512)
    private String baseUrl;

    @Column(length = 128)
    @Builder.Default
    private String modelName = "gpt-4o-mini";

    @Builder.Default
    private Double temperature = 0.3;

    @Builder.Default
    private Integer maxTokens = 1500;

    // ---- UI ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private Theme theme = Theme.DARK;

    // ---- Scheduler ----
    @Builder.Default
    private Integer refreshIntervalHours = 6;

    // ---- Discovery engine thresholds (Stage 2 filters) ----
    @Builder.Default
    private Integer marketCapMinCr = 200;

    @Builder.Default
    private Integer marketCapMaxCr = 10000;

    @Builder.Default
    private Double minPromoterHoldingPct = 35.0;

    @Builder.Default
    private Double maxDebtToEquity = 1.0;

    @Builder.Default
    private Double minRocePct = 12.0;

    @Builder.Default
    private Integer topNResults = 50;

    @Builder.Default
    private Integer minScoreThreshold = 60;

    public enum AiProvider {
        CLAUDE, OPENAI, GEMINI, OPENROUTER, LOCAL
    }

    public enum Theme {
        DARK, LIGHT
    }
}
