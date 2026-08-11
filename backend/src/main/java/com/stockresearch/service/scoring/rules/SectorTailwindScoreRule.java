package com.stockresearch.service.scoring.rules;

import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Rewards companies in sectors widely regarded as structurally strong right
 * now (Stage 6: Sector Strength). This is a simple static tiering for the
 * demo; a fuller implementation would compute this dynamically by averaging
 * revenue/profit growth across all companies in each sector (the repository
 * layer already supports this via ScoreSnapshotRepository.findLatestForSectorOrderByScoreDesc).
 */
@Component
public class SectorTailwindScoreRule implements ScoreRule {

    private static final Set<String> HIGH_MOMENTUM_SECTORS = Set.of(
            "railway", "defense", "defence", "power", "ems", "electronics manufacturing services"
    );

    private static final Set<String> MODERATE_MOMENTUM_SECTORS = Set.of(
            "chemicals", "infrastructure", "telecom", "ev", "renewable"
    );

    @Override
    public String category() {
        return "Sector Tailwind";
    }

    @Override
    public double weight() {
        return 0.05;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        String sector = input.company().getSector() == null ? "" : input.company().getSector().toLowerCase();

        if (HIGH_MOMENTUM_SECTORS.contains(sector)) {
            return new RuleResult(85, List.of(
                    new ScoreReasonItem("Operating in a high-momentum sector (" + input.company().getSector() + ")", ReasonSentiment.POSITIVE)));
        }
        if (MODERATE_MOMENTUM_SECTORS.contains(sector)) {
            return new RuleResult(60, List.of(
                    new ScoreReasonItem("Operating in a moderately strong sector (" + input.company().getSector() + ")", ReasonSentiment.NEUTRAL)));
        }
        return new RuleResult(45, List.of(
                new ScoreReasonItem("Sector momentum unclear or average (" + input.company().getSector() + ")", ReasonSentiment.NEUTRAL)));
    }
}
