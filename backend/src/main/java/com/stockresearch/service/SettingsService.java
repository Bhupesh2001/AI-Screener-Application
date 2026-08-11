package com.stockresearch.service;

import com.stockresearch.domain.AppSettings;
import com.stockresearch.dto.SettingsDto;
import com.stockresearch.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private static final Long SETTINGS_ID = 1L;

    private final AppSettingsRepository repository;

    public SettingsService(AppSettingsRepository repository) {
        this.repository = repository;
    }

    /** Returns the active settings row, creating a default one on first access. */
    public AppSettings getSettings() {
        return repository.findById(SETTINGS_ID).orElseGet(() -> {
            AppSettings defaults = AppSettings.builder().id(SETTINGS_ID).build();
            return repository.save(defaults);
        });
    }

    public SettingsDto getSettingsDto() {
        return toDto(getSettings());
    }

    public SettingsDto updateSettings(SettingsDto dto) {
        AppSettings settings = getSettings();

        if (dto.getAiProvider() != null) {
            settings.setAiProvider(AppSettings.AiProvider.valueOf(dto.getAiProvider().toUpperCase()));
        }
        if (dto.isClearApiKey()) {
            settings.setApiKey(null);
        } else if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) {
            settings.setApiKey(dto.getApiKey());
        }
        if (dto.getBaseUrl() != null) {
            settings.setBaseUrl(dto.getBaseUrl());
        }
        if (dto.getModelName() != null && !dto.getModelName().isBlank()) {
            settings.setModelName(dto.getModelName());
        }
        if (dto.getTemperature() != null) {
            settings.setTemperature(dto.getTemperature());
        }
        if (dto.getMaxTokens() != null) {
            settings.setMaxTokens(dto.getMaxTokens());
        }
        if (dto.getTheme() != null) {
            settings.setTheme(AppSettings.Theme.valueOf(dto.getTheme().toUpperCase()));
        }
        if (dto.getRefreshIntervalHours() != null) {
            settings.setRefreshIntervalHours(dto.getRefreshIntervalHours());
        }
        if (dto.getMarketCapMinCr() != null) {
            settings.setMarketCapMinCr(dto.getMarketCapMinCr());
        }
        if (dto.getMarketCapMaxCr() != null) {
            settings.setMarketCapMaxCr(dto.getMarketCapMaxCr());
        }
        if (dto.getMinPromoterHoldingPct() != null) {
            settings.setMinPromoterHoldingPct(dto.getMinPromoterHoldingPct());
        }
        if (dto.getMaxDebtToEquity() != null) {
            settings.setMaxDebtToEquity(dto.getMaxDebtToEquity());
        }
        if (dto.getMinRocePct() != null) {
            settings.setMinRocePct(dto.getMinRocePct());
        }
        if (dto.getTopNResults() != null) {
            settings.setTopNResults(dto.getTopNResults());
        }
        if (dto.getMinScoreThreshold() != null) {
            settings.setMinScoreThreshold(dto.getMinScoreThreshold());
        }

        AppSettings saved = repository.save(settings);
        return toDto(saved);
    }

    private SettingsDto toDto(AppSettings s) {
        return SettingsDto.builder()
                .aiProvider(s.getAiProvider().name())
                .apiKey(maskApiKey(s.getApiKey()))
                .baseUrl(s.getBaseUrl())
                .modelName(s.getModelName())
                .temperature(s.getTemperature())
                .maxTokens(s.getMaxTokens())
                .theme(s.getTheme().name())
                .refreshIntervalHours(s.getRefreshIntervalHours())
                .marketCapMinCr(s.getMarketCapMinCr())
                .marketCapMaxCr(s.getMarketCapMaxCr())
                .minPromoterHoldingPct(s.getMinPromoterHoldingPct())
                .maxDebtToEquity(s.getMaxDebtToEquity())
                .minRocePct(s.getMinRocePct())
                .topNResults(s.getTopNResults())
                .minScoreThreshold(s.getMinScoreThreshold())
                .build();
    }

    private String maskApiKey(String key) {
        if (key == null || key.isBlank()) return null;
        if (key.length() <= 8) return "****";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
