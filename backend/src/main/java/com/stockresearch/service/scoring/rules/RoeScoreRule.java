package com.stockresearch.service.scoring.rules;

import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RoeScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "ROE";
    }

    @Override
    public double weight() {
        return 0.05;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        BigDecimal roe = input.company().getRoe();
        if (roe == null) {
            return new RuleResult(0, List.of(
                    new ScoreReasonItem("ROE data not available", ReasonSentiment.NEUTRAL)));
        }

        double r = roe.doubleValue();
        int score;
        String text;
        ReasonSentiment sentiment;

        if (r >= 22) {
            score = 90;
            text = "ROE excellent at " + r + "%";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (r >= 15) {
            score = 75;
            text = "ROE strong at " + r + "%";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (r >= 10) {
            score = 55;
            text = "ROE adequate at " + r + "%";
            sentiment = ReasonSentiment.NEUTRAL;
        } else {
            score = 30;
            text = "ROE below average at " + r + "%";
            sentiment = ReasonSentiment.NEGATIVE;
        }

        return new RuleResult(score, List.of(new ScoreReasonItem(text, sentiment)));
    }
}
