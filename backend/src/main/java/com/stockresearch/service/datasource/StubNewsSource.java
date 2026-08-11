package com.stockresearch.service.datasource;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Stub implementation of NewsSource with canned articles for the seeded demo
 * companies. A real implementation would parse Google News RSS
 * (news.google.com/rss/search?q=...) and other feeds.
 *
 * TO REPLACE: implement a GoogleNewsRssSource implementing NewsSource,
 * mark it @Primary. This stub is otherwise unused.
 */
@Component
public class StubNewsSource implements NewsSource {

    private static final Map<String, List<NewsItem>> CANNED_NEWS = Map.of(
            "RVNL", List.of(
                    new NewsItem(
                            "RVNL bags Rs 890 crore railway electrification order",
                            "Rail Vikas Nigam Limited announced it has secured a new order for railway electrification works, continuing its strong order inflow momentum this fiscal year.",
                            "https://example.com/news/rvnl-order-890cr",
                            "Economic Times",
                            LocalDateTime.now().minusDays(2)
                    ),
                    new NewsItem(
                            "Government increases railway capex allocation for FY27",
                            "The Union Budget allocated higher capital expenditure towards railway infrastructure, with PSU railway contractors expected to be key beneficiaries.",
                            "https://example.com/news/railway-capex-fy27",
                            "Moneycontrol",
                            LocalDateTime.now().minusDays(6)
                    ),
                    new NewsItem(
                            "RVNL Q1 results: profit jumps on execution pickup",
                            "RVNL reported strong quarterly profit growth as project execution accelerated across its railway and metro portfolio.",
                            "https://example.com/news/rvnl-q1-results",
                            "Business Standard",
                            LocalDateTime.now().minusDays(12)
                    )
            ),
            "BEL", List.of(
                    new NewsItem(
                            "Bharat Electronics wins Rs 1,200 crore defense order",
                            "BEL announced fresh defense orders reinforcing its strong order book, with management guiding for continued revenue growth.",
                            "https://example.com/news/bel-defense-order",
                            "Economic Times",
                            LocalDateTime.now().minusDays(3)
                    ),
                    new NewsItem(
                            "Defense Ministry pushes indigenous electronics under Atmanirbhar Bharat",
                            "The government's continued push for indigenous defense electronics manufacturing is expected to benefit established players with strong government relationships.",
                            "https://example.com/news/atmanirbhar-defense-electronics",
                            "Livemint",
                            LocalDateTime.now().minusDays(9)
                    )
            ),
            "DEEPAKNTR", List.of(
                    new NewsItem(
                            "Deepak Nitrite commissions new specialty chemicals capacity",
                            "The company announced commissioning of new manufacturing capacity aimed at import substitution in specialty chemicals.",
                            "https://example.com/news/deepak-nitrite-capacity",
                            "Moneycontrol",
                            LocalDateTime.now().minusDays(5)
                    ),
                    new NewsItem(
                            "Chemical sector margins under pressure from Chinese imports",
                            "Domestic specialty chemical makers continue to face margin pressure due to aggressive pricing from Chinese competitors, though select players are gaining share.",
                            "https://example.com/news/chemical-sector-china-pressure",
                            "Business Standard",
                            LocalDateTime.now().minusDays(15)
                    )
            )
    );

    @Override
    public List<NewsItem> fetchRecentNews(String companySymbol, String companyName) {
        return CANNED_NEWS.getOrDefault(companySymbol.toUpperCase(), List.of());
    }
}
