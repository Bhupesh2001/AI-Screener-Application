package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Primary
@Component
@RequiredArgsConstructor
@Slf4j
public class IndianApiAnnouncementSource implements AnnouncementSource {

    private final IndianApiClient apiClient;

    @Override
    public List<RawAnnouncement> fetchRecentAnnouncements(String companySymbol) {
        log.debug("Fetching announcements for symbol: {}", companySymbol);
        try {
            JsonNode root = apiClient.getStockData(companySymbol);
            return parseAnnouncements(companySymbol, root);
        } catch (Exception e) {
            log.error("Failed to fetch announcements for {}: {}", companySymbol, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Parse announcements from an already-fetched JsonNode.
     */
    public List<RawAnnouncement> parseFromNode(String companySymbol, JsonNode root) {
        log.debug("Parsing announcements from cached node for: {}", companySymbol);
        return parseAnnouncements(companySymbol, root);
    }

    private List<RawAnnouncement> parseAnnouncements(String companySymbol, JsonNode root) {
        List<RawAnnouncement> result = new ArrayList<>();
        JsonNode actions = root.path("stockCorporateActionData");

        // Parse board meetings
        JsonNode boardMeetings = actions.path("boardMeetings");
        if (boardMeetings.isArray()) {
            for (JsonNode meeting : boardMeetings) {
                String purpose = meeting.path("purpose").asText();
                String eventDateStr = meeting.path("boardMeetDate").asText();
                LocalDateTime eventDate = parseDate(eventDateStr);
                String announcementDateStr = meeting.path("dateOfAnnouncement").asText();
                LocalDateTime announcementDate = parseDate(announcementDateStr);
                if (eventDate != null && purpose != null && !purpose.isEmpty()) {
                    result.add(new RawAnnouncement(
                            "Board Meeting: " + purpose,
                            meeting.path("remarks").asText(),
                            eventDate,
                            announcementDate,
                            null,
                            "NSE",
                            null
                    ));
                }
            }
        }

        // Parse dividends
        JsonNode dividends = actions.path("dividend");
        if (dividends.isArray()) {
            for (JsonNode div : dividends) {
                String remarks = div.path("remarks").asText();
                String eventDateStr = div.path("recordDate").asText();
                if (eventDateStr == null || eventDateStr.isEmpty()) {
                    eventDateStr = div.path("xdDate").asText();
                }
                LocalDateTime eventDate = parseDate(eventDateStr);
                String announcementDateStr = div.path("dateOfAnnouncement").asText();
                LocalDateTime announcementDate = parseDate(announcementDateStr);
                if (eventDate != null && remarks != null && !remarks.isEmpty()) {
                    result.add(new RawAnnouncement(
                            "Dividend: " + remarks,
                            "Dividend announcement",
                            eventDate,
                            announcementDate,
                            null,
                            "NSE",
                            null
                    ));
                }
            }
        }

        // Parse bonus issues
        JsonNode bonus = actions.path("bonus");
        if (bonus.isArray()) {
            for (JsonNode bn : bonus) {
                String remarks = bn.path("remarks").asText();
                String eventDateStr = bn.path("recordDate").asText();
                if (eventDateStr == null || eventDateStr.isEmpty()) {
                    eventDateStr = bn.path("xbDate").asText();
                }
                LocalDateTime eventDate = parseDate(eventDateStr);
                String announcementDateStr = bn.path("dateOfAnnouncement").asText();
                LocalDateTime announcementDate = parseDate(announcementDateStr);
                if (eventDate != null && remarks != null && !remarks.isEmpty()) {
                    result.add(new RawAnnouncement(
                            "Bonus Issue: " + remarks,
                            "Bonus issue announcement",
                            eventDate,
                            announcementDate,
                            null,
                            "NSE",
                            null
                    ));
                }
            }
        }

        // Parse rights issues (if any)
        JsonNode rights = actions.path("rights");
        if (rights.isArray()) {
            for (JsonNode r : rights) {
                String remarks = r.path("remarks").asText();
                String eventDateStr = r.path("recordDate").asText();
                LocalDateTime eventDate = parseDate(eventDateStr);
                String announcementDateStr = r.path("dateOfAnnouncement").asText();
                LocalDateTime announcementDate = parseDate(announcementDateStr);
                if (eventDate != null && remarks != null && !remarks.isEmpty()) {
                    result.add(new RawAnnouncement(
                            "Rights Issue: " + remarks,
                            "Rights issue announcement",
                            eventDate,
                            announcementDate,
                            null,
                            "NSE",
                            null
                    ));
                }
            }
        }

        log.info("Parsed {} announcements for {}", result.size(), companySymbol);
        return result;
    }

    /**
     * Robust date parser for IndianAPI corporate action dates.
     * Handles both "yyyy-MM-dd" and "yyyy-MM-ddTHH:mm:ss" formats.
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        // Try ISO date-time format (e.g., "2026-08-13T00:00:00")
        try {
            return LocalDateTime.parse(dateStr);
        } catch (DateTimeParseException e) {
            // Try plain date (e.g., "2026-08-13")
            try {
                LocalDate date = LocalDate.parse(dateStr);
                return date.atStartOfDay();
            } catch (DateTimeParseException e2) {
                log.warn("Could not parse date string: '{}' for announcements", dateStr);
                return null;
            }
        }
    }
}