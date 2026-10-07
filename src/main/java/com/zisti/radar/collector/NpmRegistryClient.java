
package com.zisti.radar.collector;

import tools.jackson.databind.JsonNode;
import com.zisti.radar.queue.PackageMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads package metadata from the public npm registry.
 *
 * <p>Two APIs are involved: the registry search endpoint supplies names and
 * publish metadata, and a separate statistics endpoint supplies download counts.
 * Both are public and unauthenticated.
 *
 * <p>Every call is rate limited by a fixed delay. These are free public APIs
 * with no contract behind them; hammering them is both rude and a quick route
 * to being blocked.
 */
@Component
public class NpmRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(NpmRegistryClient.class);

    private static final String SEARCH_URL = "https://registry.npmjs.org/-/v1/search";
    private static final String DOWNLOADS_URL = "https://api.npmjs.org/downloads/point/last-week";

    /** Pause between calls. Deliberate politeness toward a free public API. */
    private static final Duration REQUEST_DELAY = Duration.ofMillis(200);

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private final ObjectMapper json = new ObjectMapper();

    /**
     * Fetches recently published packages.
     *
     * @param limit how many to return, capped at 250 by the API
     * @return packages with metadata and download counts, newest first
     */
    public List<PackageMessage> fetchRecent(int limit) {
        List<PackageMessage> results = new ArrayList<>();

        JsonNode searchResult = get(SEARCH_URL
            + "?text=keywords:*&size=" + Math.min(limit, 250)
            + "&popularity=0.0&quality=0.0&maintenance=1.0");

        if (searchResult == null) {
            log.warn("search returned nothing");
            return results;
        }

        for (JsonNode entry : searchResult.path("objects")) {
            JsonNode pkg = entry.path("package");
            String name = pkg.path("name").asText(null);

            if (name == null || name.isBlank()) {
                continue;
            }

            // The search endpoint already returns download counts. Using them
            // avoids one extra HTTP call per package against a free public API.
            long weekly = entry.path("downloads").path("weekly").asLong(-1);
            if (weekly < 0) {
                weekly = fetchWeeklyDownloads(name);   // fall back if absent
            }

            results.add(new PackageMessage(
                name,
                weekly,
                pkg.path("publisher").path("username").asText(null),
                pkg.path("links").path("repository").asText(null),
                parseInstant(pkg.path("date").asText(null))));

        }

        log.info("fetched {} packages from the registry", results.size());
        return results;
    }

    /**
     * Weekly download count for one package.
     *
     * <p>Returns 0 when unknown. That is the safe default: a package with no
     * known downloads is treated as new and unproven, which is what the scorer's
     * popularity check assumes.
     */
    public long fetchWeeklyDownloads(String packageName) {
        sleep(); // // only reached on the fallback path, so pace it
        JsonNode result = get(DOWNLOADS_URL + "/" + packageName);

        if (result == null || result.path("downloads").isMissingNode()) {
            return 0L;
        }

        return result.path("downloads").asLong(0L);
    }

    private JsonNode get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", "supply-chain-radar (portfolio project)")
                .GET()
                .build();

            HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                return null;
            }

            if (response.statusCode() != 200) {
                log.warn("npm returned {} for {}", response.statusCode(), url);
                return null;
            }

            return json.readTree(response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.warn("request failed: {} ({})", url, e.getMessage());
            return null;
        }
    }

    private static Instant parseInstant(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (Exception e) {
            return null;
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(REQUEST_DELAY.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}