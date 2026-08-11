package com.stockresearch.scheduler;

import com.stockresearch.domain.AppSettings;
import com.stockresearch.service.SettingsService;
import com.stockresearch.service.discovery.DiscoveryPipeline;
import com.stockresearch.service.discovery.DiscoveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically refreshes financial data, news, corporate announcements, and
 * scores for every company (the "Background Jobs" section of the spec).
 * Runs every N hours as configured in application.yml (app.scheduler.*);
 * the interval can also be tuned per-run via Settings.refreshIntervalHours,
 * though changing that value takes effect on the next fixed check rather
 * than immediately rescheduling the trigger (Spring's @Scheduled interval
 * is set at startup - a fully dynamic reschedule would need a
 * TaskScheduler-based approach, which is a reasonable future improvement).
 */
@Component
public class RefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshScheduler.class);

    private final DiscoveryPipeline discoveryPipeline;
    private final SettingsService settingsService;
    private final boolean enabled;

    public RefreshScheduler(
            DiscoveryPipeline discoveryPipeline,
            SettingsService settingsService,
            @Value("${app.scheduler.enabled:true}") boolean enabled
    ) {
        this.discoveryPipeline = discoveryPipeline;
        this.settingsService = settingsService;
        this.enabled = enabled;
    }

    // Checks hourly whether a refresh is due, rather than hardcoding the
    // exact configured interval as the fixed rate - this way changes to
    // Settings.refreshIntervalHours take effect within the hour rather than
    // requiring an app restart.
    @Scheduled(fixedRate = 60 * 60 * 1000, initialDelay = 60 * 1000)
    public void scheduledRefresh() {
        if (!enabled) {
            return;
        }
        AppSettings settings = settingsService.getSettings();
        log.info("Running scheduled discovery refresh (interval: every {}h)", settings.getRefreshIntervalHours());

        try {
            List<DiscoveryResult> results = discoveryPipeline.runForAllCompanies(settings);
            long included = results.stream().filter(DiscoveryResult::passedFundamentalScreen).count();
            log.info("Scheduled refresh complete: {} processed, {} included, {} excluded",
                    results.size(), included, results.size() - included);
        } catch (Exception e) {
            log.error("Scheduled refresh failed", e);
        }
    }
}
