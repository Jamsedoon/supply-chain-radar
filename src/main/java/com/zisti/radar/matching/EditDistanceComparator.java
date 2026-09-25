package com.zisti.radar.matching;
import org.apache.commons.text.similarity.LevenshteinDistance;
/**
 * Scores similarity by counting single-character edits.
 *
 * <p>Levenshtein distance is the smallest number of insertions, deletions, or
 * substitutions needed to turn one string into the other. The raw count is
 * divided by the length of the longer name so that one edit in a short name
 * counts for more than one edit in a long name, then inverted so that a higher
 * score means more similar.
 *
 * <p>This is the workhorse comparator. It catches the plain mistyped name —
 * a dropped letter, an extra letter, a wrong letter — which covers most
 * typosquats.
 *
 * <p>Expects normalized names (see {@link Normalizer}).
 */
public final class EditDistanceComparator implements NameComparator{

    /** Shared and immutable, so it is safe to use from several threads. */
    private static final LevenshteinDistance DISTANCE = LevenshteinDistance.getDefaultInstance();
    
    @Override
    public String name() {
        return "edit_distance";
    }

    @Override
    public double compare(String candidate, String reference) {
        int longer = Math.max(candidate.length(), reference.length());

        if (longer == 0) {
            return 1.0;
        }
        // Two empty names are trivially identical; also avoids dividing by zero.
        int edits = DISTANCE.apply(candidate, reference);
        return 1.0 - ((double) edits / longer);
    }
}
