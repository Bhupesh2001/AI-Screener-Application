package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Primary
@Component
@RequiredArgsConstructor
@Slf4j
public class IndianApiAnnouncementSource implements AnnouncementSource {

    private final IndianApiClient apiClient;

    // Corporate actions date format: "2026-08-13"
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
     * This method is called by DiscoveryPipeline to avoid redundant API calls.
     */
    public List<RawAnnouncement> parseFromNode(String companySymbol, JsonNode root) {
        log.debug("Parsing announcements from cached node for: {}", companySymbol);
        return parseAnnouncements(companySymbol, root);
    }

    private List<RawAnnouncement> parseAnnouncements(String companySymbol, JsonNode root) {
        List<RawAnnouncement> result = new ArrayList<>();

        // Get corporate actions from stockCorporateActionData
        JsonNode actions = root.path("stockCorporateActionData");

        // Parse board meetings
        JsonNode boardMeetings = actions.path("boardMeetings");
        if (boardMeetings.isArray()) {
            for (JsonNode meeting : boardMeetings) {
                String purpose = meeting.path("purpose").asText();
                String dateStr = meeting.path("boardMeetDate").asText();
                LocalDateTime date = parseDate(dateStr);
                if (date != null && purpose != null && !purpose.isEmpty()) {
                    result.add(new RawAnnouncement(
                            "Board Meeting: " + purpose,
                            meeting.path("remarks").asText(),
                            date,
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
                String dateStr = div.path("recordDate").asText();
                if (dateStr == null || dateStr.isEmpty()) {
                    dateStr = div.path("xdDate").asText();
                }
                LocalDateTime date = parseDate(dateStr);
                if (date != null && remarks != null && !remarks.isEmpty()) {
                    result.add(new RawAnnouncement(
                            "Dividend: " + remarks,
                            "Dividend announcement",
                            date,
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
                String dateStr = bn.path("recordDate").asText();
                if (dateStr == null || dateStr.isEmpty()) {
                    dateStr = bn.path("xbDate").asText();
                }
                LocalDateTime date = parseDate(dateStr);
                if (date != null && remarks != null && !remarks.isEmpty()) {
                    result.add(new RawAnnouncement(
                            "Bonus Issue: " + remarks,
                            "Bonus issue announcement",
                            date,
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

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateStr, DATE_FORMAT);
        } catch (Exception e) {
            // Try alternate format
            try {
                return LocalDateTime.parse(dateStr);
            } catch (Exception ex) {
                log.debug("Could not parse date: {}", dateStr);
                return null;
            }
        }
    }
}