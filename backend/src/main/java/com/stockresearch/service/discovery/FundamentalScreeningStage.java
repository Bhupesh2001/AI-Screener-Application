package com.stockresearch.service.discovery;

import com.stockresearch.domain.AppSettings;
import com.stockresearch.domain.Company;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Slf4j
public class FundamentalScreeningStage {

    public Optional<String> checkExclusion(Company company, AppSettings settings) {
        // Market Cap
        if (company.getMarketCapCr() != null) {
            BigDecimal cap = company.getMarketCapCr();
            if (cap.compareTo(BigDecimal.valueOf(settings.getMarketCapMinCr())) < 0) {
                String reason = "Market cap below minimum threshold (₹" + cap + " Cr < ₹" + settings.getMarketCapMinCr() + " Cr)";
                log.info("Excluding {}: {}", company.getSymbol(), reason);
                return Optional.of(reason);
            }
            if (cap.compareTo(BigDecimal.valueOf(settings.getMarketCapMaxCr())) > 0) {
                String reason = "Market cap above maximum threshold (₹" + cap + " Cr > ₹" + settings.getMarketCapMaxCr() + " Cr)";
                log.info("Excluding {}: {}", company.getSymbol(), reason);
                return Optional.of(reason);
            }
        }

        // Revenue Growth
        if (company.getRevenueGrowthPct() != null && company.getRevenueGrowthPct().compareTo(BigDecimal.ZERO) < 0) {
            String reason = "Revenue growth is negative (" + company.getRevenueGrowthPct() + "%)";
            log.info("Excluding {}: {}", company.getSymbol(), reason);
            return Optional.of(reason);
        }

        // Promoter Holding
        if (company.getPromoterHoldingPct() != null
                && company.getPromoterHoldingPct().doubleValue() < settings.getMinPromoterHoldingPct()) {
            String reason = "Promoter holding below minimum threshold ("
                    + company.getPromoterHoldingPct() + "% < " + settings.getMinPromoterHoldingPct() + "%)";
            log.info("Excluding {}: {}", company.getSymbol(), reason);
            return Optional.of(reason);
        }

        // Debt to Equity
        if (company.getDebtToEquity() != null
                && company.getDebtToEquity().doubleValue() > settings.getMaxDebtToEquity()) {
            String reason = "Debt-to-equity above maximum threshold ("
                    + company.getDebtToEquity() + " > " + settings.getMaxDebtToEquity() + ")";
            log.info("Excluding {}: {}", company.getSymbol(), reason);
            return Optional.of(reason);
        }

        // ROCE
        if (company.getRoce() != null && company.getRoce().doubleValue() < settings.getMinRocePct()) {
            String reason = "ROCE below minimum threshold ("
                    + company.getRoce() + "% < " + settings.getMinRocePct() + "%)";
            log.info("Excluding {}: {}", company.getSymbol(), reason);
            return Optional.of(reason);
        }

        // Operating Cash Flow
        if (company.getOperatingCashFlowCr() != null
                && company.getOperatingCashFlowCr().compareTo(BigDecimal.ZERO) < 0) {
            String reason = "Operating cash flow is negative";
            log.info("Excluding {}: {}", company.getSymbol(), reason);
            return Optional.of(reason);
        }

        // Passed all checks
        log.debug("Company {} passed fundamental screening", company.getSymbol());
        return Optional.empty();
    }
}