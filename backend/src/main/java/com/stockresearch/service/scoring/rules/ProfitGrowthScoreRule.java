package com.stockresearch.service.scoring.rules;

import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ProfitGrowthScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "Profit Growth";
    }

    @Override
    public double weight() {
        return 0.10;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        BigDecimal growth = input.company().getProfitGrowthPct();
        if (growth == null) {
            return new RuleResult(0, List.of(
                    new ScoreReasonItem("Profit growth data not available", ReasonSentiment.NEUTRAL)));
        }

        double g = growth.doubleValue();
        int score;
        String text;
        ReasonSentiment sentiment;

        if (g >= 40) {
            score = 95;
            text = "Profit growth surging (+" + g + "%)";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (g >= 20) {
            score = 80;
            text = "Strong profit growth (+" + g + "%)";
            sentiment = ReasonSentiment.POSITIVE;
        } else if (g >= 10) {
            score = 60;
            text = "Steady profit growth (+" + g + "%)";
            sentiment = ReasonSentiment.NEUTRAL;
        } else if (g >= 0) {
            score = 40;
            text = "Profit growth is flat (+" + g + "%)";
            sentiment = ReasonSentiment.NEUTRAL;
        } else {
            score = 15;
            text = "Profit is declining (" + g + "%)";
            sentiment = ReasonSentiment.NEGATIVE;
        }

        return new RuleResult(score, List.of(new ScoreReasonItem(text, sentiment)));
    }
}
