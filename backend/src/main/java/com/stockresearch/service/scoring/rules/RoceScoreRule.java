package com.stockresearch.service.scoring.rules;

import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RoceScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "ROCE";
    }

    @Override
    public double weight() {
        return 0.07;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        BigDecimal roce = input.company().getRoce();
        if (roce == null) {
            return new RuleResult(0, List.of(
                    new ScoreReasonItem("ROCE data not available", ReasonSentiment.NEUTRAL)));
        }

        double r = roce.doubleValue();
        int score;
        String text;
        ReasonSentiment sentiment;

        if (r >= 25) {
            score = 95;
            text = "ROCE excellent at " + r + "%";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (r >= 18) {
            score = 80;
            text = "ROCE strong at " + r + "%";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (r >= 12) {
            score = 60;
            text = "ROCE adequate at " + r + "%";
            sentiment = ReasonSentiment.NEUTRAL;
        } else if (r >= 6) {
            score = 35;
            text = "ROCE below average at " + r + "%";
            sentiment = ReasonSentiment.NEGATIVE;
        } else {
            score = 15;
            text = "ROCE weak at " + r + "%";
            sentiment = ReasonSentiment.NEGATIVE;
        }

        return new RuleResult(score, List.of(new ScoreReasonItem(text, sentiment)));
    }
}
