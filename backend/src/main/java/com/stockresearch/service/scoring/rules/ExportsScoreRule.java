package com.stockresearch.service.scoring.rules;

import com.stockresearch.domain.Event;
import com.stockresearch.domain.News;
import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ExportsScoreRule implements ScoreRule {

    @Override
    public String category() {
        return "Exports";
    }

    @Override
    public double weight() {
        return 0.04;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        List<Event> exportEvents = input.recentEvents().stream()
                .filter(e -> e.getType() == Event.EventType.EXPORT_ORDER)
                .toList();

        boolean exportMentionedInNews = input.recentNews().stream()
                .anyMatch(n -> containsAny(n, "export"));

        List<ScoreReasonItem> reasons = new ArrayList<>();
        int score;

        if (!exportEvents.isEmpty()) {
            score = Math.min(90, 60 + exportEvents.size() * 15);
            for (Event e : exportEvents) {
                reasons.add(new ScoreReasonItem(e.getTitle(), ReasonSentiment.POSITIVE));
            }
        } else if (exportMentionedInNews) {
            score = 55;
            reasons.add(new ScoreReasonItem("Export activity mentioned in recent news", ReasonSentiment.NEUTRAL));
        } else {
            score = 40;
            reasons.add(new ScoreReasonItem("No significant export developments detected", ReasonSentiment.NEUTRAL));
        }

        return new RuleResult(score, reasons);
    }

    private boolean containsAny(News news, String keyword) {
        String haystack = ((news.getHeadline() == null ? "" : news.getHeadline())
                + " " + (news.getSummary() == null ? "" : news.getSummary())).toLowerCase();
        return haystack.contains(keyword.toLowerCase());
    }
}
