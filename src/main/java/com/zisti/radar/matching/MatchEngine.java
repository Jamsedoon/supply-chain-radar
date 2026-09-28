package com.zisti.radar.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Combines every comparator's opinion into one verdict.
 *
 * <p>Implements the Fellegi-Sunter model of probabilistic record linkage. Each
 * comparator's raw score is sorted into a level, each level carries a weight in
 * bits derived from how often that comparator agrees for a real match versus by
 * chance, and the weights are summed. A total at or above the threshold is an
 * alert.
 *
 * <p>Averaging the comparator scores instead would give a clue that fires
 * constantly the same say as one that almost never fires by accident. The point
 * of the model is that those two clues are not worth the same.
 *
 * <p>Stateless once constructed, and therefore safe to share across threads.
 */
public final class MatchEngine {

    /**
     * Comparators that need the raw, un-normalized names.
     *
     * <p>Only {@code delimiter} qualifies: normalization strips punctuation,
     * which is the exact difference it exists to detect. See the design note in
     * {@code docs/data-notes.md} on why this is a name check rather than a
     * method on the interface.
     */
    private static final Set<String> RAW_INPUT_COMPARATORS = Set.of("delimiter");

    private final List<NameComparator> comparators;
    private final MatchWeights weights;

    public MatchEngine(List<NameComparator> comparators, MatchWeights weights) {
        if (comparators.isEmpty()) {
            throw new IllegalArgumentException("at least one comparator is required");
        }
        this.comparators = List.copyOf(comparators);
        this.weights = weights;
    }

    /** Builds an engine with all five comparators and the shipped weights. */
    public static MatchEngine withDefaults() {
        return new MatchEngine(
            List.of(
                new EditDistanceComparator(),
                new JaroWinklerComparator(),
                new KeyboardAdjacencyComparator(),
                new HomoglyphComparator(),
                new DelimiterComparator()),
            MatchWeights.loadDefault());
    }

    /**
     * Scores one candidate against one reference package.
     *
     * @param rawCandidate the name under suspicion, exactly as published
     * @param rawReference the known-good name, exactly as published
     * @return the verdict, with a full per-comparator breakdown
     */
    public MatchResult score(String rawCandidate, String rawReference) {
        String normalizedCandidate = Normalizer.normalize(rawCandidate);
        String normalizedReference = Normalizer.normalize(rawReference);

        List<ClueEvidence> evidence = new ArrayList<>(comparators.size());
        double totalWeight = 0.0;

        for (NameComparator comparator : comparators) {
            boolean wantsRaw = RAW_INPUT_COMPARATORS.contains(comparator.name());

            double rawScore = wantsRaw
                ? comparator.compare(rawCandidate, rawReference)
                : comparator.compare(normalizedCandidate, normalizedReference);

            Level level = weights.bucket(rawScore);
            double weight = weights.weightFor(comparator.name(), level);

            totalWeight += weight;
            evidence.add(new ClueEvidence(comparator.name(), level, rawScore, weight));
        }

        double threshold = weights.threshold();

        return new MatchResult(
            rawCandidate,
            rawReference,
            totalWeight,
            threshold,
            totalWeight >= threshold,
            evidence);
    }
}