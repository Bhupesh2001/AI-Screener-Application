package com.stockresearch.service.scoring.rules;

import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Scores operating margin level as a proxy for margin expansion. NOTE: with
 * only a single fundamental snapshot stored per company (see Company entity),
 * this rule currently scores the *absolute* margin level rather than a
 * true period-over-period expansion. Once historical fundamental snapshots
 * are stored (a natural next step - see ScoreSnapshot's design, which already
 * supports historical scores), this rule should be upgraded to compare
 * current vs prior-quarter margin directly.
 */
@Component
public class MarginExpansionScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "Margin Expansion";
    }

    @Override
    public double weight() {
        return 0.06;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        BigDecimal margin = input.company().getOperatingMarginPct();
        if (margin == null) {
            return new RuleResult(0, List.of(
                    new ScoreReasonItem("Operating margin data not available", ReasonSentiment.NEUTRAL)));
        }

        double m = margin.doubleValue();
        int score;
        String text;
        ReasonSentiment sentiment;

        if (m >= 25) {
            score = 90;
            text = "Operating margin strong at " + m + "%";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (m >= 15) {
            score = 70;
            text = "Operating margin healthy at " + m + "%";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (m >= 8) {
            score = 50;
            text = "Operating margin moderate at " + m + "%";
            sentiment = ReasonSentiment.NEUTRAL;
        } else {
            score = 25;
            text = "Operating margin thin at " + m + "%";
            sentiment = ReasonSentiment.NEGATIVE;
        }

        return new RuleResult(score, List.of(new ScoreReasonItem(text, sentiment)));
    }
}
