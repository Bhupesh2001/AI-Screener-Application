// backend/src/main/java/com/stockresearch/service/datasource/NseSessionClient.java
package com.stockresearch.service.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Shared session/cookie client for nseindia.com's unofficial JSON endpoints
 * (corporate announcements now; sector indices in a later phase). NSE's
 * website rejects direct API calls without a warm browser-like session: it
 * requires cookies obtained from first loading a normal page, plus
 * browser-shaped headers (User-Agent, Accept, Referer) on every subsequent
 * call, and 401/403s any request missing those.
 *
 * This client is intentionally the ONLY place that deals with that session
 * handshake - any data source hitting nseindia.com should go through
 * NseSessionClient.get(path) rather than building its own WebClient, so the
 * cookie/retry logic isn't duplicated per source (see the real-data
 * integration plan, section 4.1).
 *
 * Design notes:
 * - NSE's session cookie lifetime isn't publicly documented, so this
 *   re-bootstraps proactively after a conservative 4-minute TTL in addition
 *   to reactively on any 401/403 - better to re-warm a bit too often than
 *   to silently fail for minutes at a time.
 * - Never throws out to callers: any failure (network, unexpected status,
 *   exhausted retry) results in Optional.empty(), so a source can log and
 *   move on to the next company rather than taking down a whole discovery
 *   run (see plan section 4.3, "never let a live source break the pipeline").
 */
@Component
public class NseSessionClient {

    private static final Logger log = LoggerFactory.getLogger(NseSessionClient.class);

    private static final String BASE_URL = "https://www.nseindia.com";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final Duration SESSION_TTL = Duration.ofMinutes(4);

    private final WebClient webClient;
    private final ReentrantLock lock = new ReentrantLock();

    // Cookie jar: name -> value. Guarded by `lock`.
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private Instant lastBootstrapAt = Instant.EPOCH;

    public NseSessionClient() {
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("Accept", "*/*")
                .defaultHeader("Accept-Language", "en-US,en;q=0.9")
                .build();
    }

    /**
     * GETs a path under nseindia.com (e.g. "/api/corporate-announcements?...")
     * using a warm session. Returns the response body, or empty if the
     * session couldn't be established or the request ultimately failed.
     */
    public Optional<String> get(String path) {
        ensureWarmSession();

        Optional<String> result = doGet(path);
        if (result.isPresent()) {
            return result;
        }

        // Reactive path: our cookies may have been valid-looking but actually
        // stale/rejected server-side. Re-bootstrap once and retry once.
        log.info("NSE request to {} failed on first attempt, re-bootstrapping session and retrying once", path);
        lock.lock();
        try {
            lastBootstrapAt = Instant.EPOCH; // force re-bootstrap below
        } finally {
            lock.unlock();
        }
        ensureWarmSession();
        return doGet(path);
    }

    private Optional<String> doGet(String path) {
        try {
            String cookieHeader = buildCookieHeader();
            return webClient.get()
                    .uri(path)
                    .header("Referer", BASE_URL + "/")
                    .header("Cookie", cookieHeader)
                    .exchangeToMono(response -> {
                        HttpStatusCode status = response.statusCode();
                        if (status.is2xxSuccessful()) {
                            return response.bodyToMono(String.class).map(Optional::of);
                        }
                        log.warn("NSE returned status {} for {}", status.value(), path);
                        return response.releaseBody().thenReturn(Optional.<String>empty());
                    })
                    .block();
        } catch (Exception e) {
            log.warn("NSE request failed for {}: {}", path, e.getMessage());
            return Optional.empty();
        }
    }

    private void ensureWarmSession() {
        lock.lock();
        try {
            if (Duration.between(lastBootstrapAt, Instant.now()).compareTo(SESSION_TTL) < 0) {
                return; // still warm
            }
        } finally {
            lock.unlock();
        }
        bootstrap();
    }

    /** Visits the NSE homepage to obtain fresh session cookies. */
    private void bootstrap() {
        lock.lock();
        try {
            log.debug("Bootstrapping NSE session (fetching cookies from homepage)");
            Map<String, String> fresh = new LinkedHashMap<>();

            webClient.get()
                    .uri("/")
                    .exchangeToMono(response -> {
                        response.cookies().forEach((name, values) -> {
                            if (!values.isEmpty()) {
                                fresh.put(name, values.get(0).getValue());
                            }
                        });
                        // drain the body so the connection is released back to the pool
                        return response.releaseBody();
                    })
                    .block();

            if (!fresh.isEmpty()) {
                cookies.clear();
                cookies.putAll(fresh);
                lastBootstrapAt = Instant.now();
                log.debug("NSE session bootstrap succeeded, {} cookies captured", fresh.size());
            } else {
                log.warn("NSE session bootstrap returned no cookies - subsequent requests may fail");
                // Still stamp the time so we don't hammer the homepage on every
                // call if NSE is simply not issuing cookies right now; the
                // reactive re-bootstrap-on-failure path in get() covers the
                // case where this was a transient issue.
                lastBootstrapAt = Instant.now();
            }
        } catch (Exception e) {
            log.warn("NSE session bootstrap failed: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private String buildCookieHeader() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            cookies.forEach((name, value) -> {
                if (sb.length() > 0) sb.append("; ");
                sb.append(name).append('=').append(value);
            });
            return sb.toString();
        } finally {
            lock.unlock();
        }
    }
}
