package com.zisti.radar.matching;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;

/**
 * Scores similarity using Jaro-Winkler, which rewards a shared prefix.
 *
 * <p>Jaro similarity counts matching characters, allowing them to be slightly
 * out of position, and penalises transpositions. Winkler's addition boosts the
 * score further when the first characters agree.
 *
 * <p>That prefix bonus is the reason this comparator earns its place alongside
 * edit distance. Readers skim the start of a name and infer the rest, so a
 * squat that matches the opening letters is far more effective than one that
 * does not. Edit distance treats every position equally; this one does not.
 *
 * <p>Expects normalized names (see {@link Normalizer}).
 */
public final class JaroWinklerComparator implements NameComparator {

    /** Shared and immutable, so it is safe to use from several threads. */
    private static final JaroWinklerSimilarity SIMILARITY = new JaroWinklerSimilarity();

    @Override
    public String name() {
        return "jaro_winkler";
    }

    @Override
    public double compare(String candidate, String reference) {
        // Handle empties ourselves so behaviour is defined and predictable.
        if (candidate.isEmpty() && reference.isEmpty()) {
            return 1.0;
        }
        if (candidate.isEmpty() || reference.isEmpty()) {
            return 0.0;
        }

        double score = SIMILARITY.apply(candidate, reference);

        // Guard the contract: clamp into [0.0, 1.0] regardless of library edge cases.
        return Math.max(0.0, Math.min(1.0, score));
    }
}