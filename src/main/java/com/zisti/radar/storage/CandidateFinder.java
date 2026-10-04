package com.zisti.radar.storage;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Stage one of two-stage matching.
 *
 * <p>Narrows the reference set from thousands of packages to a handful of
 * plausible candidates, using a trigram index in Postgres. The expensive
 * comparators then run only on what survives.
 *
 * <p>Pushing this step into the database rather than looping in application
 * code is the difference between an index lookup and a full scan of every
 * reference package, per name scored.
 */
@Component
public class CandidateFinder {

      /** How many candidates to hand to the scoring stage. */
    private static final int MAX_CANDIDATES = 50;

    /**
     * Minimum trigram overlap to be considered at all.
     *
     * <p>Lowered from 0.3 after measurement: `momnet` against `moment` scores
     * 0.273, so a 0.3 floor silently discarded a known squat before the matching
     * engine saw it. Transpositions are the weak case — swapping two letters
     * destroys two trigrams, where a dropped letter damages one.
     *
     * <p>Cost: worst-case query time rose from 1.1 ms to 6.8 ms, still 5x faster
     * than a sequential scan.
     */
    private static final double SIMILARITY_FLOOR = 0.2;

    private static final String QUERY = """
        SELECT name, normalized_name, weekly_downloads,
               similarity(normalized_name, ?) AS sim
        FROM reference_package
        WHERE normalized_name %% ?
        ORDER BY sim DESC
        LIMIT ?
        """;

    private final JdbcTemplate jdbc;

    public CandidateFinder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.jdbc.execute("SELECT set_limit(" + SIMILARITY_FLOOR + ")");
    }

    /** One reference package that might be the thing being impersonated. */
    public record Candidate(String name, String normalizedName,
                            long weeklyDownloads, double trigramSimilarity) {
    }

    /** Finds the reference packages worth comparing against this name. */
    public List<Candidate> findCandidates(String normalizedName) {
        return jdbc.query(
            QUERY.formatted(),
            (rs, rowNum) -> new Candidate(
                rs.getString("name"),
                rs.getString("normalized_name"),
                rs.getLong("weekly_downloads"),
                rs.getDouble("sim")),
            normalizedName, normalizedName, MAX_CANDIDATES);
    }

    /** Total reference packages, for benchmarking what blocking avoids. */
    public int referenceCount() {
        Integer count = jdbc.queryForObject(
            "SELECT count(*) FROM reference_package", Integer.class);
        return count == null ? 0 : count;
    }
}