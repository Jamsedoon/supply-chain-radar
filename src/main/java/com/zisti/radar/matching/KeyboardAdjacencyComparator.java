package com.zisti.radar.matching;

import java.util.HashMap;
import java.util.Map;

/**
 * Scores how plausible a substitution is as a physical typing slip.
 *
 * <p>Edit distance treats every wrong character the same. In practice they are
 * not the same: replacing {@code a} with {@code q} is a one-finger slip on a
 * QWERTY keyboard, while replacing {@code a} with {@code p} is not something a
 * person types by accident. Squatters target the former.
 *
 * <p>Only equal-length names with one or two differing positions are scored.
 * Anything else returns {@code 0.0}, because this comparator is answering a
 * narrow question: "is this a fat-finger substitution of that?" Insertions and
 * deletions are edit distance's job.
 *
 * <p>Identical names score {@code 0.0} — no substitution took place, so there
 * is no evidence of a typing slip. See {@link NameComparator} on why identical
 * input is not required to score {@code 1.0}.
 *
 * <p>Known simplification: the grid ignores the physical stagger of a real
 * keyboard, so a few diagonal pairs are treated as neighbours slightly more
 * generously than a precise model would. QWERTY layout is assumed.
 *
 * <p>Expects normalized names (see {@link Normalizer}).
 */
public final class KeyboardAdjacencyComparator implements NameComparator {

    private static final String[] ROWS = {
        "1234567890",
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    };

    /** Maps each key to its {row, column} position. Built once, never changed. */
    private static final Map<Character, int[]> POSITIONS = buildPositions();

    /** More than two differing positions is no longer a plausible single slip. */
    private static final int MAX_DIFFERENCES = 2;

    private static Map<Character, int[]> buildPositions() {
        Map<Character, int[]> map = new HashMap<>();
        for (int row = 0; row < ROWS.length; row++) {
            for (int col = 0; col < ROWS[row].length(); col++) {
                map.put(ROWS[row].charAt(col), new int[] {row, col});
            }
        }
        return map;
    }

    @Override
    public String name() {
        return "keyboard_adjacency";
    }

    @Override
    public double compare(String candidate, String reference) {
        // Only same-length names can be pure substitutions.
        if (candidate.length() != reference.length()) {
            return 0.0;
        }

        int differences = 0;
        int neighbourSwaps = 0;

        for (int i = 0; i < candidate.length(); i++) {
            char a = candidate.charAt(i);
            char b = reference.charAt(i);

            if (a == b) {
                continue;
            }

            differences++;
            if (differences > MAX_DIFFERENCES) {
                return 0.0;
            }
            if (areNeighbours(a, b)) {
                neighbourSwaps++;
            }
        }

        // No differences means no substitution to judge.
        if (differences == 0) {
            return 0.0;
        }

        return (double) neighbourSwaps / differences;
    }

    /** True when two keys sit next to each other on the grid, including diagonally. */
    private static boolean areNeighbours(char a, char b) {
        int[] posA = POSITIONS.get(a);
        int[] posB = POSITIONS.get(b);

        if (posA == null || posB == null) {
            return false;
        }

        int rowGap = Math.abs(posA[0] - posB[0]);
        int colGap = Math.abs(posA[1] - posB[1]);

        return rowGap <= 1 && colGap <= 1;
    }
}
