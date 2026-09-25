package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JaroWinklerComparatorTest {

    private final JaroWinklerComparator comparator = new JaroWinklerComparator();

    private static final double TOLERANCE = 0.001;

    @Test
    @DisplayName("identical names score 1.0")
    void identicalNamesScoreOne() {
        assertEquals(1.0, comparator.compare("lodash", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("a near-miss scores high")
    void nearMissScoresHigh() {
        double score = comparator.compare("lodahs", "lodash");
        assertTrue(score > 0.9, "expected above 0.9 but got " + score);
    }

    @Test
    @DisplayName("a wrong letter at the START hurts more than at the end")
    void prefixMattersMoreThanSuffix() {
        double wrongEnd = comparator.compare("lodasx", "lodash");
        double wrongStart = comparator.compare("xodash", "lodash");

        assertTrue(wrongEnd > wrongStart,
            "Jaro-Winkler should punish a wrong first letter harder. "
                + "wrongEnd=" + wrongEnd + " wrongStart=" + wrongStart);
    }

    @Test
    @DisplayName("an added letter at the end still scores very high")
    void trailingAdditionScoresVeryHigh() {
        double score = comparator.compare("lodashh", "lodash");
        assertTrue(score > 0.95, "expected above 0.95 but got " + score);
    }

    @Test
    @DisplayName("unrelated names score low")
    void unrelatedNamesScoreLow() {
        double score = comparator.compare("webpack", "lodash");
        assertTrue(score < 0.6, "expected below 0.6 but got " + score);
    }

    @Test
    @DisplayName("the score is the same in either order")
    void isSymmetric() {
        assertEquals(
            comparator.compare("lodahs", "lodash"),
            comparator.compare("lodash", "lodahs"),
            TOLERANCE);
    }

    @Test
    @DisplayName("two empty names score 1.0")
    void handlesTwoEmptyNames() {
        assertEquals(1.0, comparator.compare("", ""), TOLERANCE);
    }

    @Test
    @DisplayName("an empty name against a real one scores 0.0")
    void handlesOneEmptyName() {
        assertEquals(0.0, comparator.compare("", "lodash"), TOLERANCE);
    }
}