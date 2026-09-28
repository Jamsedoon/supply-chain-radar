package com.zisti.radar.matching;

/**
 * Renders a {@link MatchResult} as a readable table.
 *
 * <p>Used when tuning weights and when investigating a failing case. A total
 * weight on its own says a decision was made; the breakdown says why, which is
 * the only form in which the decision can be argued with.
 */
public final class EvidenceFormatter {

    private EvidenceFormatter() {
        throw new AssertionError("EvidenceFormatter is a utility class");
    }

    /** Formats a full scoring decision, one line per comparator. */
    public static String format(MatchResult result) {
        StringBuilder out = new StringBuilder();

        out.append(String.format("%s  vs  %s%n", result.candidate(), result.reference()));
        out.append("-".repeat(62)).append(System.lineSeparator());
        out.append(String.format("  %-22s %-8s %8s %10s%n",
            "comparator", "level", "raw", "bits"));

        for (ClueEvidence clue : result.evidence()) {
            out.append(String.format("  %-22s %-8s %8.3f %10.2f%n",
                clue.comparator(),
                clue.level(),
                clue.rawScore(),
                clue.weight()));
        }

        out.append("-".repeat(62)).append(System.lineSeparator());
        out.append(String.format("  %-22s %27.2f%n", "total evidence", result.totalWeight()));
        out.append(String.format("  %-22s %27.2f%n", "threshold", result.threshold()));
        out.append(String.format("  %-22s %27s%n",
            "verdict", result.isMatch() ? "SQUAT" : "no alert"));

        return out.toString();
    }

    /** One-line form, for scanning many results at once. */
    public static String formatCompact(MatchResult result) {
        return String.format("%-20s vs %-20s  %7.2f bits  %s",
            result.candidate(),
            result.reference(),
            result.totalWeight(),
            result.isMatch() ? "SQUAT" : "-");
    }
}