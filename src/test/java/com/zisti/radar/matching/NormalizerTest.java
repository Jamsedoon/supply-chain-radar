package com.zisti.radar.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NormalizerTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @DisplayName("normalizes real-world package names")
    @CsvSource({
        // plain names pass through unchanged
        "lodash,           lodash",
        "express,          express",

        // casing
        "LoDaSh,           lodash",
        "TypeScript,       typescript",

        // scopes
        "'@types/node',        node",
        "'@babel/core',        core",
        "'@types/Node-Fetch',  nodefetch",

        // punctuation
        "cross-env,        crossenv",
        "lodash.merge,     lodashmerge",
        "socket.io,        socketio",
        "some_package,     somepackage",

        // digits survive
        "mysql2,           mysql2",
        "base64-js,        base64js",

        // the pairs that motivate this class existing
        "crossenv,         crossenv",
        "Cross-Env,        crossenv"
    })
    void normalizesCorrectly(String raw, String expected) {
        assertEquals(expected, Normalizer.normalize(raw));
    }

    @Test
    @DisplayName("delimiter variants collapse to the same normalized form")
    void delimiterVariantsCollapse() {
        assertEquals(
            Normalizer.normalize("cross-env"),
            Normalizer.normalize("crossenv"),
            "cross-env and crossenv must normalize identically — "
                + "the delimiter comparator depends on this");
    }

    @Test
    @DisplayName("distinct packages do not collapse into each other")
    void distinctNamesStayDistinct() {
        assertEquals("lodash", Normalizer.normalize("lodash"));
        assertEquals("lodashes", Normalizer.normalize("lodashes"));
    }

    @Test
    @DisplayName("a name with no letters or digits becomes an empty string")
    void handlesDegenerateInput() {
        assertEquals("", Normalizer.normalize("---"));
        assertEquals("", Normalizer.normalize("..."));
    }

    @Test
    @DisplayName("null is rejected loudly rather than silently normalized")
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> Normalizer.normalize(null));
    }
}
