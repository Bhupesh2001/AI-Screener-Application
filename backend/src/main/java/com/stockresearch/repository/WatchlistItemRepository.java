package com.stockresearch.repository;

import com.stockresearch.domain.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    Optional<WatchlistItem> findByCompanyId(Long companyId);

    boolean existsByCompanyId(Long companyId);

    void deleteByCompanyId(Long companyId);
}
