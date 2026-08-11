package com.stockresearch.service.scoring.rules;

import com.stockresearch.domain.Event;
import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PromoterBuyingScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "Promoter Buying";
    }

    @Override
    public double weight() {
        return 0.06;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        boolean buying = input.recentEvents().stream()
                .anyMatch(e -> e.getType() == Event.EventType.PROMOTER_BUYING);
        boolean selling = input.recentEvents().stream()
                .anyMatch(e -> e.getType() == Event.EventType.PROMOTER_SELLING);

        if (buying) {
            return new RuleResult(90, List.of(
                    new ScoreReasonItem("Promoters have been buying shares recently", ReasonSentiment.POSITIVE)));
        }
        if (selling) {
            return new RuleResult(25, List.of(
                    new ScoreReasonItem("Promoters have been selling shares recently", ReasonSentiment.NEGATIVE)));
        }

        // No buying/selling activity detected - score based on holding stability/level
        BigDecimal holding = input.company().getPromoterHoldingPct();
        if (holding != null && holding.doubleValue() >= 60) {
            return new RuleResult(60, List.of(
                    new ScoreReasonItem("Promoter holding unchanged at " + holding + "% (high skin in the game)", ReasonSentiment.NEUTRAL)));
        }
        return new RuleResult(50, List.of(
                new ScoreReasonItem("Promoter holding unchanged, no recent buying activity", ReasonSentiment.NEUTRAL)));
    }
}
