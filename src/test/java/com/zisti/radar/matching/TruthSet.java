package com.zisti.radar.matching;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The labelled dataset used to measure and tune the matching engine.
 *
 * <p>Loads {@code /truth-set.csv} from the test classpath. Every row states a
 * candidate name, a reference name, and whether the pair is a genuine typosquat.
 * Measured accuracy, tuned weights, and the parameters estimated in SC-31 all
 * derive from this file, so a malformed row is a loud failure rather than a
 * silently skipped line.
 */
public final class TruthSet {

    private static final String RESOURCE = "/truth-set.csv";
    private static final int EXPECTED_COLUMNS = 5;

    private TruthSet() {
        throw new AssertionError("TruthSet is a utility class");
    }

    /**
     * One labelled pair.
     *
     * @param candidate the name under suspicion, raw
     * @param reference the known-good name, raw
     * @param isSquat   the correct answer
     * @param category  the kind of case, used to group failures while tuning
     * @param note      a short human explanation of why this row exists
     */
    public record Row(
        String candidate,
        String reference,
        boolean isSquat,
        String category,
        String note) {

        /** Short form for failure messages. */
        public String describe() {
            return candidate + " vs " + reference + " [" + category + "]";
        }
    }

    /** Loads every row, in file order. */
    public static List<Row> load() {
        List<Row> rows = new ArrayList<>();

        try (InputStream in = TruthSet.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                    "truth set not found on the test classpath: " + RESOURCE
                        + " (expected at src/test/resources/truth-set.csv)");
            }

            BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip the header and any blank lines.
                if (lineNumber == 1 || line.isBlank()) {
                    continue;
                }

                rows.add(parseRow(line, lineNumber));
            }

        } catch (IOException e) {
            throw new IllegalStateException("could not read " + RESOURCE, e);
        }

        if (rows.isEmpty()) {
            throw new IllegalStateException("the truth set is empty");
        }

        return List.copyOf(rows);
    }

    /** Only the rows that are genuine typosquats. */
    public static List<Row> squats() {
        return load().stream().filter(Row::isSquat).toList();
    }

    /** Only the rows that are legitimate package pairs. */
    public static List<Row> legitimate() {
        return load().stream().filter(row -> !row.isSquat()).toList();
    }

    private static Row parseRow(String line, int lineNumber) {
        String[] parts = line.split(",", -1);

        if (parts.length != EXPECTED_COLUMNS) {
            throw new IllegalStateException(
                "line " + lineNumber + " has " + parts.length + " columns, expected "
                    + EXPECTED_COLUMNS + ": " + line);
        }

        String isSquatValue = parts[2].trim();

        if (!isSquatValue.equals("true") && !isSquatValue.equals("false")) {
            throw new IllegalStateException(
                "line " + lineNumber + " has is_squat='" + isSquatValue
                    + "', expected exactly 'true' or 'false'");
        }

        return new Row(
            parts[0].trim(),
            parts[1].trim(),
            Boolean.parseBoolean(isSquatValue),
            parts[3].trim(),
            parts[4].trim());
    }
}