// backend/src/main/java/com/stockresearch/service/datasource/NseAnnouncementSource.java
package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Real implementation of AnnouncementSource backed by NSE's unofficial
 * corporate-announcements JSON endpoint (the same one used by community
 * tools like nsepython/jugaad-data/nse-python). Requires a warm session,
 * handled by NseSessionClient - this class only knows how to build the
 * request path and parse the response shape.
 *
 * Response shape (confirmed against a live sample, field names are NSE's
 * own, not renamed for clarity):
 *   symbol, desc, dt, attchmntFile, sm_name, sm_isin, an_dt, sort_date,
 *   seq_id, smIndustry, orgid, attchmntText, bflag, old_new, csvName,
 *   exchdisstime, difference
 *
 * Two fields matter most for classification:
 *   - "desc" is often a short controlled-vocabulary category (e.g. just
 *     "Updates" or "Financial Results") - frequently too terse on its own
 *     for EventClassifier's keyword matching.
 *   - "attchmntText" is free-text natural language (e.g. "Vodafone Idea
 *     Limited has informed the Exchange regarding...") and is where most of
 *     the classifiable detail actually lives. We feed both to
 *     EventClassifier as title + description respectively, matching the
 *     RawAnnouncement contract.
 *
 * NSE does not expose a structured "order value" field in this feed - any
 * ₹ Cr figure lives inside free text or the linked PDF, which we don't
 * parse here (no reliable regex over unstructured announcement prose - a
 * wrong extracted number is worse than none). valueCr is therefore always
 * null from this source; EventClassifier/scoring rules already treat a
 * missing value gracefully.
 */
@Primary
@Component
class NseAnnouncementSource implements AnnouncementSource {

    private static final Logger log = LoggerFactory.getLogger(NseAnnouncementSource.class);

    // Matches the plan's documented endpoint shape exactly (section 2D).
    private static final String ANNOUNCEMENTS_PATH = "/api/corporate-announcements?index=equities&symbol=";

    // "sort_date" comes back as e.g. "2023-10-18 22:11:42".
    private static final DateTimeFormatter SORT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // "an_dt" comes back as e.g. "18-Oct-2023 22:11:42" - kept as a fallback
    // in case a given row is missing sort_date. Pinned to Locale.ENGLISH
    // since NSE's month abbreviations are always English regardless of the
    // JVM's default locale, and "MMM" parsing is locale-sensitive.
    private static final DateTimeFormatter AN_DT_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", java.util.Locale.ENGLISH);

    private final NseSessionClient sessionClient;
    private final ObjectMapper objectMapper;

    public NseAnnouncementSource(NseSessionClient sessionClient, ObjectMapper objectMapper) {
        this.sessionClient = sessionClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Cacheable(value = "announcements", key = "#companySymbol", unless = "#result == null or #result.isEmpty()")
    public List<RawAnnouncement> fetchRecentAnnouncements(String companySymbol) {
        Optional<String> body = sessionClient.get(ANNOUNCEMENTS_PATH + companySymbol);
        if (body.isEmpty()) {
            log.warn("No response from NSE corporate-announcements for {} (session/network issue) - " +
                    "returning empty list rather than failing the pipeline run for other companies", companySymbol);
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(body.get());
            if (!root.isArray()) {
                log.warn("Unexpected NSE announcements response shape for {} (not a JSON array)", companySymbol);
                return List.of();
            }

            List<RawAnnouncement> result = new ArrayList<>();
            for (JsonNode item : root) {
                RawAnnouncement announcement = parseAnnouncement(item);
                if (announcement != null) {
                    result.add(announcement);
                }
            }

            log.info("Fetched {} NSE announcements for {}", result.size(), companySymbol);
            return result;

        } catch (Exception e) {
            log.warn("Failed to parse NSE announcements for {}: {}", companySymbol, e.getMessage());
            return List.of();
        }
    }

    // Event.title is VARCHAR(512) and Event.description is VARCHAR(4000).
    // NSE's "desc" field is normally short (a category label) but
    // "attchmntText" - used as a title fallback when "desc" is blank, and
    // always used as the description - is free text of unpredictable
    // length. Two separate limits, truncated defensively so neither column
    // can ever reject an insert.
    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MAX_DESCRIPTION_LENGTH = 3990;

    private RawAnnouncement parseAnnouncement(JsonNode item) {
        String desc = textOrNull(item, "desc");
        String attachmentText = textOrNull(item, "attchmntText");
        String attachmentFile = textOrNull(item, "attchmntFile");

        // Title must be non-blank for EventClassifier/Event.title (which is
        // NOT NULL in the schema) - skip rows that give us nothing usable
        // rather than persisting an empty event.
        String title = truncate(desc != null && !desc.isBlank() ? desc : attachmentText, MAX_TITLE_LENGTH);
        if (title == null || title.isBlank()) {
            return null;
        }

        LocalDateTime announcementDate = parseDate(item);
        if (announcementDate == null) {
            // Without a date we can't sort it into the Event Center timeline
            // or dedupe it reliably against future pipeline runs - skip.
            log.debug("Skipping NSE announcement with unparseable date: {}", title);
            return null;
        }

        return new RawAnnouncement(
                title,
                truncate(attachmentText, MAX_DESCRIPTION_LENGTH), // richer free text for EventClassifier to keyword-match against
                announcementDate,
                attachmentFile,
                "NSE",
                (BigDecimal) null // see class-level Javadoc: not reliably extractable from this feed
        );
    }

    private LocalDateTime parseDate(JsonNode item) {
        String sortDate = textOrNull(item, "sort_date");
        if (sortDate != null) {
            try {
                return LocalDateTime.parse(sortDate, SORT_DATE_FORMAT);
            } catch (DateTimeParseException ignored) {
                // fall through to an_dt
            }
        }

        String anDt = textOrNull(item, "an_dt");
        if (anDt != null) {
            try {
                return LocalDateTime.parse(anDt, AN_DT_FORMAT);
            } catch (DateTimeParseException ignored) {
                // fall through to null
            }
        }

        return null;
    }

    private String textOrNull(JsonNode item, String field) {
        JsonNode node = item.path(field);
        if (node.isMissingNode() || node.isNull()) return null;
        String text = node.asText();
        return text.isBlank() ? null : text.trim();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "…";
    }
}
