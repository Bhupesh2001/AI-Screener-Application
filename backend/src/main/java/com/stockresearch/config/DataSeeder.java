package com.stockresearch.config;

import com.stockresearch.domain.AppSettings;
import com.stockresearch.domain.Company;
import com.stockresearch.repository.CompanyRepository;
import com.stockresearch.service.SettingsService;
import com.stockresearch.service.discovery.DiscoveryPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds the initial company universe (Stage 1) with 3 demo companies spanning
 * different sectors, then runs the discovery pipeline once so the app has
 * real scores/events/news to show on first launch instead of an empty
 * database. Idempotent: skips seeding if companies already exist.
 *
 * TO ADD MORE COMPANIES: add entries here, and add matching canned data in
 * StubPriceDataSource / StubNewsSource / StubAnnouncementSource (or, once
 * real data source implementations exist, no seeding is needed at all -
 * Stage 1 would populate itself from a real NSE/BSE company list).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CompanyRepository companyRepository;
    private final DiscoveryPipeline discoveryPipeline;
    private final SettingsService settingsService;

    public DataSeeder(
            CompanyRepository companyRepository,
            DiscoveryPipeline discoveryPipeline,
            SettingsService settingsService
    ) {
        this.companyRepository = companyRepository;
        this.discoveryPipeline = discoveryPipeline;
        this.settingsService = settingsService;
    }

    @Override
    public void run(String... args) {
        if (companyRepository.count() > 0) {
            log.info("Companies already seeded ({} found), skipping seed step.", companyRepository.count());
            return;
        }

//        log.info("Seeding demo company universe...");
//
//        companyRepository.save(Company.builder()
//                .symbol("RVNL")
//                .name("Rail Vikas Nigam Limited")
//                .exchange("NSE")
//                .sector("Railway")
//                .industry("Railway Construction & Infrastructure")
//                .build());
//
//        companyRepository.save(Company.builder()
//                .symbol("BEL")
//                .name("Bharat Electronics Limited")
//                .exchange("NSE")
//                .sector("Defense")
//                .industry("Defense Electronics")
//                .build());
//
//        companyRepository.save(Company.builder()
//                .symbol("DEEPAKNTR")
//                .name("Deepak Nitrite Limited")
//                .exchange("NSE")
//                .sector("Chemicals")
//                .industry("Specialty Chemicals")
//                .build());
//
//        log.info("Seeded 3 demo companies. Running initial discovery pipeline...");

        AppSettings settings = settingsService.getSettings();
        // Run twice so there's a "previous" score snapshot to diff against
        // (Score Change / "Why did the score change?" feature needs at least
        // two snapshots to be meaningful in the demo).
        discoveryPipeline.runForAllCompanies(settings);
        discoveryPipeline.runForAllCompanies(settings);

        log.info("Initial discovery run complete. App is ready.");
    }
}
