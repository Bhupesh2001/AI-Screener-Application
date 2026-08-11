package com.stockresearch.service.scoring;

import com.stockresearch.domain.*;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines every registered ScoreRule bean into a single weighted 0-100
 * score, with a full category breakdown and a flat list of human-readable
 * reasons (used by the "Why is this stock interesting?" card and the
 * "Why did the score change?" panel).
 *
 * Spring automatically injects every ScoreRule @Component into the List
 * below - to add a new scoring dimension, just add a new class implementing
 * ScoreRule; nothing here needs to change.
 */
@Service
public class ScoringEngine {

    private final List<ScoreRule> rules;

    public ScoringEngine(List<ScoreRule> rules) {
        this.rules = rules;
    }

    public ComputedScore compute(Company company, List<Event> recentEvents, List<News> recentNews) {
        ScoreRule.RuleInput input = new ScoreRule.RuleInput(company, recentEvents, recentNews);

        Map<String, Integer> categoryScores = new LinkedHashMap<>();
        List<ScoreReason> reasons = new java.util.ArrayList<>();

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (ScoreRule rule : rules) {
            ScoreRule.RuleResult result = rule.evaluate(input);
            categoryScores.put(rule.category(), result.score());
            weightedSum += result.score() * rule.weight();
            totalWeight += rule.weight();

            for (ScoreRule.ScoreReasonItem item : result.reasons()) {
                reasons.add(ScoreReason.builder()
                        .text(item.text())
                        .type(mapSentiment(item.sentiment()))
                        .category(rule.category())
                        .build());
            }
        }

        int totalScore = totalWeight > 0
                ? (int) Math.round(weightedSum / totalWeight)
                : 0;
        totalScore = Math.max(0, Math.min(100, totalScore));

        return new ComputedScore(totalScore, categoryScores, reasons);
    }

    /** Converts a 0-100 score into a 1-5 star rating for the "Why interesting" card. */
    public int toStarRating(int totalScore) {
        if (totalScore >= 85) return 5;
        if (totalScore >= 70) return 4;
        if (totalScore >= 55) return 3;
        if (totalScore >= 40) return 2;
        return 1;
    }

    public String toLabel(int totalScore) {
        if (totalScore >= 85) return "Highly Interesting";
        if (totalScore >= 70) return "Interesting";
        if (totalScore >= 55) return "Worth Watching";
        if (totalScore >= 40) return "Neutral";
        return "Weak Signal";
    }

    private ScoreReason.ReasonType mapSentiment(ScoreRule.ReasonSentiment sentiment) {
        return switch (sentiment) {
            case POSITIVE -> ScoreReason.ReasonType.POSITIVE;
            case NEGATIVE -> ScoreReason.ReasonType.NEGATIVE;
            case NEUTRAL -> ScoreReason.ReasonType.NEUTRAL;
        };
    }

    public record ComputedScore(
            int totalScore,
            Map<String, Integer> categoryScores,
            List<ScoreReason> reasons
    ) {}
}
