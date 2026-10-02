package com.zisti.radar.matching;

/**
 * Detects that one name is the other plus a suffix.
 *
 * <p>Unlike the other comparators, this one is evidence <em>against</em> a
 * typosquat. Extending a popular name is how the npm ecosystem labels official
 * companion packages — {@code react-dom}, {@code webpack-cli},
 * {@code moment-timezone}, {@code jest-cli}. Squatters rarely do it, because an
 * extended name is not something a developer types by mistake.
 *
 * <p>In Fellegi-Sunter terms this clue has a low agreement rate for genuine
 * matches and a high one for non-matches, so {@code log2(m / u)} comes out
 * negative. No special handling is required: the model already expresses "this
 * clue argues against a match" as a negative weight.
 *
 * <p>Added in SC-05c after measurement showed that Jaro-Winkler and edit
 * distance produced identical evidence for {@code momnet}/{@code moment} (a real
 * squat) and {@code webpack-cli}/{@code webpack} (a legitimate companion). No
 * weighting of those two clues can separate cases that generate the same
 * evidence; a clue that fires on one and not the other can.
 *
 * <p>Equal normalized names score {@code 0.0}. That case is a delimiter variant,
 * which is a different comparator's job, and treating it as a family prefix
 * would cancel out a genuine attack signal.
 *
 * <p>Expects normalized names (see {@link Normalizer}).
 */
public final class FamilyPrefixComparator implements NameComparator {

    /**
     * The shorter name must be at least this long.
     *
     * <p>Almost anything starts with a two-character string, so without a floor
     * this clue would fire constantly on coincidence rather than on the naming
     * convention it is meant to detect.
     */
    private static final int MIN_BASE_LENGTH = 3;

    /**
     * The suffix must be at least this long.
     *
     * <p>A one-character extension is a typo, not a companion package:
     * {@code mysq} against {@code mysql} should not be excused.
     */
    private static final int MIN_SUFFIX_LENGTH = 2;

    @Override
    public String name() {
        return "family_prefix";
    }

    @Override
    public double compare(String candidate, String reference) {
        if (candidate.equals(reference)) {
            return 0.0;
        }

        String shorter = candidate.length() <= reference.length() ? candidate : reference;
        String longer = candidate.length() <= reference.length() ? reference : candidate;

        if (shorter.length() < MIN_BASE_LENGTH) {
            return 0.0;
        }

        if (longer.length() - shorter.length() < MIN_SUFFIX_LENGTH) {
            return 0.0;
        }

        return longer.startsWith(shorter) ? 1.0 : 0.0;
    }
}