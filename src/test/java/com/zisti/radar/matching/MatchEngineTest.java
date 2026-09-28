package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MatchEngineTest {

    private final MatchEngine engine = MatchEngine.withDefaults();

    @Nested
    @DisplayName("real typosquats")
    class RealSquats {

        @Test
        @DisplayName("the cross-env attack is flagged")
        void crossEnv() {
            MatchResult result = engine.score("crossenv", "cross-env");
            assertTrue(result.isMatch(),
                "expected a match; total was " + result.totalWeight());
        }

        @Test
        @DisplayName("a transposed pair of letters is flagged")
        void transposedLetters() {
            MatchResult result = engine.score("lodahs", "lodash");
            assertTrue(result.isMatch(),
                "expected a match; total was " + result.totalWeight());
        }

        @Test
        @DisplayName("a keyboard slip is flagged")
        void keyboardSlip() {
            MatchResult result = engine.score("lodqsh", "lodash");
            assertTrue(result.isMatch(),
                "expected a match; total was " + result.totalWeight());
        }

        @Test
        @DisplayName("a homoglyph substitution is flagged")
        void homoglyph() {
            MatchResult result = engine.score("1odash", "lodash");
            assertTrue(result.isMatch(),
                "expected a match; total was " + result.totalWeight());
        }

        @Test
        @DisplayName("a dropped letter is flagged")
        void droppedLetter() {
            MatchResult result = engine.score("lodas", "lodash");
            assertTrue(result.isMatch(),
                "expected a match; total was " + result.totalWeight());
        }
    }

    @Nested
    @DisplayName("legitimate packages that must not be flagged")
    class LegitimatePackages {

        @Test
        @DisplayName("react-dom is not a squat of react")
        void reactDom() {
            MatchResult result = engine.score("react-dom", "react");
            assertFalse(result.isMatch(),
                "false positive; total was " + result.totalWeight());
        }

        @Test
        @DisplayName("lodash.merge is not a squat of lodash")
        void lodashMerge() {
            MatchResult result = engine.score("lodash.merge", "lodash");
            assertFalse(result.isMatch(),
                "false positive; total was " + result.totalWeight());
        }

        @Test
        @DisplayName("express-session is not a squat of express")
        void expressSession() {
            MatchResult result = engine.score("express-session", "express");
            assertFalse(result.isMatch(),
                "false positive; total was " + result.totalWeight());
        }

        @Test
        @DisplayName("two unrelated packages are not flagged")
        void unrelated() {
            MatchResult result = engine.score("webpack", "lodash");
            assertFalse(result.isMatch(),
                "false positive; total was " + result.totalWeight());
        }

        @Test
        @DisplayName("debounce is not a squat of debug")
        void debounce() {
            MatchResult result = engine.score("debounce", "debug");
            assertFalse(result.isMatch(),
                "false positive; total was " + result.totalWeight());
        }
    }

    @Nested
    @DisplayName("the result structure")
    class ResultStructure {

        @Test
        @DisplayName("every comparator appears in the evidence, even when silent")
        void everyComparatorReports() {
            MatchResult result = engine.score("webpack", "lodash");
            assertEquals(5, result.evidence().size(),
                "all five comparators must report, so the explanation has no gaps");
        }

        @Test
        @DisplayName("the total is the sum of the individual weights")
        void totalIsTheSum() {
            MatchResult result = engine.score("lodqsh", "lodash");

            double sum = result.evidence().stream()
                .mapToDouble(ClueEvidence::weight)
                .sum();

            assertEquals(sum, result.totalWeight(), 0.0001);
        }

        @Test
        @DisplayName("the threshold in force is recorded on the result")
        void thresholdIsRecorded() {
            MatchResult result = engine.score("lodash", "lodash");
            assertTrue(result.threshold() > 0, "a threshold must be recorded");
        }

        @Test
        @DisplayName("the names are kept in their original form")
        void namesAreNotNormalized() {
            MatchResult result = engine.score("Cross-Env", "cross-env");
            assertEquals("Cross-Env", result.candidate());
            assertEquals("cross-env", result.reference());
        }
    }

    @Nested
    @DisplayName("clue weighting")
    class ClueWeighting {

        @Test
        @DisplayName("a keyboard slip outweighs an equivalent non-keyboard change")
        void keyboardSlipCarriesMoreEvidence() {
            // Both are exactly one letter different from lodash.
            double slip = engine.score("lodqsh", "lodash").totalWeight();
            double notASlip = engine.score("lodpsh", "lodash").totalWeight();

            assertTrue(slip > notASlip,
                "a physically plausible typo must carry more evidence. "
                    + "slip=" + slip + " notASlip=" + notASlip);
        }

        @Test
        @DisplayName("an identical name carries no evidence of impersonation")
        void identicalNameIsNotASquat() {
            MatchResult result = engine.score("lodash", "lodash");

            assertFalse(
                result.evidence().stream()
                    .anyMatch(clue -> clue.comparator().equals("delimiter")
                        && clue.weight() > 0),
                "a package cannot be a delimiter variant of itself");
        }
    }
}