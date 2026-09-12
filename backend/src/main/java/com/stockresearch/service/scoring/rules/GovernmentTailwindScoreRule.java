package com.stockresearch.service.scoring.rules;

import com.stockresearch.domain.Event;
import com.stockresearch.domain.News;
import com.stockresearch.service.scoring.ScoreRule;
import com.stockresearch.util.GovernmentTailwindKeywords;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements Stage 5 (Government Tailwind Detection) from the discovery
 * spec: scores companies higher when their news/announcements repeatedly
 * match government-policy themes (see GovernmentTailwindKeywords for the
 * canonical keyword list, shared with DiscoveryPipeline and NewsService).
 *
 * The keyword list is currently a static in-code set; to make it editable
 * from Settings without a redeploy, promote this to a DB-backed list (a
 * simple "tailwind_keyword" table) - the rest of this rule's logic would be
 * unaffected since it just needs a Set<String> of keywords.
 */
@Component
public class GovernmentTailwindScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "Government Policy";
    }

    @Override
    public double weight() {
        return 0.09;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        List<String> matchedThemes = new ArrayList<>();

        for (News n : input.recentNews()) {
            String haystack = (n.getHeadline() == null ? "" : n.getHeadline())
                    + " " + (n.getSummary() == null ? "" : n.getSummary());
            for (String kw : GovernmentTailwindKeywords.findMatches(haystack)) {
                if (!matchedThemes.contains(kw)) {
                    matchedThemes.add(kw);
                }
            }
        }

        for (Event e : input.recentEvents()) {
            if (e.getType() == Event.EventType.GOVERNMENT_CONTRACT
                    || e.getType() == Event.EventType.GOVERNMENT_APPROVAL
                    || e.getType() == Event.EventType.PLI_PARTICIPATION) {
                if (!matchedThemes.contains("govt-event")) {
                    matchedThemes.add("govt-event");
                }
            }
        }

        if (matchedThemes.isEmpty()) {
            return new RuleResult(30, List.of(
                    new ScoreReasonItem("No strong government policy tailwind detected", ReasonSentiment.NEUTRAL)));
        }

        int score = Math.min(95, 50 + matchedThemes.size() * 12);
        List<ScoreReasonItem> reasons = List.of(
                new ScoreReasonItem(
                        "Sector receiving government support/policy tailwind (" + matchedThemes.size() + " related theme(s) found)",
                        ReasonSentiment.POSITIVE
                )
        );

        return new RuleResult(score, reasons);
    }
}
