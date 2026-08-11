package com.stockresearch.service.scoring.rules;

import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DebtReductionScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "Debt Reduction";
    }

    @Override
    public double weight() {
        return 0.06;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        BigDecimal de = input.company().getDebtToEquity();
        if (de == null) {
            return new RuleResult(0, List.of(
                    new ScoreReasonItem("Debt-to-equity data not available", ReasonSentiment.NEUTRAL)));
        }

        double d = de.doubleValue();
        int score;
        String text;
        ReasonSentiment sentiment;

        if (d <= 0.1) {
            score = 95;
            text = "Company is nearly debt-free (D/E " + d + ")";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (d <= 0.4) {
            score = 80;
            text = "Debt levels are low (D/E " + d + ")";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (d <= 0.8) {
            score = 55;
            text = "Debt levels are moderate (D/E " + d + ")";
            sentiment = ReasonSentiment.NEUTRAL;
        } else if (d <= 1.5) {
            score = 30;
            text = "Debt levels are elevated (D/E " + d + ")";
            sentiment = ReasonSentiment.NEGATIVE;
        } else {
            score = 10;
            text = "Debt levels are high (D/E " + d + ")";
            sentiment = ReasonSentiment.NEGATIVE;
        }

        return new RuleResult(score, List.of(new ScoreReasonItem(text, sentiment)));
    }
}
