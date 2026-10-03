package com.zisti.radar.matching;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estimates Fellegi-Sunter parameters by counting agreements in the truth set.
 *
 * <p>For every comparator and level, counts how often that comparator fired at
 * that level across the rows labelled as squats (giving m) and across the rows
 * labelled legitimate (giving u). The weight is log2(m / u), as before — only
 * the source of the numbers changes, from hand estimates to observed counts.
 *
 * <p>Counts are smoothed with a Jeffreys prior, adding 0.5 to each hit count and
 * 1 to each total. Without it, a comparator that never fires across the
 * legitimate rows gives u = 0 and an undefined weight. Zero observations in a
 * small sample means "rarer than one in n", not "impossible", and the smoothed
 * estimate says exactly that. The correction shrinks as the sample grows.
 */
public final class WeightEstimator {

    /** Jeffreys prior: half an observation added to each count. */
    private static final double PRIOR_HITS = 0.5;
    private static final double PRIOR_TOTAL = 1.0;

    /** Estimated parameters for one comparator at one level. */
    public record Estimate(
        String comparator,
        Level level,
        int squatHits,
        int squatTotal,
        int legitimateHits,
        int legitimateTotal,
        double m,
        double u,
        double weightBits) {

        /**
         * True when the estimate rests on too few observations to trust.
         *
         * <p>Only the squat side is checked. A legitimate-side count of zero is
         * informative rather than missing: "never fired across 33 legitimate
         * pairs" is a measurement. The smoothing handles the arithmetic.
         */
        public boolean isThinlyObserved() {
            return squatHits < 5;
        }
    }

    private final List<NameComparator> comparators;
    private final MatchWeights weights;

    public WeightEstimator(List<NameComparator> comparators, MatchWeights weights) {
        this.comparators = List.copyOf(comparators);
        this.weights = weights;
    }

    /** Builds an estimator using the default comparator set and bucket boundaries. */
    public static WeightEstimator withDefaults() {
        return new WeightEstimator(
            List.of(
                new EditDistanceComparator(),
                new JaroWinklerComparator(),
                new KeyboardAdjacencyComparator(),
                new HomoglyphComparator(),
                new DelimiterComparator(),
                new FamilyPrefixComparator()),
            MatchWeights.loadDefault());
    }

    /**
     * Estimates every parameter from the given labelled rows.
     *
     * @return one estimate per comparator per level, in a stable order
     */
    public List<Estimate> estimate(List<TruthSet.Row> rows) {
        List<TruthSet.Row> squats = rows.stream().filter(TruthSet.Row::isSquat).toList();
        List<TruthSet.Row> legitimate = rows.stream().filter(r -> !r.isSquat()).toList();

        List<Estimate> estimates = new ArrayList<>();

        for (NameComparator comparator : comparators) {
            for (Level level : List.of(Level.STRONG, Level.WEAK)) {
                int squatHits = countHits(comparator, level, squats);
                int legitimateHits = countHits(comparator, level, legitimate);

                double m = smoothed(squatHits, squats.size());
                double u = smoothed(legitimateHits, legitimate.size());

                estimates.add(new Estimate(
                    comparator.name(),
                    level,
                    squatHits,
                    squats.size(),
                    legitimateHits,
                    legitimate.size(),
                    m,
                    u,
                    log2(m / u)));
            }
        }

        return List.copyOf(estimates);
    }

    /** How many of these rows produced this level from this comparator. */
    private int countHits(NameComparator comparator, Level level, List<TruthSet.Row> rows) {
        int hits = 0;

        for (TruthSet.Row row : rows) {
            double rawScore = scoreRow(comparator, row);
            if (weights.bucket(rawScore) == level) {
                hits++;
            }
        }

        return hits;
    }

    /**
     * Runs one comparator against one row, respecting its input requirement.
     *
     * <p>Mirrors the engine: the delimiter comparator needs raw names, because
     * normalization removes the punctuation it detects.
     */
    private double scoreRow(NameComparator comparator, TruthSet.Row row) {
        if (comparator.name().equals("delimiter")) {
            return comparator.compare(row.candidate(), row.reference());
        }
        return comparator.compare(
            Normalizer.normalize(row.candidate()),
            Normalizer.normalize(row.reference()));
    }

    /** Jeffreys-smoothed rate: (hits + 0.5) / (total + 1). */
    private static double smoothed(int hits, int total) {
        return (hits + PRIOR_HITS) / (total + PRIOR_TOTAL);
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2);
    }

    /** Renders the estimates as a properties file, ready to compare or adopt. */
    public static String toPropertiesFormat(List<Estimate> estimates) {
        Map<String, List<Estimate>> byComparator = new LinkedHashMap<>();
        for (Estimate estimate : estimates) {
            byComparator.computeIfAbsent(estimate.comparator(), k -> new ArrayList<>())
                .add(estimate);
        }

        StringBuilder out = new StringBuilder();
        out.append("# Parameters estimated from the truth set by WeightEstimator.\n");
        out.append("# Counts smoothed with a Jeffreys prior (+0.5 hits, +1 total).\n");

        byComparator.forEach((comparator, group) -> {
            out.append("\n");
            for (Estimate estimate : group) {
                out.append(String.format("# %s %s: %d/%d squat, %d/%d legitimate%s%n",
                    comparator, estimate.level(),
                    estimate.squatHits(), estimate.squatTotal(),
                    estimate.legitimateHits(), estimate.legitimateTotal(),
                    estimate.isThinlyObserved() ? "   <-- thinly observed" : ""));
            }
            for (Estimate estimate : group) {
                out.append(String.format("%s.%s.m = %.4f%n",
                    comparator, estimate.level(), estimate.m()));
                out.append(String.format("%s.%s.u = %.6f%n",
                    comparator, estimate.level(), estimate.u()));
            }
        });

        return out.toString();
    }
}