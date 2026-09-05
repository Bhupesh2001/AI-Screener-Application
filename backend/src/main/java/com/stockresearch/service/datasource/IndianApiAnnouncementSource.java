package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Primary
@Component
@Slf4j
public class IndianApiAnnouncementSource implements AnnouncementSource {

    private final IndianApiClient apiClient;

    // Off by default: /recent_announcements is a separate network call from
    // /stock (can't share the shared-fetch optimization), so this is one
    // extra API hit per company per refresh cycle on top of everything
    // else. Kept behind an explicit opt-in so it never silently burns
    // through a limited rate-limit budget - flip indianapi.recent-
    // announcements-enabled to true in application.yml once there's
    // confirmed headroom for it.
    private final boolean recentAnnouncementsEnabled;

    public IndianApiAnnouncementSource(
            IndianApiClient apiClient,
            @Value("${indianapi.recent-announcements-enabled:false}") boolean recentAnnouncementsEnabled
    ) {
        this.apiClient = apiClient;
        this.recentAnnouncementsEnabled = recentAnnouncementsEnabled;
        if (!recentAnnouncementsEnabled) {
            log.info("IndianAPI /recent_announcements is disabled (indianapi.recent-announcements-enabled=false) - " +
                    "Order Book/Capacity Expansion/New Products/Exports scoring will only see corporate actions " +
                    "(board meetings, dividends, bonus issues) until this is turned on");
        }
    }

    // Corporate actions date format: "2026-08-13"
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // /recent_announcements titles embed a "DD Mon - " fragment marking where
    // the official regulatory category ends and the free-text narrative
    // begins, e.g. "...Award_of_Order_Receipt_of_Order 26 Aug - BEL secured
    // additional orders worth Rs.730 crore..." - verified against a real
    // response sample, not assumed. This single pattern serves two purposes:
    // splitting title from description, AND extracting the announcement date
    // (the "date" field IndianAPI returns is not an actual date - it's the
    // same trailing narrative sentence duplicated, with no year at all in
    // the leading "DD Mon" fragment - so the date has to come from here).
    private static final Pattern DATE_FRAGMENT_PATTERN =
            Pattern.compile("(\\d{1,2}\\s+[A-Za-z]{3,9})\\s*-\\s*");

    private static final DateTimeFormatter DAY_MONTH_FORMAT =
            new DateTimeFormatterBuilder().appendPattern("d MMM").toFormatter(Locale.ENGLISH);

    // "Award of Order" disclosures under SEBI LODR Regulation 30 consistently
    // phrase the order value as "Rs.XXX crore" / "₹XXX crore" in the sample
    // seen - a much more standardized, regulation-driven phrasing than free
    // press-release text, so (unlike the earlier NSE announcement source)
    // attempting extraction here is reasonable rather than guesswork.
    private static final Pattern VALUE_PATTERN =
            Pattern.compile("(?:Rs\\.?|₹)\\s*([\\d,]+(?:\\.\\d+)?)\\s*crore", Pattern.CASE_INSENSITIVE);

    // Event.title is VARCHAR(512), Event.description is VARCHAR(4000) -
    // truncate defensively, same discipline as the NSE/Google News sources.
    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MAX_DESCRIPTION_LENGTH = 3990;

    @Override
    public List<RawAnnouncement> fetchRecentAnnouncements(String companySymbol) {
        log.debug("Fetching announcements for symbol: {}", companySymbol);
        List<RawAnnouncement> result = new ArrayList<>();

        try {
            JsonNode root = apiClient.getStockData(companySymbol);
            result.addAll(parseCorporateActions(companySymbol, root));
        } catch (Exception e) {
            log.error("Failed to fetch corporate actions for {}: {}", companySymbol, e.getMessage(), e);
        }

        result.addAll(fetchAndParseRecentAnnouncements(companySymbol));
        return result;
    }

    /**
     * Parse corporate-action announcements from an already-fetched JsonNode.
     * Called by DiscoveryPipeline to avoid a redundant /stock call. Note
     * this does NOT cover /recent_announcements - that's a genuinely
     * separate endpoint not bundled in /stock's response, so it always
     * needs its own call regardless of which entry point is used here.
     */
    public List<RawAnnouncement> parseFromNode(String companySymbol, JsonNode root) {
        log.debug("Parsing announcements from cached node for: {}", companySymbol);
        List<RawAnnouncement> result = new ArrayList<>(parseCorporateActions(companySymbol, root));
        result.addAll(fetchAndParseRecentAnnouncements(companySymbol));
        return result;
    }

    private List<RawAnnouncement> fetchAndParseRecentAnnouncements(String companySymbol) {
        if (!recentAnnouncementsEnabled) {
            log.debug("Skipping /recent_announcements for {} (disabled via config)", companySymbol);
            return List.of();
        }
        try {
            JsonNode announcements = apiClient.getRecentAnnouncements(companySymbol);
            return parseRecentAnnouncements(announcements);
        } catch (Exception e) {
            log.warn("Failed to fetch recent_announcements for {}: {}", companySymbol, e.getMessage());
            return List.of();
        }
    }

    // ---- Corporate actions (Board Meetings / Dividends / Bonus) - unchanged ----

    private List<RawAnnouncement> parseCorporateActions(String companySymbol, JsonNode root) {
        List<RawAnnouncement> result = new ArrayList<>();

        JsonNode actions = root.path("stockCorporateActionData");

        JsonNode boardMeetings = actions.path("boardMeetings");
        if (boardMeetings.isArray()) {
            for (JsonNode meeting : boardMeetings) {
                String purpose = meeting.path("purpose").asText();
                String dateStr = meeting.path("boardMeetDate").asText();
                LocalDateTime date = parseCorporateActionDate(dateStr);
                if (date != null && purpose != null && !purpose.isEmpty()) {
                    // "date" here IS the event date (the meeting itself).
                    // stockCorporateActionData doesn't expose a separate
                    // "disclosed on" timestamp, so announcementDate uses the
                    // same value as a defensible default rather than
                    // guessing at a distinct disclosure date we don't have.
                    result.add(new RawAnnouncement(
                            "Board Meeting: " + purpose,
                            meeting.path("remarks").asText(),
                            date,
                            date,
                            null,
                            "NSE",
                            null
                    ));
                }
            }
        }

        JsonNode dividends = actions.path("dividend");
        if (dividends.isArray()) {
            for (JsonNode div : dividends) {
                String remarks = div.path("remarks").asText();
                String dateStr = div.path("recordDate").asText();
                if (dateStr == null || dateStr.isEmpty()) {
                    dateStr = div.path("xdDate").asText();
                }
                LocalDateTime date = parseCorporateActionDate(dateStr);
                if (date != null && remarks != null && !remarks.isEmpty()) {
                    result.add(new RawAnnouncement(
                            "Dividend: " + remarks,
                            "Dividend announcement",
                            date,
                            date,
                            null,
                            "NSE",
                            null
                    ));
                }
            }
        }

        JsonNode bonus = actions.path("bonus");
        if (bonus.isArray()) {
            for (JsonNode bn : bonus) {
                String remarks = bn.path("remarks").asText();
                String dateStr = bn.path("recordDate").asText();
                if (dateStr == null || dateStr.isEmpty()) {
                    dateStr = bn.path("xbDate").asText();
                }
                LocalDateTime date = parseCorporateActionDate(dateStr);
                if (date != null && remarks != null && !remarks.isEmpty()) {
                    result.add(new RawAnnouncement(
                            "Bonus Issue: " + remarks,
                            "Bonus issue announcement",
                            date,
                            date,
                            null,
                            "NSE",
                            null
                    ));
                }
            }
        }

        log.info("Parsed {} corporate action announcements for {}", result.size(), companySymbol);
        return result;
    }

    private LocalDateTime parseCorporateActionDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr, DATE_FORMAT).atStartOfDay();
        } catch (Exception e) {
            log.debug("Could not parse corporate action date: {}", dateStr);
            return null;
        }
    }

    // ---- Recent announcements (orders, expansions, director changes, etc.) ----

    private List<RawAnnouncement> parseRecentAnnouncements(JsonNode root) {
        List<RawAnnouncement> result = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return result;
        }

        for (JsonNode item : root) {
            try {
                RawAnnouncement parsed = parseAnnouncementItem(item);
                if (parsed != null) {
                    result.add(parsed);
                }
            } catch (Exception e) {
                log.debug("Skipping unparseable recent_announcements item: {}", e.getMessage());
            }
        }

        log.info("Parsed {} recent announcements", result.size());
        return result;
    }

    private RawAnnouncement parseAnnouncementItem(JsonNode item) {
        String rawTitle = item.path("title").asText(null);
        String link = item.path("link").asText(null);

        if (rawTitle == null || rawTitle.isBlank()) {
            return null;
        }

        Matcher matcher = DATE_FRAGMENT_PATTERN.matcher(rawTitle);
        if (!matcher.find()) {
            // No extractable "DD Mon -" fragment means no reliable date -
            // skip rather than fabricate a timeline position, same
            // discipline as the NSE/Google News sources.
            log.debug("No date fragment found in announcement, skipping: {}", rawTitle);
            return null;
        }

        String beforeMatch = rawTitle.substring(0, matcher.start()).trim();
        String dateFragment = matcher.group(1);
        String afterMatch = rawTitle.substring(matcher.end()).trim();

        LocalDateTime date = extractDateFromFragment(dateFragment);
        if (date == null) {
            return null;
        }

        String title = !beforeMatch.isBlank() ? beforeMatch : afterMatch;
        String description = (!beforeMatch.isBlank() && !afterMatch.isBlank()) ? afterMatch : null;
        BigDecimal valueCr = extractValueCr(rawTitle);

        // The extracted date here is genuinely closer to announcementDate
        // (when this was disclosed) than eventDate (when the underlying
        // order/action actually occurred) - BSE/NSE announcement feeds
        // conventionally lead with the filing date, and the narrative text
        // itself often references a separate, fuzzier event window (e.g.
        // "...orders worth Rs.730 crore since 10 August 2026" - the order
        // activity spans a period, it isn't a single dated event). Reliably
        // extracting that separate event date would mean parsing an
        // arbitrary embedded date out of free narrative prose, which is
        // exactly the kind of fragile guessing already avoided elsewhere in
        // this class (see the valueCr extraction comment) - so eventDate
        // uses the same value as a defensible default rather than a guess.
        return new RawAnnouncement(
                truncate(title, MAX_TITLE_LENGTH),
                truncate(description, MAX_DESCRIPTION_LENGTH),
                date,
                date,
                link,
                "BSE", // /recent_announcements links point to bseindia.com - confirmed from a real sample
                valueCr
        );
    }

    /**
     * "26 Aug" has no year at all. Combines with the current year, computed
     * fresh on every call (never baked into static state - a formatter with
     * a hardcoded year would silently go stale across a year boundary if
     * the app runs continuously). If that combination would land far in the
     * future, assumes it actually belongs to last year - handles the
     * routine edge case of a late-December announcement being fetched in
     * early January. The threshold is deliberately generous (60 days, not
     * just a few) - a normal recent announcement can easily be dated a
     * handful of days or weeks ahead of whenever the pipeline happens to
     * fetch it (upcoming AGM dates, board meeting dates, etc. are routinely
     * near-term future dates, not errors); the failure mode actually worth
     * correcting for is being off by close to a full year, not by a week.
     */
    private LocalDateTime extractDateFromFragment(String dayMonthFragment) {
        if (dayMonthFragment == null || dayMonthFragment.isBlank()) return null;
        try {
            MonthDay monthDay = MonthDay.parse(dayMonthFragment.trim(), DAY_MONTH_FORMAT);
            int currentYear = LocalDate.now().getYear();
            LocalDate candidate = monthDay.atYear(currentYear);
            if (candidate.isAfter(LocalDate.now().plusDays(60))) {
                candidate = monthDay.atYear(currentYear - 1);
            }
            return candidate.atStartOfDay();
        } catch (Exception e) {
            log.debug("Could not extract date from fragment '{}': {}", dayMonthFragment, e.getMessage());
            return null;
        }
    }

    private BigDecimal extractValueCr(String text) {
        if (text == null) return null;
        Matcher m = VALUE_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return new BigDecimal(m.group(1).replace(",", ""));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "…";
    }
}
