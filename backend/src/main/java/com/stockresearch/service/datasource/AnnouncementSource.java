package com.stockresearch.service.datasource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Abstraction over corporate announcement providers (NSE/BSE corporate
 * announcement feeds, investor presentation PDFs, annual/quarterly reports).
 * A real implementation would scrape/call the NSE and BSE announcement APIs;
 * the stub implementation returns canned data for the seeded demo companies.
 */
public interface AnnouncementSource {

    List<RawAnnouncement> fetchRecentAnnouncements(String companySymbol);

    record RawAnnouncement(
            String title,
            String description,
            LocalDateTime eventDate,           // the future date (record/xd/board meet)
            LocalDateTime announcementDate,   // NEW: when the company announced it
            String sourceUrl,
            String exchange,
            BigDecimal valueCr
    ) {}
}
