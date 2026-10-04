package com.zisti.radar.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Measures what blocking avoids, and verifies it does not lose the answer.
 *
 * <p>Requires a running local database: docker compose up -d db
 */
@SpringBootTest
class BlockingBenchmarkTest {

    @Autowired
    private CandidateFinder finder;

    private record Probe(String squat, String expectedReference) {
    }

    private static final List<Probe> PROBES = List.of(
        new Probe("lodahs", "lodash"),
        new Probe("expres", "express"),
        new Probe("crossenv", "cross-env"),
        new Probe("reqct", "react"),
        new Probe("momnet", "moment"),
        new Probe("chslk", "chalk"),
        new Probe("mysqp", "mysql"),
        new Probe("axois", "axios"),
        new Probe("webpak", "webpack"),
        new Probe("eslnt", "eslint"));

    @Test
    @DisplayName("blocking narrows 5,000 reference packages to a handful")
    void blockingNarrowsTheSearch() {
        int referenceCount = finder.referenceCount();
        assertTrue(referenceCount > 4000,
            "reference data not loaded; found " + referenceCount);

        long start = System.nanoTime();
        int totalCandidates = 0;

        for (Probe probe : PROBES) {
            List<CandidateFinder.Candidate> candidates = finder.findCandidates(probe.squat());
            assertFalse(candidates.isEmpty(), "no candidates for " + probe.squat());
            totalCandidates += candidates.size();
        }

        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        double avgCandidates = (double) totalCandidates / PROBES.size();
        double avgMs = elapsedMs / PROBES.size();

        System.out.println();
        System.out.println("=".repeat(62));
        System.out.println("  BLOCKING BENCHMARK");
        System.out.println("=".repeat(62));
        System.out.printf("  reference packages            %,10d%n", referenceCount);
        System.out.printf("  comparisons without blocking  %,10d  per name%n", referenceCount);
        System.out.printf("  comparisons with blocking     %,10.1f  per name%n", avgCandidates);
        System.out.printf("  reduction                     %,10.0fx%n",
            referenceCount / avgCandidates);
        System.out.printf("  average query time            %10.2f  ms%n", avgMs);
        System.out.println("=".repeat(62));
        System.out.println();

        assertTrue(avgCandidates < 60, "blocking returned too many candidates");
        assertTrue(avgMs < 50, "query took " + avgMs + "ms");
    }

    @Test
    @DisplayName("blocking never discards the correct reference package")
    void blockingDoesNotLoseTheAnswer() {
        for (Probe probe : PROBES) {
            List<CandidateFinder.Candidate> candidates = finder.findCandidates(probe.squat());

            boolean found = candidates.stream()
                .anyMatch(c -> c.name().equals(probe.expectedReference()));

            assertTrue(found,
                "blocking lost " + probe.expectedReference() + " for " + probe.squat()
                    + " — this squat would be undetectable regardless of matching quality. "
                    + "Returned: " + candidates.stream()
                        .map(CandidateFinder.Candidate::name).toList());
        }
    }
}