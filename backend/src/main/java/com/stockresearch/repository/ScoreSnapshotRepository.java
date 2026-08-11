package com.stockresearch.repository;

import com.stockresearch.domain.ScoreSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScoreSnapshotRepository extends JpaRepository<ScoreSnapshot, Long> {

    @Query("""
        SELECT s FROM ScoreSnapshot s
        WHERE s.company.id = :companyId
        ORDER BY s.computedAt DESC
    """)
    List<ScoreSnapshot> findByCompanyIdOrderByComputedAtDesc(@Param("companyId") Long companyId, Pageable pageable);

    @Query("""
        SELECT s FROM ScoreSnapshot s
        WHERE s.company.id = :companyId
        ORDER BY s.computedAt DESC
        LIMIT 1
    """)
    Optional<ScoreSnapshot> findMostRecent(@Param("companyId") Long companyId);

    @Query("""
        SELECT s FROM ScoreSnapshot s
        WHERE s.id IN (
            SELECT MAX(s2.id) FROM ScoreSnapshot s2 GROUP BY s2.company.id
        )
        ORDER BY s.totalScore DESC
    """)
    List<ScoreSnapshot> findLatestForAllCompaniesOrderByScoreDesc();

    @Query("""
        SELECT s FROM ScoreSnapshot s
        WHERE s.company.sector = :sector
          AND s.id IN (SELECT MAX(s2.id) FROM ScoreSnapshot s2 GROUP BY s2.company.id)
        ORDER BY s.totalScore DESC
    """)
    List<ScoreSnapshot> findLatestForSectorOrderByScoreDesc(@Param("sector") String sector);
}
