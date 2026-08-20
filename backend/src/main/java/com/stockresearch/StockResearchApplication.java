package com.stockresearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the personal Stock Research application.
 *
 * This is a single-user tool: no auth, no multi-tenancy, no enterprise
 * scaffolding. Keep it that way unless requirements genuinely change.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.stockresearch")
@EnableCaching
public class StockResearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockResearchApplication.class, args);
    }
}
