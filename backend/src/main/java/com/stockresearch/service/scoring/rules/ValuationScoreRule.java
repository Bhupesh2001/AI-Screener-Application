package com.stockresearch.service.scoring.rules;

import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Scores valuation richness using PE ratio as a simple proxy. Lower PE
 * (cheaper relative to earnings) scores higher, all else equal - this is a
 * simplification; a fuller version would compare PE against the company's
 * own historical average and sector peers.
 */
@Component
public class ValuationScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "Valuation";
    }

    @Override
    public double weight() {
        return 0.10;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        BigDecimal pe = input.company().getPeRatio();
        if (pe == null || pe.compareTo(BigDecimal.ZERO) <= 0) {
            return new RuleResult(50, List.of(
                    new ScoreReasonItem("PE ratio not available or not meaningful", ReasonSentiment.NEUTRAL)));
        }

        double p = pe.doubleValue();
        int score;
        String text;
        ReasonSentiment sentiment;

        if (p <= 15) {
            score = 85;
            text = "Valuation appears reasonable (PE " + p + ")";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (p <= 30) {
            score = 65;
            text = "Valuation is fair, in line with growth (PE " + p + ")";
            sentiment = ReasonSentiment.NEUTRAL;
        } else if (p <= 50) {
            score = 45;
            text = "Valuation slightly above historical average (PE " + p + ")";
            sentiment = ReasonSentiment.NEGATIVE;
        } else {
            score = 25;
            text = "Valuation appears stretched (PE " + p + ")";
            sentiment = ReasonSentiment.NEGATIVE;
        }

        return new RuleResult(score, List.of(new ScoreReasonItem(text, sentiment)));
    }
}
