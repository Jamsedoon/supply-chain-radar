package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DelimiterComparatorTest {

    private final DelimiterComparator comparator = new DelimiterComparator();

    private static final double TOLERANCE = 0.001;

    @Test
    @DisplayName("the real cross-env attack scores 1.0")
    void crossEnvAttack() {
        assertEquals(1.0, comparator.compare("crossenv", "cross-env"), TOLERANCE);
    }

    @Test
    @DisplayName("a missing hyphen scores 1.0")
    void missingHyphen() {
        assertEquals(1.0, comparator.compare("nodefetch", "node-fetch"), TOLERANCE);
    }

    @Test
    @DisplayName("an underscore swapped for a hyphen scores 1.0")
    void underscoreForHyphen() {
        assertEquals(1.0, comparator.compare("node_fetch", "node-fetch"), TOLERANCE);
    }

    @Test
    @DisplayName("a dot swapped for a hyphen scores 1.0")
    void dotForHyphen() {
        assertEquals(1.0, comparator.compare("socket.io", "socket-io"), TOLERANCE);
    }

    @Test
    @DisplayName("a case-only difference scores 1.0")
    void caseOnlyDifference() {
        assertEquals(1.0, comparator.compare("Cross-Env", "cross-env"), TOLERANCE);
    }

    @Test
    @DisplayName("identical names score 0.0 because no trick was played")
    void identicalNamesScoreZero() {
        assertEquals(0.0, comparator.compare("cross-env", "cross-env"), TOLERANCE);
    }

    @Test
    @DisplayName("a real letter difference is not a delimiter variant")
    void letterDifferenceScoresZero() {
        assertEquals(0.0, comparator.compare("cross-envs", "cross-env"), TOLERANCE);
    }

    @Test
    @DisplayName("legitimate sibling packages score 0.0")
    void legitimateSiblingScoresZero() {
        assertEquals(0.0, comparator.compare("react-dom", "react-router"), TOLERANCE);
    }

    @Test
    @DisplayName("unrelated names score 0.0")
    void unrelatedNamesScoreZero() {
        assertEquals(0.0, comparator.compare("webpack", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("names with no letters or digits do not collapse together")
    void punctuationOnlyNamesScoreZero() {
        assertEquals(0.0, comparator.compare("---", "..."), TOLERANCE);
    }

    @Test
    @DisplayName("the score is the same in either order")
    void isSymmetric() {
        assertEquals(
            comparator.compare("crossenv", "cross-env"),
            comparator.compare("cross-env", "crossenv"),
            TOLERANCE);
    }

    @Test
    @DisplayName("two empty names score 0.0 instead of crashing")
    void handlesTwoEmptyNames() {
        assertEquals(0.0, comparator.compare("", ""), TOLERANCE);
    }

    @Test
    @DisplayName("an empty name against a real one scores 0.0")
    void handlesOneEmptyName() {
        assertEquals(0.0, comparator.compare("", "lodash"), TOLERANCE);
    }
}