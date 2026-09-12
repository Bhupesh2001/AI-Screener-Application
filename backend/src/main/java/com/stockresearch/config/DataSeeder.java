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
 * Originally seeded a small demo company universe (3 companies) on first
 * launch. Company seeding now happens via a separate one-time script
 * (services/fundamentals-service/import_nifty500.py), which populates the
 * full universe from NSE's official Nifty 500 constituent list directly -
 * this class's original seeding code is commented out below and kept only
 * for reference, not as the active mechanism. StubPriceDataSource /
 * StubNewsSource / StubAnnouncementSource (referenced in the original
 * seeding code's comments below) no longer exist - real data sources
 * replaced them entirely.
 *
 * Current behavior: if companies already exist (the normal case once
 * import_nifty500.py has been run), this logs and does nothing further.
 * If the company table is genuinely empty, execution falls through to
 * running the discovery pipeline twice regardless - but since the seeding
 * block below is commented out and nothing else populates companies at
 * that point, that run has nothing to process. KNOWN GAP: on a truly fresh
 * environment, import_nifty500.py needs to be run manually before this
 * class (or the app generally) does anything useful - there is currently
 * no automatic bootstrap path.
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
