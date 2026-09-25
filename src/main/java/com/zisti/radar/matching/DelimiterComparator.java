package com.zisti.radar.matching;

/**
 * Detects names that differ only in punctuation.
 *
 * <p>Registering {@code crossenv} against the real {@code cross-env} was a
 * genuine attack; the fake was installed several hundred times before removal.
 * The two names read as the same package to anyone skimming a dependency list.
 *
 * <p><strong>This comparator takes raw, un-normalized names.</strong> It is the
 * only one that does, and it must. Normalization strips punctuation, so by the
 * time the other comparators see these two names they are already the same
 * string — the very difference being detected has been erased. See
 * {@link NameComparator} on comparators that require the raw form.
 *
 * <p>The rule is: different as written, identical once normalized. That is
 * exactly the definition of a delimiter variant, which is why this rests on the
 * guarantee tested by {@code NormalizerTest.delimiterVariantsCollapse}.
 *
 * <p>Like the homoglyph comparator, this is all-or-nothing: a name either is a
 * punctuation variant of another or it is not.
 */
public final class DelimiterComparator implements NameComparator {

    @Override
    public String name() {
        return "delimiter";
    }

    /**
     * {@inheritDoc}
     *
     * @param candidate the raw candidate name, punctuation intact
     * @param reference the raw reference name, punctuation intact
     */
    @Override
    public double compare(String candidate, String reference) {
        // Identical as written means no delimiter trick was played.
        if (candidate.equals(reference)) {
            return 0.0;
        }

        String normalizedCandidate = Normalizer.normalize(candidate);
        String normalizedReference = Normalizer.normalize(reference);

        // A name with no letters or digits normalizes to empty; two such names
        // would collapse together and produce a meaningless match.
        if (normalizedCandidate.isEmpty()) {
            return 0.0;
        }

        return normalizedCandidate.equals(normalizedReference) ? 1.0 : 0.0;
    }
}
