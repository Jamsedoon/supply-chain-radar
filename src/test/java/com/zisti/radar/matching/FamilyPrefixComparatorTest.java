package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FamilyPrefixComparatorTest {

    private final FamilyPrefixComparator comparator = new FamilyPrefixComparator();

    private static final double TOLERANCE = 0.001;

    @Test
    @DisplayName("an official companion package scores 1.0")
    void companionPackage() {
        assertEquals(1.0, comparator.compare("webpackcli", "webpack"), TOLERANCE);
    }

    @Test
    @DisplayName("a versioned successor scores 1.0")
    void versionedSuccessor() {
        assertEquals(1.0, comparator.compare("uuidv4", "uuid"), TOLERANCE);
    }

    @Test
    @DisplayName("react-dom against react scores 1.0")
    void reactDom() {
        assertEquals(1.0, comparator.compare("reactdom", "react"), TOLERANCE);
    }

    @Test
    @DisplayName("a transposition is not a family prefix")
    void transpositionScoresZero() {
        assertEquals(0.0, comparator.compare("momnet", "moment"), TOLERANCE);
    }

    @Test
    @DisplayName("a keyboard slip is not a family prefix")
    void keyboardSlipScoresZero() {
        assertEquals(0.0, comparator.compare("lodqsh", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("a one-letter extension is a typo, not a companion package")
    void oneLetterSuffixScoresZero() {
        // mysql2 would be legitimate, but mysq vs mysql must not be excused
        assertEquals(0.0, comparator.compare("mysq", "mysql"), TOLERANCE);
    }

    @Test
    @DisplayName("a very short base name does not count")
    void shortBaseScoresZero() {
        assertEquals(0.0, comparator.compare("d3js", "d3"), TOLERANCE);
    }

    @Test
    @DisplayName("identical names score 0.0")
    void identicalScoresZero() {
        assertEquals(0.0, comparator.compare("lodash", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("a shared prefix is not enough — one must contain the other")
    void sharedPrefixIsNotEnough() {
        // babelcore and babelcli share a prefix, but neither contains the other
        assertEquals(0.0, comparator.compare("babelcore", "babelcli"), TOLERANCE);
    }

    @Test
    @DisplayName("unrelated names score 0.0")
    void unrelatedScoresZero() {
        assertEquals(0.0, comparator.compare("webpack", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("the score is the same in either order")
    void isSymmetric() {
        assertEquals(
            comparator.compare("webpackcli", "webpack"),
            comparator.compare("webpack", "webpackcli"),
            TOLERANCE);
    }

    @Test
    @DisplayName("two empty names score 0.0 instead of crashing")
    void handlesEmptyNames() {
        assertEquals(0.0, comparator.compare("", ""), TOLERANCE);
        assertEquals(0.0, comparator.compare("", "lodash"), TOLERANCE);
    }
}