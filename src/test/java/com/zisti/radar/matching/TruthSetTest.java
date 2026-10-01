package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TruthSetTest {

    private final List<TruthSet.Row> rows = TruthSet.load();

    @Test
    @DisplayName("the file loads and has enough rows to measure anything")
    void loadsEnoughRows() {
        assertTrue(rows.size() >= 50,
            "a truth set below 50 rows gives accuracy figures too noisy to quote; found "
                + rows.size());
    }

    @Test
    @DisplayName("squats and legitimate pairs are roughly balanced")
    void isRoughlyBalanced() {
        int squats = TruthSet.squats().size();
        int legitimate = TruthSet.legitimate().size();

        assertTrue(squats >= 20, "need at least 20 squat rows, found " + squats);
        assertTrue(legitimate >= 20,
            "need at least 20 legitimate rows, found " + legitimate);

        // A lopsided set makes one metric look good for the wrong reason.
        double ratio = (double) squats / legitimate;
        assertTrue(ratio > 0.5 && ratio < 2.0,
            "the set is too lopsided; squats=" + squats + " legitimate=" + legitimate);
    }

    @Test
    @DisplayName("no pair is listed twice")
    void hasNoDuplicates() {
        Set<String> seen = new HashSet<>();

        for (TruthSet.Row row : rows) {
            String key = row.candidate() + "|" + row.reference();
            assertTrue(seen.add(key), "duplicate row: " + row.describe());
        }
    }

    @Test
    @DisplayName("no row compares a package against itself")
    void hasNoSelfComparisons() {
        for (TruthSet.Row row : rows) {
            assertFalse(row.candidate().equals(row.reference()),
                "a package cannot be a squat of itself: " + row.describe());
        }
    }

    @Test
    @DisplayName("every row has a category and a note")
    void everyRowIsDocumented() {
        for (TruthSet.Row row : rows) {
            assertFalse(row.category().isBlank(),
                "missing category: " + row.candidate() + " vs " + row.reference());
            assertFalse(row.note().isBlank(),
                "missing note: " + row.describe());
        }
    }

    @Test
    @DisplayName("the hard false-positive patterns are all represented")
    void coversTheDangerousLegitimatePatterns() {
        Set<String> categories = TruthSet.legitimate().stream()
            .map(TruthSet.Row::category)
            .collect(HashSet::new, HashSet::add, HashSet::addAll);

        // Each of these is a different way a legitimate pair can look suspicious.
        assertTrue(categories.contains("family_prefix"),
            "need pairs like react-dom vs react");
        assertTrue(categories.contains("scoped_sibling"),
            "need pairs like lodash.merge vs lodash");
        assertTrue(categories.contains("short_similar"),
            "need short names that are close by edit distance");
        assertTrue(categories.contains("versioned_sibling"),
            "need pairs like mysql2 vs mysql");
    }

    @Test
    @DisplayName("the main attack patterns are all represented")
    void coversTheAttackPatterns() {
        Set<String> categories = TruthSet.squats().stream()
            .map(TruthSet.Row::category)
            .collect(HashSet::new, HashSet::add, HashSet::addAll);

        assertTrue(categories.contains("real_attack"),
            "need documented historical incidents");
        assertTrue(categories.contains("keyboard_slip"),
            "need cases only keyboard adjacency can catch");
        assertTrue(categories.contains("homoglyph"),
            "need cases only the homoglyph comparator can catch");
        assertTrue(categories.contains("delimiter"),
            "need cases only the delimiter comparator can catch");
        assertTrue(categories.contains("transposition"),
            "need swapped-letter cases");
        assertTrue(categories.contains("dropped_letter"),
            "need missing-letter cases");
    }

    @Test
    @DisplayName("prints the composition for the record")
    void printsComposition() {
        System.out.println("Truth set: " + rows.size() + " rows ("
            + TruthSet.squats().size() + " squats, "
            + TruthSet.legitimate().size() + " legitimate)");

        System.out.println("\nSquat categories:");
        TruthSet.squats().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                TruthSet.Row::category, java.util.stream.Collectors.counting()))
            .forEach((category, count) -> System.out.printf("  %-20s %d%n", category, count));

        System.out.println("\nLegitimate categories:");
        TruthSet.legitimate().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                TruthSet.Row::category, java.util.stream.Collectors.counting()))
            .forEach((category, count) -> System.out.printf("  %-20s %d%n", category, count));
    }
}