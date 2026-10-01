package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Measures the engine against the labelled truth set.
 *
 * <p>The assertion floors are deliberately permissive for now. This run exists
 * to produce a baseline number; SC-05c tunes against it and raises the floors
 * once there is something real to hold the engine to.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatchEngineAccuracyTest {

    private final MatchEngine engine = MatchEngine.withDefaults();
    private final Scorecard scorecard = buildScorecard();

    private Scorecard buildScorecard() {
        Scorecard card = new Scorecard();
        for (TruthSet.Row row : TruthSet.load()) {
            card.record(row, engine.score(row.candidate(), row.reference()));
        }
        return card;
    }

    @Test
    @DisplayName("prints the full accuracy report")
    void printsReport() {
        System.out.println();
        System.out.println("=".repeat(72));
        System.out.println("  ACCURACY AGAINST THE TRUTH SET");
        System.out.println("=".repeat(72));
        System.out.println();

        System.out.printf("  precision  %.1f%%   of everything flagged, this much was real%n",
            scorecard.precision() * 100);
        System.out.printf("  recall     %.1f%%   of all real squats, this much was caught%n",
            scorecard.recall() * 100);
        System.out.printf("  f1         %.1f%%   the two combined%n", scorecard.f1() * 100);
        System.out.println();

        System.out.println("  confusion matrix");
        System.out.printf("    %-26s %3d   correct catches%n",
            "true positives", scorecard.truePositives());
        System.out.printf("    %-26s %3d   FALSE ALARMS%n",
            "false positives", scorecard.falsePositives());
        System.out.printf("    %-26s %3d   correctly ignored%n",
            "true negatives", scorecard.trueNegatives());
        System.out.printf("    %-26s %3d   MISSED ATTACKS%n",
            "false negatives", scorecard.falseNegatives());
        System.out.printf("    %-26s %3d%n", "total rows", scorecard.total());
        System.out.println();

        if (!scorecard.failures().isEmpty()) {
            System.out.println("  failures by category");
            scorecard.failuresByCategory().forEach((category, count) ->
                System.out.printf("    %-26s %3d%n", category, count));
            System.out.println();
        }

        printFailureList("MISSED ATTACKS (should have been flagged)",
            scorecard.falseNegativeFailures());
        printFailureList("FALSE ALARMS (should not have been flagged)",
            scorecard.falsePositiveFailures());

        System.out.println("=".repeat(72));
    }

    private void printFailureList(String heading, List<Scorecard.Failure> failures) {
        if (failures.isEmpty()) {
            System.out.println("  " + heading + ": none");
            System.out.println();
            return;
        }

        System.out.println("  " + heading);
        for (Scorecard.Failure failure : failures) {
            System.out.printf("    %-22s vs %-22s %7.2f bits  [%s]%n",
                failure.row().candidate(),
                failure.row().reference(),
                failure.result().totalWeight(),
                failure.row().category());
        }
        System.out.println();
    }

    @Test
    @DisplayName("precision is above the current floor")
    void precisionFloor() {
        assertTrue(scorecard.precision() >= 0.50,
            "precision too low to be useful: " + scorecard.summary());
    }

    @Test
    @DisplayName("recall is above the current floor")
    void recallFloor() {
        assertTrue(scorecard.recall() >= 0.50,
            "recall too low to be useful: " + scorecard.summary());
    }

    @Test
    @DisplayName("every truth-set row was actually scored")
    void everyRowWasScored() {
        assertTrue(scorecard.total() == TruthSet.load().size(),
            "scored " + scorecard.total() + " rows but the truth set has "
                + TruthSet.load().size());
    }

    @Test
    @DisplayName("the engine is not simply flagging everything")
    void doesNotFlagEverything() {
        assertTrue(scorecard.trueNegatives() > 0,
            "nothing was correctly ignored — the engine flags every pair, which "
                + "would give perfect recall and worthless precision");
    }

    @Test
    @DisplayName("the engine is not simply flagging nothing")
    void doesNotFlagNothing() {
        assertTrue(scorecard.truePositives() > 0,
            "nothing was flagged at all — the threshold is unreachable");
    }
}