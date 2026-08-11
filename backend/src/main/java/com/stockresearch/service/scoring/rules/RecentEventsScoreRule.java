package com.stockresearch.service.scoring.rules;

import com.stockresearch.domain.Event;
import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Captures overall "news/event density" as a standalone signal, distinct
 * from the specific categorized rules (Order Book, Promoter Buying, etc).
 * A company with many positive-leaning events recently is "in motion" even
 * if no single rule captures the full picture.
 */
@Component
public class RecentEventsScoreRule implements ScoreRule {

    private static final java.util.Set<Event.EventType> NEGATIVE_TYPES = java.util.Set.of(
            Event.EventType.PROMOTER_SELLING, Event.EventType.CREDIT_RATING_DOWNGRADE
    );

    @Override
    public String category() {
        return "Recent Events";
    }

    @Override
    public double weight() {
        return 0.06;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        List<Event> events = input.recentEvents();

        if (events.isEmpty()) {
            return new RuleResult(35, List.of(
                    new ScoreReasonItem("No notable corporate events in the recent period", ReasonSentiment.NEUTRAL)));
        }

        long negativeCount = events.stream().filter(e -> NEGATIVE_TYPES.contains(e.getType())).count();
        long positiveCount = events.size() - negativeCount;

        int score = (int) Math.min(95, 40 + positiveCount * 10 - negativeCount * 15);
        score = Math.max(10, score);

        String text = positiveCount + " notable event(s) detected in the recent period"
                + (negativeCount > 0 ? " (" + negativeCount + " cautionary)" : "");
        ReasonSentiment sentiment = negativeCount > positiveCount ? ReasonSentiment.NEGATIVE
                : (positiveCount > 0 ? ReasonSentiment.POSITIVE : ReasonSentiment.NEUTRAL);

        return new RuleResult(score, List.of(new ScoreReasonItem(text, sentiment)));
    }
}
