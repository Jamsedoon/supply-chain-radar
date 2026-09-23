package com.zisti.radar.matching;

/**
 * One way of measuring how similar two package names are.
 *
 * <p>Each implementation targets a different impersonation trick: a mistyped
 * character, a swapped delimiter, a look-alike letter. The matching engine runs
 * all of them and combines the results, so a single comparator doesn't need to
 * be right on its own — it needs to add independent information.
 *
 * <p>Implementations must not store changing state, so they are safe to call
 * from several threads at once. The scorer processes queued packages in parallel.
 */
public interface NameComparator {

    /**
     * A fixed identifier for this comparator.
     *
     * <p>Used as the key in {@code matching-weights.yml} and shown on the
     * dashboard's evidence panel. Changing it breaks the configured weights,
     * so treat it as permanent once chosen.
     *
     * @return a lowercase snake_case name, e.g. {@code "edit_distance"}
     */
    String name();

    /**
     * Scores how similar two names are.
     *
     * <p>Higher means more alike. The result must be the same whichever order
     * the names are given in, and must be {@code 1.0} for identical input.
     *
     * <p>Most implementations expect normalized names (see {@link Normalizer}).
     * A comparator that needs the raw name must say so in its own documentation.
     *
     * @param candidate the name under suspicion
     * @param reference the known-good name it's being compared against
     * @return a value from {@code 0.0} to {@code 1.0}, inclusive
     */
    double compare(String candidate, String reference);
}