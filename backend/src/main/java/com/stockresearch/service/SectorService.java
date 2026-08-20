package com.stockresearch.service;

import com.stockresearch.domain.Company;
import com.stockresearch.domain.ScoreSnapshot;
import com.stockresearch.dto.CompanySummaryDto;
import com.stockresearch.dto.SectorSummaryDto;
import com.stockresearch.repository.CompanyRepository;
import com.stockresearch.repository.NewsRepository;
import com.stockresearch.repository.ScoreSnapshotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/** Backs the Sector Dashboard module. */
@Service
public class SectorService {

    public static final List<String> TRACKED_SECTORS = List.of(
            "Power", "Railway", "Defense", "EMS", "Telecom", "Chemicals",
            "Infrastructure", "Banking", "Consumer", "EV"
    );

    private final CompanyRepository companyRepository;
    private final ScoreSnapshotRepository scoreSnapshotRepository;
    private final NewsRepository newsRepository;

    public SectorService(
            CompanyRepository companyRepository,
            ScoreSnapshotRepository scoreSnapshotRepository,
            NewsRepository newsRepository
    ) {
        this.companyRepository = companyRepository;
        this.scoreSnapshotRepository = scoreSnapshotRepository;
        this.newsRepository = newsRepository;
    }

    @Transactional(readOnly = true)
    public List<SectorSummaryDto> getAllSectorSummaries() {
        return TRACKED_SECTORS.stream()
                .map(this::getSectorSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public SectorSummaryDto getSectorSummary(String sector) {
        List<Company> companies = companyRepository.findBySectorIgnoreCase(sector);
        List<ScoreSnapshot> latestSnapshots = scoreSnapshotRepository.findLatestForSectorOrderByScoreDesc(sector);

        BigDecimal avgRevenueGrowth = average(companies.stream().map(Company::getRevenueGrowthPct).toList());
        BigDecimal avgProfitGrowth = average(companies.stream().map(Company::getProfitGrowthPct).toList());

        int avgScore = latestSnapshots.isEmpty() ? 0 :
                (int) Math.round(latestSnapshots.stream().mapToInt(ScoreSnapshot::getTotalScore).average().orElse(0));
        int highestScore = latestSnapshots.isEmpty() ? 0 :
                latestSnapshots.stream().mapToInt(ScoreSnapshot::getTotalScore).max().orElse(0);

        int recentNewsCount = companies.stream()
                .mapToInt(c -> newsRepository.findByCompanyIdOrderByPublishedAtDesc(c.getId()).size())
                .sum();

        List<CompanySummaryDto> bestPerforming = latestSnapshots.stream()
                .limit(5)
                .map(snap -> toSummaryDto(snap.getCompany(), snap))
                .toList();

        return SectorSummaryDto.builder()
                .sector(sector)
                .averageRevenueGrowthPct(avgRevenueGrowth)
                .averageProfitGrowthPct(avgProfitGrowth)
                .averageScore(avgScore)
                .recentNewsCount(recentNewsCount)
                .highestScore(highestScore)
                .bestPerforming(bestPerforming)
                .build();
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> present = values.stream().filter(v -> v != null).toList();
        if (present.isEmpty()) return null;
        BigDecimal sum = present.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(present.size()), 2, RoundingMode.HALF_UP);
    }

    private CompanySummaryDto toSummaryDto(Company company, ScoreSnapshot latestSnapshot) {
        List<ScoreSnapshot> history = scoreSnapshotRepository.findByCompanyIdOrderByComputedAtDesc(company.getId(), PageRequest.of(0, 2));
        Integer previous = history.size() > 1 ? history.get(1).getTotalScore() : null;

        return CompanySummaryDto.builder()
                .id(company.getId())
                .symbol(company.getSymbol())
                .name(company.getName())
                .sector(company.getSector())
                .marketCapCr(company.getMarketCapCr())
                .currentPrice(company.getCurrentPrice())
                .currentScore(latestSnapshot.getTotalScore())
                .previousScore(previous)
                .scoreChange(previous != null ? latestSnapshot.getTotalScore() - previous : null)
                .build();
    }
}
