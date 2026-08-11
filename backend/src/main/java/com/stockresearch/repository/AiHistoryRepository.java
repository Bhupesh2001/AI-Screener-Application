package com.stockresearch.repository;

import com.stockresearch.domain.AiHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiHistoryRepository extends JpaRepository<AiHistory, Long> {

    List<AiHistory> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<AiHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
