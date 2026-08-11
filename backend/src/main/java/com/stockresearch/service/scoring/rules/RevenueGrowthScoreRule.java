package com.stockresearch.service.scoring.rules;

import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RevenueGrowthScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "Revenue Growth";
    }

    @Override
    public double weight() {
        return 0.10;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        BigDecimal growth = input.company().getRevenueGrowthPct();
        if (growth == null) {
            return new RuleResult(0, List.of(
                    new ScoreReasonItem("Revenue growth data not available", ReasonSentiment.NEUTRAL)));
        }

        double g = growth.doubleValue();
        int score;
        String text;
        ReasonSentiment sentiment;

        if (g >= 25) {
            score = 95;
            text = "Revenue growth strongly accelerating (+" + g + "%)";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (g >= 15) {
            score = 80;
            text = "Healthy revenue growth (+" + g + "%)";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (g >= 8) {
            score = 60;
            text = "Moderate revenue growth (+" + g + "%)";
            sentiment = ReasonSentiment.NEUTRAL;
        } else if (g >= 0) {
            score = 40;
            text = "Revenue growth is sluggish (+" + g + "%)";
            sentiment = ReasonSentiment.NEUTRAL;
        } else {
            score = 15;
            text = "Revenue is declining (" + g + "%)";
            sentiment = ReasonSentiment.NEGATIVE;
        }

        return new RuleResult(score, List.of(new ScoreReasonItem(text, sentiment)));
    }
}
