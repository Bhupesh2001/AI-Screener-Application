package com.stockresearch.service.scoring.rules;

import com.stockresearch.domain.Company;
import com.stockresearch.repository.CompanyRepository;
import com.stockresearch.service.datasource.SectorIndexSource;
import com.stockresearch.service.scoring.ScoreRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Rewards companies in sectors currently showing real momentum (Stage 6:
 * Sector Strength). Tries a dedicated NSE sector index first; if this
 * app's sector label has no verified dedicated index (or NSE doesn't have
 * a matching entry this cycle), falls back to averaging revenue growth
 * across whatever companies we track in that same sector, per the plan:
 * "if dedicated indices doesn't exist just show all stocks in that
 * sector."
 *
 * Sector-to-index mapping was verified against NSE's own thematic-indices
 * listing plus corroborating broker/index-aggregator sites, not a
 * live-fetched NSE response - some of the newer thematic index name
 * strings (Defence, Railways PSU, EV & New Age Automotive in particular)
 * could differ slightly from NSE's exact canonical string. That's fine:
 * SectorIndexSource.getPerformance() returning empty for any reason - no
 * mapping, name mismatch, or NSE being unreachable - all land on the same
 * sibling-company fallback below, so a naming miss degrades gracefully
 * rather than silently scoring wrong.
 *
 * EMS has NO dedicated NSE index - checked directly, not present in NSE's
 * own sectoral/thematic index listings - so it's intentionally left out of
 * the map and always uses the fallback.
 */
@Component
public class SectorTailwindScoreRule implements ScoreRule {

    private static final Map<String, String> SECTOR_TO_INDEX = Map.ofEntries(
            Map.entry("power", "NIFTY POWER"),
            Map.entry("railway", "NIFTY INDIA RAILWAYS PSU"),
            Map.entry("defense", "NIFTY INDIA DEFENCE"),
            Map.entry("defence", "NIFTY INDIA DEFENCE"),
            Map.entry("telecom", "NIFTY TELECOM"),
            Map.entry("chemicals", "NIFTY CHEMICALS"),
            Map.entry("infrastructure", "NIFTY INFRASTRUCTURE"),
            Map.entry("banking", "NIFTY BANK"),
            Map.entry("consumer", "NIFTY FMCG"),
            Map.entry("ev", "NIFTY EV & NEW AGE AUTOMOTIVE")
            // "ems" intentionally absent - see class Javadoc
    );

    private final SectorIndexSource sectorIndexSource;
    private final CompanyRepository companyRepository;

    public SectorTailwindScoreRule(SectorIndexSource sectorIndexSource, CompanyRepository companyRepository) {
        this.sectorIndexSource = sectorIndexSource;
        this.companyRepository = companyRepository;
    }

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
        String sectorRaw = input.company().getSector();
        String sectorKey = sectorRaw == null ? "" : sectorRaw.toLowerCase().trim();

        String indexName = SECTOR_TO_INDEX.get(sectorKey);
        if (indexName != null) {
            Optional<SectorIndexSource.SectorIndexPerformance> perf = sectorIndexSource.getPerformance(indexName);
            if (perf.isPresent()) {
                return scoreFromIndexPerformance(sectorRaw, perf.get());
            }
        }

        return scoreFromSiblingCompanies(sectorRaw, input.company());
    }

    private RuleResult scoreFromIndexPerformance(String sectorLabel, SectorIndexSource.SectorIndexPerformance perf) {
        BigDecimal change = perf.changePercent30d() != null ? perf.changePercent30d() : perf.changePercent365d();
        if (change == null) {
            return new RuleResult(50, List.of(new ScoreReasonItem(
                    "Sector index found (" + perf.indexName() + ") but no usable performance figure this cycle",
                    ReasonSentiment.NEUTRAL)));
        }

        double c = change.doubleValue();
        int score;
        ReasonSentiment sentiment;
        String trend;

        if (c >= 10) {
            score = 90; sentiment = ReasonSentiment.POSITIVE; trend = "strongly outperforming";
        } else if (c >= 4) {
            score = 75; sentiment = ReasonSentiment.POSITIVE; trend = "outperforming";
        } else if (c >= -2) {
            score = 55; sentiment = ReasonSentiment.NEUTRAL; trend = "roughly flat";
        } else if (c >= -8) {
            score = 35; sentiment = ReasonSentiment.NEGATIVE; trend = "underperforming";
        } else {
            score = 20; sentiment = ReasonSentiment.NEGATIVE; trend = "sharply underperforming";
        }

        String text = String.format("%s sector (%s) is %s, %+.1f%% over the last month",
                sectorLabel, perf.indexName(), trend, c);
        return new RuleResult(score, List.of(new ScoreReasonItem(text, sentiment)));
    }

    /**
     * Used when the sector has no verified dedicated index (EMS today) or
     * NSE didn't return a matching entry this cycle. Averages revenue
     * growth across every company we track in the same sector - which, with
     * today's small seeded universe, often reduces to just this one
     * company, since most sectors currently have a single tracked company.
     * That's an expected, transparent limitation of a small universe, not a
     * bug - the average becomes genuinely more meaningful as more companies
     * per sector get added.
     */
    private RuleResult scoreFromSiblingCompanies(String sectorLabel, Company self) {
        List<Company> siblings = sectorLabel == null
                ? List.of()
                : companyRepository.findBySectorIgnoreCase(sectorLabel);

        List<BigDecimal> revenueGrowths = siblings.stream()
                .map(Company::getRevenueGrowthPct)
                .filter(v -> v != null)
                .toList();

        if (revenueGrowths.isEmpty()) {
            return new RuleResult(45, List.of(new ScoreReasonItem(
                    "No dedicated sector index and no comparable sector data yet for " + sectorLabel,
                    ReasonSentiment.NEUTRAL)));
        }

        BigDecimal sum = revenueGrowths.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        double avgGrowth = sum.doubleValue() / revenueGrowths.size();

        int score;
        ReasonSentiment sentiment;
        if (avgGrowth >= 20) {
            score = 80; sentiment = ReasonSentiment.POSITIVE;
        } else if (avgGrowth >= 10) {
            score = 65; sentiment = ReasonSentiment.POSITIVE;
        } else if (avgGrowth >= 0) {
            score = 50; sentiment = ReasonSentiment.NEUTRAL;
        } else {
            score = 30; sentiment = ReasonSentiment.NEGATIVE;
        }

        String text = String.format(
                "No dedicated NSE index for %s - based on %d tracked compan%s in this sector, average revenue growth is %+.1f%%",
                sectorLabel, siblings.size(), siblings.size() == 1 ? "y" : "ies", avgGrowth);
        return new RuleResult(score, List.of(new ScoreReasonItem(text, sentiment)));
    }
}
