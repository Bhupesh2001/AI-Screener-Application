// backend/src/main/java/com/stockresearch/service/datasource/NseSectorIndexSource.java
package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Real implementation of SectorIndexSource backed by NSE's unofficial
 * "all indices" JSON endpoint, fetched through the shared NseSessionClient
 * built for corporate announcements - that client's Javadoc specifically
 * anticipated this reuse.
 *
 * Endpoint: nseindia.com/api/allIndices - returns EVERY NSE index (Nifty 50,
 * every sectoral/thematic index, etc.) in a single response, so this class
 * fetches once and reuses the parsed result for every getPerformance()
 * lookup within the TTL window, rather than hitting NSE once per sector.
 *
 * CACHING NOTE: this deliberately does NOT use @Cacheable, unlike the other
 * Phase 1-3 sources. getPerformance() (the public entry point other classes
 * call) internally calls this class's own fetch-all method - and Spring's
 * @Cacheable relies on a proxy that only intercepts calls arriving from
 * OUTSIDE the bean. A same-object internal call bypasses that proxy
 * entirely, so @Cacheable here would silently never actually cache anything
 * while looking like it should. Hand-rolling the cache (lock + TTL
 * timestamp) sidesteps that trap - this mirrors the exact pattern
 * NseSessionClient already uses successfully for its own cookie state.
 *
 * CAVEAT (same as Phase 1-3): field names below are based on general
 * knowledge of this endpoint's shape from community tooling, not a
 * live-verified sample - nseindia.com isn't reachable from this build
 * environment. Parsing tries a couple of plausible field-name variants and
 * simply omits any index it can't get a usable number for, rather than
 * failing the whole fetch - SectorTailwindScoreRule's sibling-company
 * fallback (see that class) takes over cleanly for anything missing here.
 */
@Primary
@Component
class NseSectorIndexSource implements SectorIndexSource {

    private static final Logger log = LoggerFactory.getLogger(NseSectorIndexSource.class);
    private static final String ALL_INDICES_PATH = "/api/allIndices";
    private static final Duration CACHE_TTL = Duration.ofHours(1); // matches price/announcements/news cadence

    private final NseSessionClient sessionClient;
    private final ObjectMapper objectMapper;

    private final ReentrantLock lock = new ReentrantLock();
    private Map<String, SectorIndexPerformance> cachedIndices = Map.of();
    private Instant lastFetchedAt = Instant.EPOCH;

    public NseSectorIndexSource(NseSessionClient sessionClient, ObjectMapper objectMapper) {
        this.sessionClient = sessionClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SectorIndexPerformance> getPerformance(String indexName) {
        if (indexName == null || indexName.isBlank()) return Optional.empty();
        Map<String, SectorIndexPerformance> all = getOrFetchAllIndices();
        return Optional.ofNullable(all.get(indexName.trim().toUpperCase()));
    }

    private Map<String, SectorIndexPerformance> getOrFetchAllIndices() {
        lock.lock();
        try {
            if (Duration.between(lastFetchedAt, Instant.now()).compareTo(CACHE_TTL) < 0) {
                return cachedIndices; // still fresh
            }
        } finally {
            lock.unlock();
        }

        Map<String, SectorIndexPerformance> fresh = fetchAllIndices();

        lock.lock();
        try {
            if (!fresh.isEmpty()) {
                cachedIndices = fresh;
            }
            // On a failed/empty fetch, deliberately keep serving whatever we
            // had before (stale-but-present) rather than dropping to empty -
            // a transient NSE hiccup shouldn't force every sector into the
            // sibling-company fallback if we had good data a moment ago.
            // Still stamp the time so we don't hammer NSE again immediately.
            lastFetchedAt = Instant.now();
            return cachedIndices;
        } finally {
            lock.unlock();
        }
    }

    private Map<String, SectorIndexPerformance> fetchAllIndices() {
        Optional<String> body = sessionClient.get(ALL_INDICES_PATH);
        if (body.isEmpty()) {
            log.warn("No response from NSE allIndices endpoint this cycle");
            return Map.of();
        }

        Map<String, SectorIndexPerformance> result = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(body.get());
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                log.warn("Unexpected NSE allIndices response shape (no 'data' array)");
                return Map.of();
            }

            for (JsonNode item : data) {
                String name = textOrNull(item, "index");
                if (name == null) name = textOrNull(item, "indexName");
                if (name == null) continue;

                BigDecimal current = firstNumeric(item, "last", "lastPrice");
                BigDecimal change30d = firstNumeric(item, "perChange30d", "percentChange30d");
                BigDecimal change365d = firstNumeric(item, "perChange365d", "percentChange365d");

                if (change30d == null && change365d == null) {
                    continue; // no usable performance signal for this row
                }

                result.put(name.trim().toUpperCase(),
                        new SectorIndexPerformance(name, current, change30d, change365d));
            }
        } catch (Exception e) {
            log.warn("Failed to parse NSE allIndices response: {}", e.getMessage());
            return Map.of();
        }

        log.info("Parsed {} indices with usable performance data from NSE allIndices", result.size());
        return result;
    }

    private String textOrNull(JsonNode item, String field) {
        JsonNode node = item.path(field);
        if (node.isMissingNode() || node.isNull()) return null;
        String text = node.asText();
        return text.isBlank() ? null : text;
    }

    private BigDecimal firstNumeric(JsonNode item, String... fields) {
        for (String field : fields) {
            JsonNode node = item.path(field);
            if (node.isMissingNode() || node.isNull()) continue;
            try {
                return new BigDecimal(node.asText());
            } catch (NumberFormatException ignored) {
                // try the next candidate field name
            }
        }
        return null;
    }
}
