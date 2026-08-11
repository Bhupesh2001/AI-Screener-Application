package com.stockresearch.controller;

import com.stockresearch.domain.AppSettings;
import com.stockresearch.domain.Company;
import com.stockresearch.domain.ResearchSummary;
import com.stockresearch.dto.ResearchSummaryDto;
import com.stockresearch.repository.CompanyRepository;
import com.stockresearch.service.SettingsService;
import com.stockresearch.service.ai.ResearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies/{companyId}/research")
public class ResearchController {

    private final ResearchService researchService;
    private final CompanyRepository companyRepository;
    private final SettingsService settingsService;

    public ResearchController(
            ResearchService researchService,
            CompanyRepository companyRepository,
            SettingsService settingsService
    ) {
        this.researchService = researchService;
        this.companyRepository = companyRepository;
        this.settingsService = settingsService;
    }

    /** "Generate AI Research" button - Module 6 of the spec. */
    @PostMapping("/generate")
    public ResponseEntity<?> generateResearch(@PathVariable Long companyId) {
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }

        AppSettings settings = settingsService.getSettings();

        try {
            ResearchSummary summary = researchService.generateResearch(company, settings);
            return ResponseEntity.ok(toDto(summary));
        } catch (ResearchService.AiRequestException e) {
            return ResponseEntity.status(502).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    private ResearchSummaryDto toDto(ResearchSummary r) {
        return ResearchSummaryDto.builder()
                .id(r.getId())
                .businessOverview(r.getBusinessOverview())
                .strengths(r.getStrengths())
                .weaknesses(r.getWeaknesses())
                .growthDrivers(r.getGrowthDrivers())
                .governmentTailwinds(r.getGovernmentTailwinds())
                .risks(r.getRisks())
                .recentDevelopments(r.getRecentDevelopments())
                .improvingAssessment(r.getImprovingAssessment())
                .futureMonitoringPoints(r.getFutureMonitoringPoints())
                .confidenceLevel(r.getConfidenceLevel())
                .generatedAt(r.getGeneratedAt())
                .build();
    }
}
