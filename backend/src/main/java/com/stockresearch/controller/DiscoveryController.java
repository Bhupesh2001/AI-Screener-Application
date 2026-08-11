package com.stockresearch.controller;

import com.stockresearch.domain.AppSettings;
import com.stockresearch.service.SettingsService;
import com.stockresearch.service.discovery.DiscoveryPipeline;
import com.stockresearch.service.discovery.DiscoveryResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Manual trigger for the discovery pipeline (in addition to the scheduled
 * background job - see scheduler package). Lets the user hit "Refresh Now"
 * from the UI rather than waiting for the next scheduled run.
 */
@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final DiscoveryPipeline discoveryPipeline;
    private final SettingsService settingsService;

    public DiscoveryController(DiscoveryPipeline discoveryPipeline, SettingsService settingsService) {
        this.discoveryPipeline = discoveryPipeline;
        this.settingsService = settingsService;
    }

    @PostMapping("/refresh")
    public Map<String, Object> refreshAll() {
        AppSettings settings = settingsService.getSettings();
        List<DiscoveryResult> results = discoveryPipeline.runForAllCompanies(settings);

        long included = results.stream().filter(DiscoveryResult::passedFundamentalScreen).count();
        long excluded = results.size() - included;

        return Map.of(
                "totalProcessed", results.size(),
                "included", included,
                "excluded", excluded
        );
    }
}
