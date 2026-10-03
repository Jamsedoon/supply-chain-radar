package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WeightEstimatorTest {

    private final List<TruthSet.Row> rows = TruthSet.load();
    private final List<WeightEstimator.Estimate> estimates =
        WeightEstimator.withDefaults().estimate(rows);

    private static final double STRONG_BOUNDARY = 0.85;
    private static final double WEAK_BOUNDARY = 0.65;
    private static final double THRESHOLD = 9.0;

    @Test
    @DisplayName("prints the estimated parameters with their observation counts")
    void printEstimates() {
        System.out.println();
        System.out.println("=".repeat(96));
        System.out.println("  PARAMETERS ESTIMATED FROM THE TRUTH SET");
        System.out.println("=".repeat(96));
        System.out.printf("  %-20s %-7s %12s %14s %9s %10s %9s  %s%n",
            "comparator", "level", "squat hits", "legit hits", "m", "u", "bits", "note");
        System.out.println("-".repeat(96));

        for (WeightEstimator.Estimate e : estimates) {
            System.out.printf("  %-20s %-7s %7d/%-4d %9d/%-4d %9.4f %10.6f %9.2f  %s%n",
                e.comparator(), e.level(),
                e.squatHits(), e.squatTotal(),
                e.legitimateHits(), e.legitimateTotal(),
                e.m(), e.u(), e.weightBits(),
                e.isThinlyObserved() ? "THIN" : "");
        }

        System.out.println("=".repeat(96));
        System.out.println();
    }

    @Test
    @DisplayName("prints hand-estimated against learned weights, side by side")
    void printWeightComparison() {
        MatchWeights configured = MatchWeights.loadDefault();

        System.out.println();
        System.out.println("=".repeat(76));
        System.out.println("  HAND-ESTIMATED vs LEARNED WEIGHTS (bits)");
        System.out.println("=".repeat(76));
        System.out.printf("  %-20s %-7s %10s %10s %10s%n",
            "comparator", "level", "hand", "learned", "change");
        System.out.println("-".repeat(76));

        for (WeightEstimator.Estimate e : estimates) {
            double hand = configured.weightFor(e.comparator(), e.level());
            double learned = e.weightBits();

            System.out.printf("  %-20s %-7s %10.2f %10.2f %+10.2f%n",
                e.comparator(), e.level(), hand, learned, learned - hand);
        }

        System.out.println("=".repeat(76));
        System.out.println();
    }

    @Test
    @DisplayName("prints accuracy under hand-estimated against learned weights")
    void printAccuracyComparison() {
        Scorecard handTuned = scoreWith(MatchEngine.withDefaults());

        MatchEngine learnedEngine = new MatchEngine(
            List.of(
                new EditDistanceComparator(),
                new JaroWinklerComparator(),
                new KeyboardAdjacencyComparator(),
                new HomoglyphComparator(),
                new DelimiterComparator(),
                new FamilyPrefixComparator()),
            LearnedWeights.from(estimates, STRONG_BOUNDARY, WEAK_BOUNDARY, THRESHOLD));

        Scorecard learned = scoreWith(learnedEngine);

        System.out.println();
        System.out.println("=".repeat(68));
        System.out.println("  ACCURACY: HAND-ESTIMATED vs LEARNED");
        System.out.println("=".repeat(68));
        System.out.printf("  %-18s %12s %12s %12s%n", "", "hand", "learned", "change");
        System.out.println("-".repeat(68));
        printRow("precision", handTuned.precision(), learned.precision());
        printRow("recall", handTuned.recall(), learned.recall());
        printRow("f1", handTuned.f1(), learned.f1());
        System.out.println();
        System.out.printf("  %-18s %12d %12d%n", "false positives",
            handTuned.falsePositives(), learned.falsePositives());
        System.out.printf("  %-18s %12d %12d%n", "false negatives",
            handTuned.falseNegatives(), learned.falseNegatives());
        System.out.println("=".repeat(68));
        System.out.println();
    }

    @Test
    @DisplayName("prints the estimates as a properties file")
    void printAsProperties() {
        System.out.println();
        System.out.println(WeightEstimator.toPropertiesFormat(estimates));
    }

    @Test
    @DisplayName("every comparator is estimated at both levels")
    void coversEveryComparatorAndLevel() {
        assertEquals(12, estimates.size(),
            "six comparators at two levels each");
    }

    @Test
    @DisplayName("smoothing keeps every rate strictly above zero")
    void smoothingPreventsZeroRates() {
        for (WeightEstimator.Estimate e : estimates) {
            assertTrue(e.m() > 0.0,
                e.comparator() + " " + e.level() + " has m = 0, which breaks log2(m/u)");
            assertTrue(e.u() > 0.0,
                e.comparator() + " " + e.level() + " has u = 0, which breaks log2(m/u)");
        }
    }

    @Test
    @DisplayName("no weight is infinite or not-a-number")
    void weightsAreFinite() {
        for (WeightEstimator.Estimate e : estimates) {
            assertTrue(Double.isFinite(e.weightBits()),
                e.comparator() + " " + e.level() + " produced " + e.weightBits());
        }
    }

    @Test
    @DisplayName("the family-prefix clue is still learned as evidence against a match")
    void familyPrefixStaysNegative() {
        WeightEstimator.Estimate familyPrefix = estimates.stream()
            .filter(e -> e.comparator().equals("family_prefix") && e.level() == Level.STRONG)
            .findFirst()
            .orElseThrow();

        assertTrue(familyPrefix.weightBits() < 0,
            "the data should independently confirm that extending a package name "
                + "argues against a squat; got " + familyPrefix.weightBits() + " bits");
    }

    private Scorecard scoreWith(MatchEngine engine) {
        Scorecard card = new Scorecard();
        for (TruthSet.Row row : rows) {
            card.record(row, engine.score(row.candidate(), row.reference()));
        }
        return card;
    }

    private static void printRow(String label, double hand, double learned) {
        System.out.printf("  %-18s %11.1f%% %11.1f%% %+11.1f%%%n",
            label, hand * 100, learned * 100, (learned - hand) * 100);
    }
}
