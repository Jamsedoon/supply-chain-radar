package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EditDistanceComparatorTest {

    private final EditDistanceComparator comparator = new EditDistanceComparator();

    /** Doubles are never exactly equal, so compare within a small tolerance. */
    private static final double TOLERANCE = 0.001;

    @Test
    @DisplayName("identical names score 1.0")
    void identicalNamesScoreOne() {
        assertEquals(1.0, comparator.compare("lodash", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("one missing letter in a seven-letter name scores about 0.86")
    void oneEditInSevenLetters() {
        // "expres" needs one insertion to become "express"; 1 - (1/7) = 0.857
        assertEquals(0.857, comparator.compare("expres", "express"), TOLERANCE);
    }

    @Test
    @DisplayName("two swapped letters in a six-letter name score about 0.67")
    void twoEditsInSixLetters() {
        // "lodahs" -> "lodash" is two substitutions; 1 - (2/6) = 0.667
        assertEquals(0.667, comparator.compare("lodahs", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("unrelated names score low")
    void unrelatedNamesScoreLow() {
        double score = comparator.compare("lodash", "webpack");
        assertTrue(score < 0.3, "expected a low score but got " + score);
    }

    @Test
    @DisplayName("a real squat scores higher than an unrelated name")
    void squatBeatsUnrelated() {
        double squat = comparator.compare("lodahs", "lodash");
        double unrelated = comparator.compare("webpack", "lodash");
        assertTrue(squat > unrelated,
            "a typosquat must score higher than an unrelated package");
    }

    @Test
    @DisplayName("one edit counts for more in a short name than a long one")
    void lengthIsTakenIntoAccount() {
        double shortName = comparator.compare("ab", "ac");
        double longName = comparator.compare("abcdefghij", "abcdefghix");
        assertTrue(longName > shortName,
            "one edit in a long name should hurt the score less");
    }

    @Test
    @DisplayName("two empty names score 1.0 instead of crashing")
    void handlesTwoEmptyNames() {
        assertEquals(1.0, comparator.compare("", ""), TOLERANCE);
    }

    @Test
    @DisplayName("an empty name against a real one scores 0.0")
    void handlesOneEmptyName() {
        assertEquals(0.0, comparator.compare("", "lodash"), TOLERANCE);
    }
}