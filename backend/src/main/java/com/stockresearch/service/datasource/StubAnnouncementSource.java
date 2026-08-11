package com.stockresearch.service.datasource;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Stub implementation of AnnouncementSource with canned NSE/BSE-style
 * announcements for the seeded demo companies.
 *
 * TO REPLACE: implement NseAnnouncementSource / BseAnnouncementSource
 * hitting the actual exchange announcement endpoints, mark @Primary.
 */
@Component
public class StubAnnouncementSource implements AnnouncementSource {

    private static final Map<String, List<RawAnnouncement>> CANNED = Map.of(
            "RVNL", List.of(
                    new RawAnnouncement(
                            "Award of Letter of Acceptance for Railway Electrification Project",
                            "Company has received LOA for electrification works worth approximately Rs 890 crore from Indian Railways.",
                            LocalDateTime.now().minusDays(2),
                            "https://example.com/nse/rvnl-loa-890cr",
                            "NSE",
                            new BigDecimal("890")
                    ),
                    new RawAnnouncement(
                            "Disclosure under Regulation 30 - Promoter Shareholding",
                            "President of India (promoter) shareholding remains stable; no change in the quarter.",
                            LocalDateTime.now().minusDays(20),
                            "https://example.com/nse/rvnl-promoter-holding",
                            "NSE",
                            null
                    )
            ),
            "BEL", List.of(
                    new RawAnnouncement(
                            "Order Win Intimation - Defense Electronics Systems",
                            "BEL has secured orders worth Rs 1,200 crore for defense electronics systems from Ministry of Defence.",
                            LocalDateTime.now().minusDays(3),
                            "https://example.com/bse/bel-order-1200cr",
                            "BSE",
                            new BigDecimal("1200")
                    ),
                    new RawAnnouncement(
                            "Credit Rating Reaffirmation",
                            "CRISIL has reaffirmed BEL's credit rating at AAA, citing strong order book and healthy balance sheet.",
                            LocalDateTime.now().minusDays(11),
                            "https://example.com/bse/bel-credit-rating",
                            "BSE",
                            null
                    )
            ),
            "DEEPAKNTR", List.of(
                    new RawAnnouncement(
                            "Commissioning of New Manufacturing Facility",
                            "Company has commissioned a new specialty chemicals manufacturing line at its existing facility, expected to add to revenue from next quarter.",
                            LocalDateTime.now().minusDays(5),
                            "https://example.com/nse/deepak-facility-commission",
                            "NSE",
                            null
                    )
            )
    );

    @Override
    public List<RawAnnouncement> fetchRecentAnnouncements(String companySymbol) {
        return CANNED.getOrDefault(companySymbol.toUpperCase(), List.of());
    }
}
