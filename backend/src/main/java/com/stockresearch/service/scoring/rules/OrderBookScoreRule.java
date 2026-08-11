package com.stockresearch.service.scoring.rules;

import com.stockresearch.domain.Event;
import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrderBookScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "Order Book";
    }

    @Override
    public double weight() {
        return 0.08;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        List<Event> orderEvents = input.recentEvents().stream()
                .filter(e -> e.getType() == Event.EventType.LARGE_ORDER
                        || e.getType() == Event.EventType.GOVERNMENT_CONTRACT)
                .toList();

        if (orderEvents.isEmpty()) {
            return new RuleResult(40, List.of(
                    new ScoreReasonItem("No major new orders detected recently", ReasonSentiment.NEUTRAL)));
        }

        BigDecimal totalValue = orderEvents.stream()
                .map(Event::getValueCr)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int score;
        if (totalValue.doubleValue() >= 1000) {
            score = 95;
        } else if (totalValue.doubleValue() >= 500) {
            score = 85;
        } else if (totalValue.doubleValue() >= 100) {
            score = 70;
        } else {
            score = 55;
        }

        List<ScoreReasonItem> reasons = new ArrayList<>();
        for (Event e : orderEvents) {
            String valueStr = e.getValueCr() != null ? " (₹" + e.getValueCr().toPlainString() + " Cr)" : "";
            reasons.add(new ScoreReasonItem("New order: " + e.getTitle() + valueStr, ReasonSentiment.POSITIVE));
        }

        return new RuleResult(score, reasons);
    }
}
