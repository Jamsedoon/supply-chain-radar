package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HomoglyphComparatorTest {

    private final HomoglyphComparator comparator = new HomoglyphComparator();

    private static final double TOLERANCE = 0.001;

    @Test
    @DisplayName("digit one standing in for letter L scores 1.0")
    void digitOneForLetterL() {
        assertEquals(1.0, comparator.compare("1odash", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("digit zero standing in for letter O scores 1.0")
    void digitZeroForLetterO() {
        assertEquals(1.0, comparator.compare("l0dash", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("rn standing in for m scores 1.0")
    void rnForM() {
        assertEquals(1.0, comparator.compare("rnysql", "mysql"), TOLERANCE);
    }

    @Test
    @DisplayName("digit five standing in for letter S scores 1.0")
    void digitFiveForLetterS() {
        assertEquals(1.0, comparator.compare("loda5h", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("two substitutions at once still score 1.0")
    void multipleSubstitutions() {
        assertEquals(1.0, comparator.compare("10dash", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("identical names score 0.0 because nothing was disguised")
    void identicalNamesScoreZero() {
        assertEquals(0.0, comparator.compare("lodash", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("a plain typo is not a homoglyph")
    void plainTypoScoresZero() {
        // 'q' does not look like 'a'; it is a keyboard slip, not a disguise
        assertEquals(0.0, comparator.compare("lodqsh", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("unrelated names score 0.0")
    void unrelatedNamesScoreZero() {
        assertEquals(0.0, comparator.compare("webpack", "lodash"), TOLERANCE);
    }

    @Test
    @DisplayName("legitimate similar packages score 0.0")
    void legitimateSiblingScoresZero() {
        assertEquals(0.0, comparator.compare("reactdom", "react"), TOLERANCE);
    }

    @Test
    @DisplayName("the score is the same in either order")
    void isSymmetric() {
        assertEquals(
            comparator.compare("1odash", "lodash"),
            comparator.compare("lodash", "1odash"),
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