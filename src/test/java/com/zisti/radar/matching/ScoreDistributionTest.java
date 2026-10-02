package com.zisti.radar.matching;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prints every truth-set row ranked by total evidence.
 *
 * <p>Precision and recall say how many decisions were wrong. This says where
 * the wrong ones sit relative to the threshold, which is the thing you actually
 * need in order to change anything. A failure two bits over the line is a
 * different problem from one eight bits over.
 *
 * <p>Not an assertion. This is an instrument.
 */
class ScoreDistributionTest {

    private final MatchEngine engine = MatchEngine.withDefaults();

    @Test
    @DisplayName("prints every row ranked by score, with the threshold marked")
    void printDistribution() {
        List<TruthSet.Row> rows = TruthSet.load();

        record Scored(TruthSet.Row row, MatchResult result) {
        }

        List<Scored> scored = rows.stream()
            .map(row -> new Scored(row, engine.score(row.candidate(), row.reference())))
            .sorted(Comparator.comparingDouble(
                (Scored s) -> s.result().totalWeight()).reversed())
            .toList();

        double threshold = scored.get(0).result().threshold();
        boolean thresholdDrawn = false;

        System.out.println();
        System.out.println("=".repeat(86));
        System.out.println("  SCORE DISTRIBUTION  (sorted high to low)");
        System.out.println("=".repeat(86));
        System.out.printf("  %-24s %-24s %8s  %-6s %-5s %s%n",
            "candidate", "reference", "bits", "truth", "call", "category");
        System.out.println("-".repeat(86));

        for (Scored s : scored) {
            double weight = s.result().totalWeight();

            if (!thresholdDrawn && weight < threshold) {
                System.out.printf("  %s  THRESHOLD %.2f  %s%n",
                    "-".repeat(28), threshold, "-".repeat(28));
                thresholdDrawn = true;
            }

            boolean correct = s.result().isMatch() == s.row().isSquat();

            System.out.printf("  %-24s %-24s %8.2f  %-6s %-5s %s%s%n",
                s.row().candidate(),
                s.row().reference(),
                weight,
                s.row().isSquat() ? "SQUAT" : "ok",
                s.result().isMatch() ? "flag" : "-",
                s.row().category(),
                correct ? "" : "   <-- WRONG");
        }

        if (!thresholdDrawn) {
            System.out.printf("  %s  THRESHOLD %.2f  %s%n",
                "-".repeat(28), threshold, "-".repeat(28));
        }

        System.out.println("=".repeat(86));
        System.out.println();
    }

    @Test
    @DisplayName("prints the full evidence table for every wrong decision")
    void printFailureEvidence() {
        Scorecard card = new Scorecard();
        for (TruthSet.Row row : TruthSet.load()) {
            card.record(row, engine.score(row.candidate(), row.reference()));
        }

        if (card.failures().isEmpty()) {
            System.out.println("\nNo failures to explain.\n");
            return;
        }

        System.out.println();
        System.out.println("=".repeat(72));
        System.out.println("  EVIDENCE FOR EVERY WRONG DECISION");
        System.out.println("=".repeat(72));

        for (Scorecard.Failure failure : card.failures()) {
            System.out.println();
            System.out.println("  " + (failure.wasFalsePositive()
                ? "FALSE ALARM" : "MISSED ATTACK")
                + "  [" + failure.row().category() + "]  "
                + failure.row().note());
            System.out.println();
            System.out.println(EvidenceFormatter.format(failure.result())
                .indent(2));
        }

        System.out.println("=".repeat(72));
        System.out.println();
    }
}