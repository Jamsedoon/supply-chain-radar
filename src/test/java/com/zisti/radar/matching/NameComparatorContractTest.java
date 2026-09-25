package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Checks that every comparator obeys the {@link NameComparator} contract.
 *
 * <p>As comparators are written in SC-03b and SC-03c, add them to
 * {@link #allComparators()} and they are checked automatically.
 */
class NameComparatorContractTest {

    /** Every comparator implementation. Add new ones here. */
    private static List<NameComparator> allComparators() {
        return List.of(
            new EditDistanceComparator(),
            new JaroWinklerComparator(),
            new KeyboardAdjacencyComparator()
            // SC-03c: new HomoglyphComparator(), new DelimiterComparator()
        );
    }

    @Test
    @DisplayName("every comparator returns a score between 0.0 and 1.0")
    void scoresStayInRange() {
        List<String> samples = List.of("lodash", "lodahs", "express", "", "a", "reactdom");

        for (NameComparator comparator : allComparators()) {
            for (String a : samples) {
                for (String b : samples) {
                    double score = comparator.compare(a, b);
                    assertTrue(
                        score >= 0.0 && score <= 1.0,
                        () -> comparator.name() + " returned " + score
                            + " for (\"" + a + "\", \"" + b + "\")");
                }
            }
        }
    }

    @Test
    @DisplayName("every comparator gives the same score in either order")
    void comparatorsAreSymmetric() {
        List<String> samples = List.of("lodash", "lodahs", "crossenv", "express");

        for (NameComparator comparator : allComparators()) {
            for (String a : samples) {
                for (String b : samples) {
                    assertEquals(
                        comparator.compare(a, b),
                        comparator.compare(b, a),
                        1e-9,
                        comparator.name() + " is not symmetric for \"" + a + "\" / \"" + b + "\"");
                }
            }
        }
    }

    @Test
    @DisplayName("every comparator has a unique, non-blank name")
    void namesAreUniqueAndPresent() {
        List<String> names = allComparators().stream().map(NameComparator::name).toList();

        for (String name : names) {
            assertTrue(name != null && !name.isBlank(), "comparator name must not be blank");
        }
        assertEquals(names.size(), names.stream().distinct().count(),
            "comparator names must be unique — they are the keys in the weights file");
    }
}