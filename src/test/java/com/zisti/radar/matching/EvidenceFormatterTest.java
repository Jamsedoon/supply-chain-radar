package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvidenceFormatterTest {

    private final MatchEngine engine = MatchEngine.withDefaults();

    @Test
    @DisplayName("the full report names both packages and every comparator")
    void fullReportIsComplete() {
        String report = EvidenceFormatter.format(engine.score("lodqsh", "lodash"));

        assertTrue(report.contains("lodqsh"), "must name the candidate");
        assertTrue(report.contains("lodash"), "must name the reference");
        assertTrue(report.contains("edit_distance"));
        assertTrue(report.contains("jaro_winkler"));
        assertTrue(report.contains("keyboard_adjacency"));
        assertTrue(report.contains("homoglyph"));
        assertTrue(report.contains("delimiter"));
        assertTrue(report.contains("total evidence"));
        assertTrue(report.contains("threshold"));
    }

    @Test
    @DisplayName("the compact form fits on one line")
    void compactFormIsOneLine() {
        String line = EvidenceFormatter.formatCompact(engine.score("lodqsh", "lodash"));

        assertTrue(line.lines().count() == 1, "compact form must be a single line");
        assertTrue(line.contains("lodqsh"));
    }

    @Test
    @DisplayName("prints two real decisions for eyeballing")
    void printSamples() {
        System.out.println(EvidenceFormatter.format(engine.score("lodqsh", "lodash")));
        System.out.println(EvidenceFormatter.format(engine.score("react-dom", "react")));
    }
}