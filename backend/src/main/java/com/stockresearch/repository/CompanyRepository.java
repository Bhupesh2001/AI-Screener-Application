package com.stockresearch.repository;

import com.stockresearch.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findBySymbolIgnoreCase(String symbol);

    List<Company> findBySectorIgnoreCase(String sector);

    @Query("""
        SELECT c FROM Company c
        WHERE (:query IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(c.symbol) LIKE LOWER(CONCAT('%', :query, '%')))
    """)
    List<Company> search(@Param("query") String query);

    @Query("""
        SELECT c FROM Company c
        WHERE (:sector IS NULL OR c.sector = :sector)
          AND (:minMarketCap IS NULL OR c.marketCapCr >= :minMarketCap)
          AND (:maxMarketCap IS NULL OR c.marketCapCr <= :maxMarketCap)
          AND (:maxPe IS NULL OR c.peRatio <= :maxPe)
          AND (:minRoce IS NULL OR c.roce >= :minRoce)
          AND (:maxDebtToEquity IS NULL OR c.debtToEquity <= :maxDebtToEquity)
          AND (:minRevenueGrowth IS NULL OR c.revenueGrowthPct >= :minRevenueGrowth)
          AND (:minProfitGrowth IS NULL OR c.profitGrowthPct >= :minProfitGrowth)
    """)
    List<Company> filter(
            @Param("sector") String sector,
            @Param("minMarketCap") BigDecimal minMarketCap,
            @Param("maxMarketCap") BigDecimal maxMarketCap,
            @Param("maxPe") BigDecimal maxPe,
            @Param("minRoce") BigDecimal minRoce,
            @Param("maxDebtToEquity") BigDecimal maxDebtToEquity,
            @Param("minRevenueGrowth") BigDecimal minRevenueGrowth,
            @Param("minProfitGrowth") BigDecimal minProfitGrowth
    );

    @Query("SELECT c FROM Company c LEFT JOIN ScoreSnapshot s ON s.company = c " +
            "GROUP BY c.id ORDER BY MAX(s.computedAt) ASC NULLS FIRST")
    List<Company> findAllOrderByLastScoreAsc();

    List<Company> findAllBySectorIgnoreCaseOrderByMarketCapCrDesc(String sector);
}
