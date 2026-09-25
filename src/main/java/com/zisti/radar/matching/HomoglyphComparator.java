package com.zisti.radar.matching;

/**
 * Detects names that are visually identical but textually different.
 *
 * <p>A homoglyph is a character that looks like another character without being
 * it: the digit {@code 1} against the letter {@code l}, the digit {@code 0}
 * against the letter {@code o}, the pair {@code rn} against {@code m}. A reader
 * scanning a dependency list sees the shape, not the code point.
 *
 * <p>This is a different attack from a typo, and the earlier comparators do not
 * catch it. Keyboard adjacency finds nothing, because {@code 1} and {@code l}
 * are not neighbouring keys. Edit distance sees one changed character and
 * returns a middling score, the same as it would for any other substitution.
 * Neither notices that the two names are indistinguishable on screen.
 *
 * <p>Both names are reduced to a visual form in which every character is
 * replaced by the representative of its look-alike family. Identical results
 * mean the names are visually the same.
 *
 * <p>This comparator is deliberately all-or-nothing. A name either reads as
 * another name or it does not; there is no useful middle ground.
 *
 * <p>Known simplification: only Latin-script look-alikes are handled. Real
 * attacks also use Cyrillic and Greek characters that render almost identically
 * to Latin ones. npm restricts package names to a limited character set, which
 * blunts that route, but the gap is real.
 *
 * <p>Expects normalized names (see {@link Normalizer}).
 */
public final class HomoglyphComparator implements NameComparator {

    @Override
    public String name() {
        return "homoglyph";
    }

    @Override
    public double compare(String candidate, String reference) {
        // Identical text is not a homoglyph attack; there is nothing disguised.
        if (candidate.equals(reference)) {
            return 0.0;
        }

        return toVisualForm(candidate).equals(toVisualForm(reference)) ? 1.0 : 0.0;
    }

    /**
     * Reduces a name to its visual shape.
     *
     * <p>Multi-character substitutions run first, so that {@code rn} collapses to
     * {@code m} before any single-character rule can interfere with those letters.
     */
    private static String toVisualForm(String name) {
        return name
            .replace("rn", "m")
            .replace("vv", "w")
            .replace("cl", "d")
            .replace('0', 'o')
            .replace('1', 'l')
            .replace('i', 'l')
            .replace('5', 's')
            .replace('2', 'z')
            .replace('8', 'b');
    }
}