package com.stockresearch.service.scoring.rules;

import com.stockresearch.domain.Event;
import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NewProductsScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "New Products";
    }

    @Override
    public double weight() {
        return 0.04;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        List<Event> productEvents = input.recentEvents().stream()
                .filter(e -> e.getType() == Event.EventType.NEW_PRODUCT
                        || e.getType() == Event.EventType.PATENT
                        || e.getType() == Event.EventType.JOINT_VENTURE
                        || e.getType() == Event.EventType.STRATEGIC_PARTNERSHIP)
                .toList();

        if (productEvents.isEmpty()) {
            return new RuleResult(45, List.of(
                    new ScoreReasonItem("No new product launches detected recently", ReasonSentiment.NEUTRAL)));
        }

        List<ScoreReasonItem> reasons = new ArrayList<>();
        for (Event e : productEvents) {
            reasons.add(new ScoreReasonItem(e.getTitle(), ReasonSentiment.POSITIVE));
        }

        int score = Math.min(90, 55 + productEvents.size() * 12);
        return new RuleResult(score, reasons);
    }
}
