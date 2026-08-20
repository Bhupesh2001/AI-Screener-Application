package com.stockresearch.service.scoring.rules;

import com.stockresearch.domain.Event;
import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CapacityExpansionScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "Capacity Expansion";
    }

    @Override
    public double weight() {
        return 0.05;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        List<Event> expansionEvents = input.recentEvents().stream()
                .filter(e -> e.getType() == Event.EventType.CAPACITY_EXPANSION
                        || e.getType() == Event.EventType.NEW_FACTORY
                        || e.getType() == Event.EventType.PLANT_COMMISSIONING)
                .toList();

        if (expansionEvents.isEmpty()) {
            return new RuleResult(45, List.of(
                    new ScoreReasonItem("No capacity expansion announced recently", ReasonSentiment.NEUTRAL)));
        }

        List<ScoreReasonItem> reasons = new ArrayList<>();
        for (Event e : expansionEvents) {
            reasons.add(new ScoreReasonItem(e.getTitle(), ReasonSentiment.POSITIVE));
        }

        int score = Math.min(95, 60 + expansionEvents.size() * 15);
        return new RuleResult(score, reasons);
    }
}
