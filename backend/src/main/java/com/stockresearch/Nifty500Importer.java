package com.stockresearch;

import com.stockresearch.domain.Company;
import com.stockresearch.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component
public class Nifty500Importer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Nifty500Importer.class);
    private static final String NIFTY500_CSV_URL = "https://www.niftyindices.com/IndexConstituent/ind_nifty500list.csv";

    private final CompanyRepository companyRepository;

    public Nifty500Importer(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only run if explicitly enabled via environment variable or argument
        // This prevents accidental re-import on every startup
        boolean shouldRun = System.getenv("IMPORT_NIFTY500") != null 
                || System.getProperty("import.nifty500") != null;
        
        if (!shouldRun) {
            log.info("Nifty500 importer skipped. Set IMPORT_NIFTY500=true to run.");
            return;
        }

        // Check if already imported
        long existingCount = companyRepository.count();
        if (existingCount > 3) { // More than just the 3 seeded demo companies
            log.info("Nifty500 already imported ({} companies found). Skipping.", existingCount);
            return;
        }

        log.info("Starting Nifty 500 import from: {}", NIFTY500_CSV_URL);
        int imported = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new URL(NIFTY500_CSV_URL).openStream(), StandardCharsets.UTF_8))) {
            
            String line;
            boolean isHeader = true;
            
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // Skip header row
                }
                
                String[] columns = line.split(",");
                if (columns.length < 4) {
                    log.warn("Skipping malformed line: {}", line);
                    skipped++;
                    continue;
                }
                
                // Columns: Company Name, Industry, Symbol, Series, ISIN Code
                String symbol = columns[2].trim();
                String name = columns[0].trim();
                String industry = columns[1].trim();
                // We'll derive sector from industry (or keep as is)
                String sector = industry; // Or map industry to broader sector if needed
                
                if (symbol.isEmpty()) {
                    skipped++;
                    continue;
                }
                
                // Check if company already exists
                if (companyRepository.findBySymbolIgnoreCase(symbol).isPresent()) {
                    log.debug("Company {} already exists, skipping", symbol);
                    skipped++;
                    continue;
                }
                
                Company company = Company.builder()
                        .symbol(symbol)
                        .name(name)
                        .exchange("NSE")
                        .sector(sector)
                        .industry(industry)
                        .build();
                
                companyRepository.save(company);
                imported++;
                
                if (imported % 50 == 0) {
                    log.info("Imported {} companies so far...", imported);
                }
            }
        } catch (Exception e) {
            log.error("Failed to import Nifty 500: {}", e.getMessage(), e);
        }

        log.info("Nifty 500 import complete: {} imported, {} skipped, {} total in DB", 
                imported, skipped, companyRepository.count());
    }
}