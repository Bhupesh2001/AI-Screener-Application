package com.stockresearch.service.ai;

import com.stockresearch.domain.Company;
import com.stockresearch.domain.Event;
import com.stockresearch.domain.News;
import com.stockresearch.service.scoring.ScoringEngine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds prompts for AI research generation and discovery validation
 * (Stage 8 of the discovery spec). The system prompt is the single place
 * where the "never give Buy/Sell advice, only summarize" constraint is
 * enforced - every AI call in the app must route through here rather than
 * constructing ad-hoc prompts, so this constraint can't accidentally be
 * bypassed elsewhere.
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are a financial research assistant helping a personal investor \
            organize and understand publicly available information about Indian \
            listed companies.

            STRICT RULES YOU MUST FOLLOW:
            1. You must NEVER recommend buying or selling any security, and you must \
               NEVER use phrases like "buy", "sell", "accumulate", "good time to invest", \
               "target price", or similar recommendation language.
            2. Your job is ONLY to summarize, organize, and assess whether the \
               company's fundamentals/events suggest genuine improvement or not - \
               never to give investment advice.
            3. Be specific and grounded in the data provided. Do not invent facts, \
               numbers, or events that were not given to you.
            4. Where the data is ambiguous or insufficient, say so plainly rather \
               than guessing.
            5. Distinguish clearly between temporary/one-off factors and structural, \
               durable changes in the business.

            Respond ONLY in strict JSON with exactly these keys (all string values):
            businessOverview, strengths, weaknesses, growthDrivers, governmentTailwinds, \
            risks, recentDevelopments, improvingAssessment, futureMonitoringPoints, \
            confidenceLevel (one of "High", "Medium", "Low").
            Do not include any text outside the JSON object, no markdown fences.
            """;

    public AiClient.AiRequest buildResearchPrompt(
            Company company,
            List<Event> recentEvents,
            List<News> recentNews,
            ScoringEngine.ComputedScore score
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Company: ").append(company.getName()).append(" (").append(company.getSymbol()).append(")\n");
        sb.append("Sector: ").append(company.getSector()).append("\n");
        sb.append("Exchange: ").append(company.getExchange()).append("\n\n");

        sb.append("FINANCIALS:\n");
        appendIfPresent(sb, "Market Cap (Cr)", company.getMarketCapCr());
        appendIfPresent(sb, "Revenue Growth %", company.getRevenueGrowthPct());
        appendIfPresent(sb, "Profit Growth %", company.getProfitGrowthPct());
        appendIfPresent(sb, "Operating Margin %", company.getOperatingMarginPct());
        appendIfPresent(sb, "Debt to Equity", company.getDebtToEquity());
        appendIfPresent(sb, "ROCE %", company.getRoce());
        appendIfPresent(sb, "ROE %", company.getRoe());
        appendIfPresent(sb, "PE Ratio", company.getPeRatio());
        appendIfPresent(sb, "Promoter Holding %", company.getPromoterHoldingPct());
        appendIfPresent(sb, "Institutional Holding %", company.getInstitutionalHoldingPct());
        sb.append("\n");

        sb.append("RESEARCH SCORE BREAKDOWN (0-100 total = ").append(score.totalScore()).append("):\n");
        score.categoryScores().forEach((category, catScore) ->
                sb.append("- ").append(category).append(": ").append(catScore).append("\n"));
        sb.append("\n");

        sb.append("RECENT EVENTS:\n");
        if (recentEvents.isEmpty()) {
            sb.append("(none detected recently)\n");
        } else {
            sb.append(recentEvents.stream()
                    .map(e -> "- [" + e.getType() + "] " + e.getTitle()
                            + (e.getDescription() != null ? ": " + e.getDescription() : ""))
                    .collect(Collectors.joining("\n")));
            sb.append("\n");
        }
        sb.append("\n");

        sb.append("RECENT NEWS:\n");
        if (recentNews.isEmpty()) {
            sb.append("(none available)\n");
        } else {
            sb.append(recentNews.stream()
                    .map(n -> "- " + n.getHeadline() + (n.getSummary() != null ? " - " + n.getSummary() : ""))
                    .collect(Collectors.joining("\n")));
            sb.append("\n");
        }

        sb.append("\nBased on the above, provide your structured JSON analysis as instructed.");

        return new AiClient.AiRequest(SYSTEM_PROMPT, sb.toString());
    }

    private void appendIfPresent(StringBuilder sb, String label, Object value) {
        if (value != null) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }
}
