package com.stockresearch.util;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Single source of truth for the government-policy-tailwind keyword list.
 * Used by GovernmentTailwindScoreRule (scoring), DiscoveryPipeline (sets
 * News.matchesGovernmentTheme during scheduled discovery), and NewsService
 * (sets the same flag during a manual per-company news refresh).
 *
 * This used to be three independently-maintained copies that had already
 * drifted apart: DiscoveryPipeline and NewsService were both missing
 * several themes (transmission, wind, battery, bharatnet, smart city, data
 * center, hydrogen, import substitution, manufacturing) that
 * GovernmentTailwindScoreRule's copy - the most complete of the three -
 * already covered, and NewsService's copy was additionally missing "ev".
 * In practice this meant whether a piece of news got flagged as a
 * government tailwind depended on which code path happened to process it,
 * not on the news itself. Consolidated here so there's exactly one list to
 * keep current going forward.
 */
public final class GovernmentTailwindKeywords {

    private GovernmentTailwindKeywords() {
        // utility class, not instantiable
    }

    public static final Set<String> KEYWORDS = Set.of(
            "pli", "defense", "defence", "railway", "power grid", "transmission",
            "renewable", "solar", "wind", "semiconductor", "electronics", "battery",
            "ev", "telecom", "bharatnet", "smart city", "data center", "infrastructure",
            "hydrogen", "import substitution", "atmanirbhar", "manufacturing"
    );

    /** True if any keyword appears anywhere in the given text (case-insensitive). */
    public static boolean matches(String text) {
        return !findMatches(text).isEmpty();
    }

    /**
     * Returns exactly which keywords matched, preserving KEYWORDS' iteration
     * order. GovernmentTailwindScoreRule needs the distinct count of matched
     * themes (not just a yes/no) to compute its score - DiscoveryPipeline
     * and NewsService only need matches(), for a simple boolean flag.
     */
    public static Set<String> findMatches(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String haystack = text.toLowerCase();
        Set<String> matched = new LinkedHashSet<>();
        for (String kw : KEYWORDS) {
            if (haystack.contains(kw)) {
                matched.add(kw);
            }
        }
        return matched;
    }
}
