package com.stockresearch.service;

import com.stockresearch.domain.Company;
import com.stockresearch.domain.WatchlistItem;
import com.stockresearch.dto.CompanySummaryDto;
import com.stockresearch.repository.CompanyRepository;
import com.stockresearch.repository.ScoreSnapshotRepository;
import com.stockresearch.repository.WatchlistItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WatchlistService {

    private final WatchlistItemRepository watchlistItemRepository;
    private final CompanyRepository companyRepository;
    private final ScoreSnapshotRepository scoreSnapshotRepository;

    public WatchlistService(
            WatchlistItemRepository watchlistItemRepository,
            CompanyRepository companyRepository,
            ScoreSnapshotRepository scoreSnapshotRepository
    ) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.companyRepository = companyRepository;
        this.scoreSnapshotRepository = scoreSnapshotRepository;
    }

    public List<CompanySummaryDto> getWatchlist() {
        return watchlistItemRepository.findAll().stream()
                .map(item -> toSummaryDto(item.getCompany()))
                .toList();
    }

    public Optional<WatchlistItem> addToWatchlist(Long companyId, String note) {
        if (watchlistItemRepository.existsByCompanyId(companyId)) {
            return watchlistItemRepository.findByCompanyId(companyId);
        }
        Optional<Company> company = companyRepository.findById(companyId);
        if (company.isEmpty()) return Optional.empty();

        WatchlistItem item = WatchlistItem.builder()
                .company(company.get())
                .note(note)
                .build();
        return Optional.of(watchlistItemRepository.save(item));
    }

    public void removeFromWatchlist(Long companyId) {
        watchlistItemRepository.deleteByCompanyId(companyId);
    }

    public boolean isOnWatchlist(Long companyId) {
        return watchlistItemRepository.existsByCompanyId(companyId);
    }

    private CompanySummaryDto toSummaryDto(Company company) {
        var history = scoreSnapshotRepository.findByCompanyIdOrderByComputedAtDesc(company.getId(), PageRequest.of(0, 2));
        Integer current = history.isEmpty() ? null : history.get(0).getTotalScore();
        Integer previous = history.size() > 1 ? history.get(1).getTotalScore() : null;

        return CompanySummaryDto.builder()
                .id(company.getId())
                .symbol(company.getSymbol())
                .name(company.getName())
                .sector(company.getSector())
                .marketCapCr(company.getMarketCapCr())
                .currentPrice(company.getCurrentPrice())
                .currentScore(current)
                .previousScore(previous)
                .scoreChange(current != null && previous != null ? current - previous : null)
                .build();
    }
}
