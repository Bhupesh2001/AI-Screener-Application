package com.stockresearch.service.scoring;

import com.stockresearch.domain.Company;
import com.stockresearch.domain.Event;
import com.stockresearch.domain.News;

import java.util.List;

/**
 * Contract for a single, independent scoring rule (e.g. Revenue Score, Debt
 * Score, Government Tailwind Score). Each rule:
 *   - looks at whatever inputs it needs (company fundamentals, events, news)
 *   - returns a 0-100 sub-score
 *   - returns a list of human-readable reasons explaining that sub-score
 *
 * Rules are intentionally decoupled from each other so any one of them can
 * be tuned or replaced without touching the rest of the engine. See
 * ScoringEngine for how they're combined and weighted.
 */
public interface ScoreRule {

    /** Category name shown in the UI, e.g. "Revenue Growth". Must be stable/unique. */
    String category();

    /** Weight (0.0-1.0) this rule contributes to the final weighted score. */
    double weight();

    RuleResult evaluate(RuleInput input);

    /** Everything a rule might need. Not every rule uses every field. */
    record RuleInput(
            Company company,
            List<Event> recentEvents,
            List<News> recentNews
    ) {}

    record RuleResult(
            int score, // 0-100
            List<ScoreReasonItem> reasons
    ) {}

    record ScoreReasonItem(
            String text,
            ReasonSentiment sentiment
    ) {}

    enum ReasonSentiment {
        POSITIVE, NEGATIVE, NEUTRAL
    }
}
