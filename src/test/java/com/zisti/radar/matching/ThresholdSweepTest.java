package com.zisti.radar.matching;

import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Finds the best threshold for a given set of weights.
 *
 * <p>The threshold is measured in the same unit as the weights, so it cannot be
 * carried from one weight set to another. Comparing two weight sets fairly means
 * giving each the threshold that suits it.
 *
 * <p>Sweeps every distinct total weight the rows produce, which is exact: the
 * decision can only change at a value some row actually scored.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThresholdSweepTest {

    private final List<TruthSet.Row> rows = TruthSet.load();

    private static final double STRONG_BOUNDARY = 0.85;
    private static final double WEAK_BOUNDARY = 0.65;

    private record Best(double threshold, double precision, double recall, double f1) {
    }

    @Test
    @DisplayName("sweeps the threshold for both weight sets and compares at each one's best")
    void sweepBothWeightSets() {
        List<NameComparator> comparators = List.of(
            new EditDistanceComparator(),
            new JaroWinklerComparator(),
            new KeyboardAdjacencyComparator(),
            new HomoglyphComparator(),
            new DelimiterComparator(),
            new FamilyPrefixComparator());

        MatchWeights hand = MatchWeights.loadDefault();
        MatchWeights learned = LearnedWeights.from(
            WeightEstimator.withDefaults().estimate(rows),
            STRONG_BOUNDARY, WEAK_BOUNDARY, 9.0);

        Best bestHand = sweep("HAND-ESTIMATED", comparators, hand);
        Best bestLearned = sweep("LEARNED", comparators, learned);

        System.out.println();
        System.out.println("=".repeat(68));
        System.out.println("  FAIR COMPARISON, each at its own best threshold");
        System.out.println("=".repeat(68));
        System.out.printf("  %-14s %12s %12s %12s%n", "", "hand", "learned", "change");
        System.out.println("-".repeat(68));
        System.out.printf("  %-14s %12.2f %12.2f%n",
            "threshold", bestHand.threshold(), bestLearned.threshold());
        System.out.printf("  %-14s %11.1f%% %11.1f%% %+11.1f%%%n",
            "precision", bestHand.precision() * 100, bestLearned.precision() * 100,
            (bestLearned.precision() - bestHand.precision()) * 100);
        System.out.printf("  %-14s %11.1f%% %11.1f%% %+11.1f%%%n",
            "recall", bestHand.recall() * 100, bestLearned.recall() * 100,
            (bestLearned.recall() - bestHand.recall()) * 100);
        System.out.printf("  %-14s %11.1f%% %11.1f%% %+11.1f%%%n",
            "f1", bestHand.f1() * 100, bestLearned.f1() * 100,
            (bestLearned.f1() - bestHand.f1()) * 100);
        System.out.println("=".repeat(68));
        System.out.println();
    }

    private Best sweep(String label, List<NameComparator> comparators, MatchWeights weights) {
        MatchEngine engine = new MatchEngine(comparators, weights);

        List<MatchResult> results = rows.stream()
            .map(row -> engine.score(row.candidate(), row.reference()))
            .toList();

        TreeSet<Double> candidates = new TreeSet<>();
        for (MatchResult result : results) {
            candidates.add(result.totalWeight());
        }

        System.out.println();
        System.out.println("=".repeat(62));
        System.out.println("  THRESHOLD SWEEP: " + label);
        System.out.println("=".repeat(62));
        System.out.printf("  %10s %12s %12s %12s%n",
            "threshold", "precision", "recall", "f1");
        System.out.println("-".repeat(62));

        Best best = new Best(0, 0, 0, -1);

        for (double threshold : candidates) {
            Scorecard card = new Scorecard();
            for (int i = 0; i < rows.size(); i++) {
                MatchResult original = results.get(i);
                card.record(rows.get(i), retarget(original, threshold));
            }

            System.out.printf("  %10.2f %11.1f%% %11.1f%% %11.1f%%%s%n",
                threshold, card.precision() * 100, card.recall() * 100, card.f1() * 100,
                card.f1() > best.f1() ? "   <-- best so far" : "");

            if (card.f1() > best.f1()) {
                best = new Best(threshold, card.precision(), card.recall(), card.f1());
            }
        }

        System.out.println("-".repeat(62));
        System.out.printf("  best threshold %.2f -> f1 %.1f%%%n",
            best.threshold(), best.f1() * 100);
        System.out.println("=".repeat(62));

        return best;
    }

    /** The same result re-decided against a different threshold. */
    private static MatchResult retarget(MatchResult result, double threshold) {
        return new MatchResult(
            result.candidate(),
            result.reference(),
            result.totalWeight(),
            threshold,
            result.totalWeight() >= threshold,
            result.evidence());
    }
}