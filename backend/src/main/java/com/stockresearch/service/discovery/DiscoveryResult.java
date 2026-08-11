package com.stockresearch.service.discovery;

import com.stockresearch.domain.Company;
import com.stockresearch.service.scoring.ScoringEngine;

/**
 * Result of running a company through the discovery pipeline: whether it
 * passed the fundamental screen (Stage 2), and if so, its computed score.
 * Companies that fail Stage 2 are excluded before scoring is even attempted -
 * this mirrors the spec's explicit staged filtering rather than scoring
 * everything indiscriminately.
 */
public record DiscoveryResult(
        Company company,
        boolean passedFundamentalScreen,
        String exclusionReason, // set only if passedFundamentalScreen is false
        ScoringEngine.ComputedScore score // null if excluded
) {
    public static DiscoveryResult excluded(Company company, String reason) {
        return new DiscoveryResult(company, false, reason, null);
    }

    public static DiscoveryResult included(Company company, ScoringEngine.ComputedScore score) {
        return new DiscoveryResult(company, true, null, score);
    }
}
