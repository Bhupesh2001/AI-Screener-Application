package com.stockresearch.service.discovery;

import com.stockresearch.domain.AppSettings;
import com.stockresearch.domain.Company;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Stage 2: Fundamental Screening. Removes companies that clearly don't
 * qualify based on configurable thresholds (from AppSettings). Every
 * rejection includes a specific reason so the process is transparent, not
 * a black box.
 */
@Component
public class FundamentalScreeningStage {

    public Optional<String> checkExclusion(Company company, AppSettings settings) {
        if (company.getMarketCapCr() != null) {
            BigDecimal cap = company.getMarketCapCr();
            if (cap.compareTo(BigDecimal.valueOf(settings.getMarketCapMinCr())) < 0) {
                return Optional.of("Market cap below minimum threshold (₹" + cap + " Cr < ₹" + settings.getMarketCapMinCr() + " Cr)");
            }
            if (cap.compareTo(BigDecimal.valueOf(settings.getMarketCapMaxCr())) > 0) {
                return Optional.of("Market cap above maximum threshold (₹" + cap + " Cr > ₹" + settings.getMarketCapMaxCr() + " Cr)");
            }
        }

        if (company.getRevenueGrowthPct() != null && company.getRevenueGrowthPct().compareTo(BigDecimal.ZERO) < 0) {
            return Optional.of("Revenue growth is negative (" + company.getRevenueGrowthPct() + "%)");
        }

        if (company.getPromoterHoldingPct() != null
                && company.getPromoterHoldingPct().doubleValue() < settings.getMinPromoterHoldingPct()) {
            return Optional.of("Promoter holding below minimum threshold ("
                    + company.getPromoterHoldingPct() + "% < " + settings.getMinPromoterHoldingPct() + "%)");
        }

        if (company.getDebtToEquity() != null
                && company.getDebtToEquity().doubleValue() > settings.getMaxDebtToEquity()) {
            return Optional.of("Debt-to-equity above maximum threshold ("
                    + company.getDebtToEquity() + " > " + settings.getMaxDebtToEquity() + ")");
        }

        if (company.getRoce() != null && company.getRoce().doubleValue() < settings.getMinRocePct()) {
            return Optional.of("ROCE below minimum threshold ("
                    + company.getRoce() + "% < " + settings.getMinRocePct() + "%)");
        }

        if (company.getOperatingCashFlowCr() != null
                && company.getOperatingCashFlowCr().compareTo(BigDecimal.ZERO) < 0) {
            return Optional.of("Operating cash flow is negative");
        }

        return Optional.empty(); // passed screening
    }
}
