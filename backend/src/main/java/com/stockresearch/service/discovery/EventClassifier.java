package com.stockresearch.service.discovery;

import com.stockresearch.domain.Company;
import com.stockresearch.domain.Event;
import com.stockresearch.service.datasource.AnnouncementSource;
import org.springframework.stereotype.Component;

/**
 * Classifies a raw announcement (free-text title/description from
 * NSE/BSE/etc) into a typed Event.EventType using simple keyword matching.
 *
 * This is intentionally rule-based rather than AI-based: classification
 * needs to run on every refresh cycle for every company cheaply and
 * deterministically, whereas the more expensive/nuanced AI analysis is
 * reserved for Stage 8 (on-demand "Generate AI Research"). If keyword
 * matching proves too coarse later, this is the single place to swap in
 * an AI-based classifier without touching the rest of the pipeline.
 *
 * NOTE on NSE coverage: NSE's corporate-announcements feed (see
 * NseAnnouncementSource) often tags rows with short controlled-vocabulary
 * category labels (e.g. "Award of Order / Receipt of Order") rather than
 * natural-language headlines. Only "award of order"/"receipt of order" has
 * been added here as a verified-against-a-live-sample phrase; NSE's full
 * official subject taxonomy is longer than what's covered below. If real
 * announcements are consistently landing in OTHER (check Event Center),
 * that's the signal to add more of NSE's own category phrases here.
 */
@Component
public class EventClassifier {

    public Event classify(Company company, AnnouncementSource.RawAnnouncement raw) {
        // Regulatory category names (from BSE/NSE feeds) often use underscores
        // instead of spaces, e.g. "Award_of_Order_Receipt_of_Order" - normalize
        // so keyword phrases like "award of order" still match regardless of
        // which separator the source used. Verified necessary against a real
        // IndianAPI /recent_announcements sample, not a hypothetical case.
        String haystack = (raw.title() + " " + (raw.description() == null ? "" : raw.description()))
                .toLowerCase()
                .replace('_', ' ');

        Event.EventType type = determineType(haystack);

        return Event.builder()
                .company(company)
                .type(type)
                .title(raw.title())
                .description(raw.description())
                .valueCr(raw.valueCr())
                .eventDate(raw.announcementDate())
                .sourceUrl(raw.sourceUrl())
                .source(raw.exchange())
                .build();
    }

    private Event.EventType determineType(String haystack) {
        if (containsAny(haystack, "loa", "letter of acceptance", "order win", "order worth", "secured order",
                "bags order", "award of order", "receipt of order")) {
            if (containsAny(haystack, "government", "ministry", "railway", "defence", "defense")) {
                return Event.EventType.GOVERNMENT_CONTRACT;
            }
            if (containsAny(haystack, "export")) {
                return Event.EventType.EXPORT_ORDER;
            }
            return Event.EventType.LARGE_ORDER;
        }
        if (containsAny(haystack, "capacity expansion", "expand capacity")) return Event.EventType.CAPACITY_EXPANSION;
        if (containsAny(haystack, "new factory", "greenfield")) return Event.EventType.NEW_FACTORY;
        if (containsAny(haystack, "commission", "commissioning")) return Event.EventType.PLANT_COMMISSIONING;
        if (containsAny(haystack, "acquisition", "acquire", "acquired")) return Event.EventType.ACQUISITION;
        if (containsAny(haystack, "new product", "product launch")) return Event.EventType.NEW_PRODUCT;
        if (containsAny(haystack, "joint venture", " jv ")) return Event.EventType.JOINT_VENTURE;
        if (containsAny(haystack, "strategic partnership", "partnership")) return Event.EventType.STRATEGIC_PARTNERSHIP;
        if (containsAny(haystack, "promoter") && containsAny(haystack, "buy", "acquisition of shares", "increase")) {
            return Event.EventType.PROMOTER_BUYING;
        }
        if (containsAny(haystack, "promoter") && containsAny(haystack, "sell", "sale of shares", "decrease")) {
            return Event.EventType.PROMOTER_SELLING;
        }
        if (containsAny(haystack, "bulk deal")) return Event.EventType.BULK_DEAL;
        if (containsAny(haystack, "block deal")) return Event.EventType.BLOCK_DEAL;
        if (containsAny(haystack, "credit rating") && containsAny(haystack, "upgrade", "reaffirm")) {
            return Event.EventType.CREDIT_RATING_UPGRADE;
        }
        if (containsAny(haystack, "credit rating") && containsAny(haystack, "downgrade")) {
            return Event.EventType.CREDIT_RATING_DOWNGRADE;
        }
        if (containsAny(haystack, "patent")) return Event.EventType.PATENT;
        if (containsAny(haystack, "government approval", "regulatory approval")) return Event.EventType.GOVERNMENT_APPROVAL;
        if (containsAny(haystack, "pli", "production linked incentive")) return Event.EventType.PLI_PARTICIPATION;
        if (containsAny(haystack, "guidance")) return Event.EventType.MANAGEMENT_GUIDANCE_UP;
        if (containsAny(haystack, "earnings surprise", "beat estimates")) return Event.EventType.EARNINGS_SURPRISE;
        if (containsAny(haystack, "dividend")) return Event.EventType.DIVIDEND;
        if (containsAny(haystack, "bonus")) return Event.EventType.BONUS;
        if (containsAny(haystack, "split")) return Event.EventType.SPLIT;
        if (containsAny(haystack, "quarterly result", "q1 result", "q2 result", "q3 result", "q4 result", "financial result")) {
            return Event.EventType.QUARTERLY_RESULTS;
        }

        return Event.EventType.OTHER;
    }

    private boolean containsAny(String haystack, String... keywords) {
        for (String kw : keywords) {
            if (haystack.contains(kw)) return true;
        }
        return false;
    }
}
