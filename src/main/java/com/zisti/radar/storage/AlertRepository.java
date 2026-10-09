package com.zisti.radar.storage;

import com.zisti.radar.matching.MatchResult;
import com.zisti.radar.queue.PackageMessage;
import java.sql.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes observed packages and the alerts raised against them.
 *
 * <p>
 * An observed package must exist before an alert can reference it, because the alert table has a
 * foreign key to it. Callers therefore record the package first.
 *
 * <p>
 * Both writes are idempotent. SQS delivers at least once, so the same package may arrive twice; a
 * repeat must not create a second row or throw.
 */
@Component
public class AlertRepository {

    private static final Logger log = LoggerFactory.getLogger(AlertRepository.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public AlertRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Records a package seen on the registry.
     *
     * <p>
     * On a repeat, refreshes the download count and the observation time rather than inserting
     * again. Download counts move, and the newest value is the one the popularity check should use.
     */
    public void recordObserved(PackageMessage pkg, String normalizedName) {
        jdbc.update("""
                INSERT INTO observed_package
                    (name, normalized_name, weekly_downloads, publisher,
                     repository_url, published_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (name) DO UPDATE SET
                    weekly_downloads = EXCLUDED.weekly_downloads,
                    observed_at      = now()
                """, pkg.name(), normalizedName, pkg.weeklyDownloads(), pkg.publisher(),
                pkg.repositoryUrl(),
                pkg.publishedAt() == null ? null : Timestamp.from(pkg.publishedAt()));
    }

    /**
     * Saves an alert, with its full per-clue evidence.
     *
     * <p>
     * The evidence is stored as JSON so the dashboard can explain the decision rather than only
     * reporting a number.
     *
     * @return true if a new alert was created, false if one already existed
     */
    public boolean saveAlert(MatchResult result) {
        String evidenceJson = json.writeValueAsString(result.evidence());

        int rows = jdbc.update("""
                INSERT INTO alert
                    (candidate_name, reference_name, total_weight, threshold, evidence)
                VALUES (?, ?, ?, ?, ?::jsonb)
                ON CONFLICT (candidate_name, reference_name) DO NOTHING
                """, result.candidate(), result.reference(), result.totalWeight(),
                result.threshold(), evidenceJson);

        if (rows > 0) {
            log.info("ALERT  {} vs {}  {} bits", result.candidate(), result.reference(),
                    String.format("%.2f", result.totalWeight()));
        }

        return rows > 0;
    }

    /** Total alerts raised. Used by the API and by the metrics endpoint. */
    public int alertCount() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM alert", Integer.class);
        return count == null ? 0 : count;
    }
}
