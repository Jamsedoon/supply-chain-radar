package com.zisti.radar.matching;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tallies engine decisions against the labelled truth set.
 *
 * <p>Collects the four outcome counts, derives precision, recall, and F1, and
 * keeps the rows that were decided wrongly so they can be inspected during
 * tuning. Knowing the score is only half of it; knowing which cases produced it
 * is what tells you what to change.
 */
public final class Scorecard {

    /** One wrong decision, kept for inspection. */
    public record Failure(TruthSet.Row row, MatchResult result, boolean wasFalsePositive) {
    }

    private int truePositives;
    private int falsePositives;
    private int trueNegatives;
    private int falseNegatives;

    private final List<Failure> failures = new ArrayList<>();

    /** Records one decision. */
    public void record(TruthSet.Row row, MatchResult result) {
        boolean flagged = result.isMatch();
        boolean shouldFlag = row.isSquat();

        if (flagged && shouldFlag) {
            truePositives++;
        } else if (flagged) {
            falsePositives++;
            failures.add(new Failure(row, result, true));
        } else if (shouldFlag) {
            falseNegatives++;
            failures.add(new Failure(row, result, false));
        } else {
            trueNegatives++;
        }
    }

    public int truePositives() {
        return truePositives;
    }

    public int falsePositives() {
        return falsePositives;
    }

    public int trueNegatives() {
        return trueNegatives;
    }

    public int falseNegatives() {
        return falseNegatives;
    }

    public int total() {
        return truePositives + falsePositives + trueNegatives + falseNegatives;
    }

    /** Every wrongly decided row, in the order they were recorded. */
    public List<Failure> failures() {
        return List.copyOf(failures);
    }

    /** Only the false alarms — legitimate packages that were flagged. */
    public List<Failure> falsePositiveFailures() {
        return failures.stream().filter(Failure::wasFalsePositive).toList();
    }

    /** Only the misses — real squats that were not flagged. */
    public List<Failure> falseNegativeFailures() {
        return failures.stream().filter(f -> !f.wasFalsePositive()).toList();
    }

    /**
     * Of everything flagged, the fraction that was a real squat.
     *
     * <p>Returns 0.0 when nothing was flagged at all. Precision is strictly
     * undefined there, but an engine that flags nothing is not a useful engine,
     * so scoring it zero keeps comparisons honest.
     */
    public double precision() {
        int flagged = truePositives + falsePositives;
        return flagged == 0 ? 0.0 : (double) truePositives / flagged;
    }

    /** Of all the real squats, the fraction that was caught. */
    public double recall() {
        int actualSquats = truePositives + falseNegatives;
        return actualSquats == 0 ? 0.0 : (double) truePositives / actualSquats;
    }

    /**
     * The harmonic mean of precision and recall.
     *
     * <p>Chosen over a plain average because it punishes imbalance: 100%
     * precision with 10% recall gives 18%, not 55%.
     */
    public double f1() {
        double precision = precision();
        double recall = recall();
        return (precision + recall) == 0.0
            ? 0.0
            : 2 * precision * recall / (precision + recall);
    }

    /** Counts of wrong decisions, grouped by truth-set category. */
    public Map<String, Integer> failuresByCategory() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Failure failure : failures) {
            counts.merge(failure.row().category(), 1, Integer::sum);
        }
        return counts;
    }

    /** A short summary line for logs and reports. */
    public String summary() {
        return String.format(
            "precision=%.3f recall=%.3f f1=%.3f  (tp=%d fp=%d tn=%d fn=%d, n=%d)",
            precision(), recall(), f1(),
            truePositives, falsePositives, trueNegatives, falseNegatives, total());
    }
}