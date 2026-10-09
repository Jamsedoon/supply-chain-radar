package com.zisti.radar.api;

import com.zisti.radar.storage.AlertQueries;
import com.zisti.radar.storage.AlertQueries.AlertDetail;
import com.zisti.radar.storage.AlertQueries.AlertSummary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serves alerts to the dashboard.
 *
 * <p>Read-mostly. The only write is a reviewer's verdict on an alert, which
 * feeds the alert-precision metric: of the alerts a human has judged, how many
 * were real.
 *
 * <p>Runs when {@code APP_ROLE=api}, or {@code all} for local development.
 */
@RestController
@RequestMapping("/api/alerts")
@ConditionalOnExpression("'${radar.role}' == 'api' or '${radar.role}' == 'all'")
public class AlertController {

    private static final Set<String> VERDICTS = Set.of("REAL", "NOISE");
    private static final int MAX_PAGE_SIZE = 100;

    private final AlertQueries queries;

    public AlertController(AlertQueries queries) {
        this.queries = queries;
    }

    /** GET /api/alerts?limit=50&offset=0 — highest risk first. */
    @GetMapping
    public List<AlertSummary> list(@RequestParam(defaultValue = "50") int limit,
                                   @RequestParam(defaultValue = "0") int offset) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        int safeOffset = Math.max(0, offset);
        return queries.list(safeLimit, safeOffset);
    }

    /** GET /api/alerts/42 — one alert with its full evidence. */
    @GetMapping("/{id}")
    public AlertDetail get(@PathVariable long id) {
        return queries.find(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "no alert with id " + id));
    }

    /** The body of a verdict request: {"verdict": "REAL"} or {"verdict": "NOISE"}. */
    public record VerdictRequest(String verdict) {
    }

    /** POST /api/alerts/42/verdict — a reviewer marks the alert real or noise. */
    @PostMapping("/{id}/verdict")
    public Map<String, Object> setVerdict(@PathVariable long id,
                                          @RequestBody VerdictRequest request) {
        String verdict = request.verdict() == null ? "" : request.verdict().toUpperCase();

        if (!VERDICTS.contains(verdict)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "verdict must be REAL or NOISE");
        }

        if (!queries.setVerdict(id, verdict)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no alert with id " + id);
        }

        return Map.of("id", id, "verdict", verdict);
    }
}