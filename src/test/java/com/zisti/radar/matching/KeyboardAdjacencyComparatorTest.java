package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KeyboardAdjacencyComparatorTest {

    private final KeyboardAdjacencyComparator comparator = new KeyboardAdjacencyComparator();

    private static final double TOLERANCE = 0.001;

    @Test
    @DisplayName("a neighbouring-key swap scores 1.0")
    void neighbouringKeySwapScoresOne() {
        // 'q' sits directly above 'a' on QWERTY
        assertEquals(1.0, comparator.compare("lodqsh", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("a far-away-key swap scores 0.0")
    void distantKeySwapScoresZero() {
        // 'p' is on the opposite side of the keyboard from 'a'
        assertEquals(0.0, comparator.compare("lodpsh", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("this is the comparator that tells two one-letter typos apart")
    void separatesPlausibleFromImplausible() {
        double plausible = comparator.compare("lodqsh", "lodash");
        double implausible = comparator.compare("lodpsh", "lodash");

        assertTrue(plausible > implausible,
            "a neighbouring-key slip must outscore a far-key change");
    }

    @Test
    @DisplayName("one neighbour swap and one far swap scores 0.5")
    void mixedSwapsScoreHalf() {
        // 'q' for 'a' is a neighbour; 'p' for 'd' is not
        assertEquals(0.5, comparator.compare("lopqsh", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("three or more differences score 0.0")
    void tooManyDifferencesScoreZero() {
        assertEquals(0.0, comparator.compare("lqdqsq", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("different lengths score 0.0")
    void differentLengthsScoreZero() {
        assertEquals(0.0, comparator.compare("lodas", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("identical names score 0.0 because no substitution happened")
    void identicalNamesScoreZero() {
        assertEquals(0.0, comparator.compare("lodash", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("the score is the same in either order")
    void isSymmetric() {
        assertEquals(
            comparator.compare("lodqsh", "lodash"),
            comparator.compare("lodash", "lodqsh"),
            TOLERANCE);
    }

    @Test
    @DisplayName("digits are on the grid too")
    void handlesDigits() {
        // '1' sits next to '2' on the number row
        assertEquals(1.0, comparator.compare("mysql1", "mysql2"), TOLERANCE);
    }

    @Test
    @DisplayName("two empty names score 0.0 instead of crashing")
    void handlesEmptyNames() {
        assertEquals(0.0, comparator.compare("", ""), TOLERANCE);
    }
}