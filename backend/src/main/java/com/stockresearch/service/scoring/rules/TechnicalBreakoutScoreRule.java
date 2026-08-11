package com.stockresearch.service.scoring.rules;

import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implements Stage 7 (Technical Confirmation). Per spec this should NEVER
 * be the main score driver - hence the deliberately low weight() below - it
 * only confirms or slightly tempers what fundamentals/events already show.
 */
@Component
public class TechnicalBreakoutScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "Technical Breakout";
    }

    @Override
    public double weight() {
        return 0.04;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        BigDecimal price = input.company().getCurrentPrice();
        BigDecimal high52w = input.company().getWeek52High();
        BigDecimal low52w = input.company().getWeek52Low();

        if (price == null || high52w == null || low52w == null || high52w.compareTo(BigDecimal.ZERO) == 0) {
            return new RuleResult(0, List.of(
                    new ScoreReasonItem("Price data not available for technical analysis", ReasonSentiment.NEUTRAL)));
        }

        double pctOfHigh = price.divide(high52w, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();

        if (pctOfHigh >= 95) {
            return new RuleResult(90, List.of(
                    new ScoreReasonItem("Trading near 52-week high, breakout territory", ReasonSentiment.POSITIVE)));
        } else if (pctOfHigh >= 80) {
            return new RuleResult(70, List.of(
                    new ScoreReasonItem("Trading within striking distance of 52-week high", ReasonSentiment.POSITIVE)));
        } else if (pctOfHigh >= 60) {
            return new RuleResult(50, List.of(
                    new ScoreReasonItem("Trading in the middle of its 52-week range", ReasonSentiment.NEUTRAL)));
        } else {
            return new RuleResult(30, List.of(
                    new ScoreReasonItem("Trading well below 52-week high", ReasonSentiment.NEUTRAL)));
        }
    }
}
