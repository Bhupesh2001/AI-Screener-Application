package com.stockresearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Used for both reading and writing settings.
 *
 * On READ: apiKey is masked (e.g. "sk-...ab12") so it's never fully exposed
 * back to the browser/logs after being saved once.
 * On WRITE: if apiKey is null/blank, the existing stored key is preserved
 * (so the frontend doesn't have to resend it every time settings are saved).
 * If the user wants to clear it, they must send an explicit clear flag.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsDto {
    private String aiProvider; // CLAUDE, OPENAI, GEMINI, OPENROUTER, LOCAL
    private String apiKey; // masked on output
    private String baseUrl;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;

    private String theme; // DARK, LIGHT

    private Integer refreshIntervalHours;

    private Integer marketCapMinCr;
    private Integer marketCapMaxCr;
    private Double minPromoterHoldingPct;
    private Double maxDebtToEquity;
    private Double minRocePct;
    private Integer topNResults;
    private Integer minScoreThreshold;

    private boolean clearApiKey; // explicit flag to clear the stored key
}
