package com.stockresearch.controller;

import com.stockresearch.dto.*;
import com.stockresearch.service.CompanyQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyQueryService companyQueryService;

    public CompanyController(CompanyQueryService companyQueryService) {
        this.companyQueryService = companyQueryService;
    }

    @GetMapping("/search")
    public List<CompanySummaryDto> search(@RequestParam(required = false) String q) {
        return companyQueryService.search(q);
    }

    @GetMapping("/filter")
    public List<CompanySummaryDto> filter(
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) BigDecimal minMarketCapCr,
            @RequestParam(required = false) BigDecimal maxMarketCapCr,
            @RequestParam(required = false) BigDecimal maxPe,
            @RequestParam(required = false) BigDecimal minRoce,
            @RequestParam(required = false) BigDecimal maxDebtToEquity,
            @RequestParam(required = false) BigDecimal minRevenueGrowthPct,
            @RequestParam(required = false) BigDecimal minProfitGrowthPct,
            @RequestParam(required = false) Integer minScore
    ) {
        CompanyFilterRequest req = CompanyFilterRequest.builder()
                .sector(sector)
                .minMarketCapCr(minMarketCapCr)
                .maxMarketCapCr(maxMarketCapCr)
                .maxPe(maxPe)
                .minRoce(minRoce)
                .maxDebtToEquity(maxDebtToEquity)
                .minRevenueGrowthPct(minRevenueGrowthPct)
                .minProfitGrowthPct(minProfitGrowthPct)
                .minScore(minScore)
                .build();
        return companyQueryService.filter(req);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyDetailDto> getById(@PathVariable Long id) {
        return companyQueryService.getCompanyDetail(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<CompanyDetailDto> getBySymbol(@PathVariable String symbol) {
        return companyQueryService.getCompanyDetailBySymbol(symbol)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/score-change")
    public ResponseEntity<ScoreChangeDto> getScoreChange(@PathVariable Long id) {
        return companyQueryService.getScoreChange(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/why-interesting")
    public ResponseEntity<WhyInterestingDto> getWhyInteresting(@PathVariable Long id) {
        return companyQueryService.getWhyInteresting(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
