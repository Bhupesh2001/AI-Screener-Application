package com.stockresearch.service.scoring.rules;

import com.stockresearch.domain.Event;
import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class InstitutionalBuyingScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "Institutional Buying";
    }

    @Override
    public double weight() {
        return 0.05;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        boolean buying = input.recentEvents().stream()
                .anyMatch(e -> e.getType() == Event.EventType.INSTITUTIONAL_BUYING
                        || e.getType() == Event.EventType.BULK_DEAL
                        || e.getType() == Event.EventType.BLOCK_DEAL);

        if (buying) {
            return new RuleResult(85, List.of(
                    new ScoreReasonItem("Institutional buying activity detected (bulk/block deals)", ReasonSentiment.POSITIVE)));
        }

        BigDecimal holding = input.company().getInstitutionalHoldingPct();
        if (holding == null) {
            return new RuleResult(45, List.of(
                    new ScoreReasonItem("Institutional holding data not available", ReasonSentiment.NEUTRAL)));
        }

        if (holding.doubleValue() >= 25) {
            return new RuleResult(65, List.of(
                    new ScoreReasonItem("Healthy institutional holding at " + holding + "%", ReasonSentiment.NEUTRAL)));
        }
        return new RuleResult(45, List.of(
                new ScoreReasonItem("Institutional holding modest at " + holding + "%", ReasonSentiment.NEUTRAL)));
    }
}
