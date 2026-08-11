// backend/src/main/java/com/stockresearch/service/datasource/FundamentalsDataSource.java
package com.stockresearch.service.datasource;

import java.util.Optional;

/**
 * Abstraction for fetching fundamental data and shareholding patterns.
 */
public interface FundamentalsDataSource {

    Optional<FundamentalsSnapshot> fetchFundamentals(String symbol);
}