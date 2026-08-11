package com.stockresearch.repository;

import com.stockresearch.domain.ResearchSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResearchSummaryRepository extends JpaRepository<ResearchSummary, Long> {

    @Query("""
        SELECT r FROM ResearchSummary r
        WHERE r.company.id = :companyId
        ORDER BY r.generatedAt DESC
        LIMIT 1
    """)
    Optional<ResearchSummary> findMostRecentByCompanyId(@Param("companyId") Long companyId);
}
