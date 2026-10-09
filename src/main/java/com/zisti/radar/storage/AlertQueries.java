package com.zisti.radar.storage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads alerts for the dashboard.
 *
 * <p>Joins each alert to both packages involved so the dashboard can show the
 * download gap that made it suspicious, not just the name similarity.
 */
@Component
public class AlertQueries {

    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public AlertQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** One row in the ranked alert list. */
    public record AlertSummary(
        long id,
        String candidateName,
        String referenceName,
        double totalWeight,
        long candidateDownloads,
        long referenceDownloads,
        String verdict,
        Instant createdAt) {
    }

    /** One alert with everything needed to explain it. */
    public record AlertDetail(
        long id,
        String candidateName,
        String referenceName,
        double totalWeight,
        double threshold,
        long candidateDownloads,
        long referenceDownloads,
        String publisher,
        String repositoryUrl,
        String verdict,
        Instant createdAt,
        JsonNode evidence) {
    }

    private static final String JOINS = """
        FROM alert a
        JOIN observed_package  o ON o.name = a.candidate_name
        JOIN reference_package r ON r.name = a.reference_name
        """;

    /** Highest risk first. */
    public List<AlertSummary> list(int limit, int offset) {
        return jdbc.query("""
            SELECT a.id, a.candidate_name, a.reference_name, a.total_weight,
                   o.weekly_downloads AS candidate_downloads,
                   r.weekly_downloads AS reference_downloads,
                   a.verdict, a.created_at
            """ + JOINS + """
            ORDER BY a.total_weight DESC, a.created_at DESC
            LIMIT ? OFFSET ?
            """,
            (rs, row) -> new AlertSummary(
                rs.getLong("id"),
                rs.getString("candidate_name"),
                rs.getString("reference_name"),
                rs.getDouble("total_weight"),
                rs.getLong("candidate_downloads"),
                rs.getLong("reference_downloads"),
                rs.getString("verdict"),
                rs.getTimestamp("created_at").toInstant()),
            limit, offset);
    }

    public Optional<AlertDetail> find(long id) {
        List<AlertDetail> rows = jdbc.query("""
            SELECT a.id, a.candidate_name, a.reference_name, a.total_weight,
                   a.threshold, a.evidence::text AS evidence,
                   o.weekly_downloads AS candidate_downloads,
                   r.weekly_downloads AS reference_downloads,
                   o.publisher, o.repository_url, a.verdict, a.created_at
            """ + JOINS + """
            WHERE a.id = ?
            """,
            (rs, row) -> new AlertDetail(
                rs.getLong("id"),
                rs.getString("candidate_name"),
                rs.getString("reference_name"),
                rs.getDouble("total_weight"),
                rs.getDouble("threshold"),
                rs.getLong("candidate_downloads"),
                rs.getLong("reference_downloads"),
                rs.getString("publisher"),
                rs.getString("repository_url"),
                rs.getString("verdict"),
                rs.getTimestamp("created_at").toInstant(),
                json.readTree(rs.getString("evidence"))),
            id);

        return rows.stream().findFirst();
    }

    /** @return false if no alert has that id */
    public boolean setVerdict(long id, String verdict) {
        return jdbc.update("UPDATE alert SET verdict = ? WHERE id = ?", verdict, id) > 0;
    }
}