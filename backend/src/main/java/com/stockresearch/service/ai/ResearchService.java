package com.stockresearch.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockresearch.domain.*;
import com.stockresearch.repository.AiHistoryRepository;
import com.stockresearch.repository.EventRepository;
import com.stockresearch.repository.NewsRepository;
import com.stockresearch.repository.ResearchSummaryRepository;
import com.stockresearch.service.scoring.ScoringEngine;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates "Generate AI Research" (Module 6 / Stage 8 of the discovery
 * spec). Gathers context, calls the configured AI provider via
 * AiClientRouter + PromptBuilder, parses the strict-JSON response into a
 * ResearchSummary, and logs the raw exchange to AiHistory for auditing.
 *
 * This is the ONLY place in the app that persists AI-authored research text,
 * and it never writes a buy/sell field because ResearchSummary has no such
 * column (see that entity's Javadoc).
 */
@Service
public class ResearchService {

    private final AiClientRouter aiClientRouter;
    private final PromptBuilder promptBuilder;
    private final ScoringEngine scoringEngine;
    private final EventRepository eventRepository;
    private final NewsRepository newsRepository;
    private final ResearchSummaryRepository researchSummaryRepository;
    private final AiHistoryRepository aiHistoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResearchService(
            AiClientRouter aiClientRouter,
            PromptBuilder promptBuilder,
            ScoringEngine scoringEngine,
            EventRepository eventRepository,
            NewsRepository newsRepository,
            ResearchSummaryRepository researchSummaryRepository,
            AiHistoryRepository aiHistoryRepository
    ) {
        this.aiClientRouter = aiClientRouter;
        this.promptBuilder = promptBuilder;
        this.scoringEngine = scoringEngine;
        this.eventRepository = eventRepository;
        this.newsRepository = newsRepository;
        this.researchSummaryRepository = researchSummaryRepository;
        this.aiHistoryRepository = aiHistoryRepository;
    }

    public ResearchSummary generateResearch(Company company, AppSettings settings) {
        List<Event> recentEvents = eventRepository.findByCompanyIdOrderByEventDateDesc(company.getId());
        List<News> recentNews = newsRepository.findByCompanyIdOrderByPublishedAtDesc(company.getId());

        ScoringEngine.ComputedScore score = scoringEngine.compute(company, recentEvents, recentNews);

        AiClient.AiRequest request = promptBuilder.buildResearchPrompt(company, recentEvents, recentNews, score);
        AiClient.AiResponse response = aiClientRouter.complete(request, settings);

        AiHistory.AiHistoryBuilder historyBuilder = AiHistory.builder()
                .company(company)
                .provider(settings.getAiProvider().name())
                .model(settings.getModelName())
                .purpose(AiHistory.AiCallPurpose.RESEARCH_SUMMARY)
                .promptSummary(truncate(request.userPrompt(), 4000))
                .success(response.success());

        if (!response.success()) {
            aiHistoryRepository.save(historyBuilder
                    .errorMessage(response.errorMessage())
                    .build());
            throw new AiRequestException(response.errorMessage());
        }

        aiHistoryRepository.save(historyBuilder
                .responseText(truncate(response.text(), 4000))
                .promptTokens(response.promptTokens())
                .completionTokens(response.completionTokens())
                .build());

        ResearchSummary summary = parseResponse(company, response.text());
        return researchSummaryRepository.save(summary);
    }

    private ResearchSummary parseResponse(Company company, String rawText) {
        try {
            String cleaned = stripMarkdownFences(rawText);
            JsonNode json = objectMapper.readTree(cleaned);

            return ResearchSummary.builder()
                    .company(company)
                    .businessOverview(json.path("businessOverview").asText(null))
                    .strengths(json.path("strengths").asText(null))
                    .weaknesses(json.path("weaknesses").asText(null))
                    .growthDrivers(json.path("growthDrivers").asText(null))
                    .governmentTailwinds(json.path("governmentTailwinds").asText(null))
                    .risks(json.path("risks").asText(null))
                    .recentDevelopments(json.path("recentDevelopments").asText(null))
                    .improvingAssessment(json.path("improvingAssessment").asText(null))
                    .futureMonitoringPoints(json.path("futureMonitoringPoints").asText(null))
                    .confidenceLevel(json.path("confidenceLevel").asText("Medium"))
                    .generatedAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            // If the AI didn't return clean JSON, fall back to storing the raw
            // text in businessOverview rather than losing the response entirely.
            return ResearchSummary.builder()
                    .company(company)
                    .businessOverview(rawText)
                    .confidenceLevel("Low")
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
    }

    private String stripMarkdownFences(String text) {
        if (text == null) return "{}";
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\n", "");
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        return t.trim();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    public static class AiRequestException extends RuntimeException {
        public AiRequestException(String message) {
            super(message);
        }
    }
}
