package com.zisti.radar.matching;

import java.util.List;

/**
 * The verdict on one candidate-and-reference pair.
 *
 * @param candidate   the name under suspicion, as published
 * @param reference   the known-good name it was compared against
 * @param totalWeight the summed evidence in bits; higher means more suspicious
 * @param threshold   the cutoff in force when this decision was made
 * @param isMatch     whether {@code totalWeight} reached {@code threshold}
 * @param evidence    one entry per comparator, in the order they ran
 */
public record MatchResult(
    String candidate,
    String reference,
    double totalWeight,
    double threshold,
    boolean isMatch,
    List<ClueEvidence> evidence) {

    /** Defensive copy, so the evidence list cannot be altered after the fact. */
    public MatchResult {
        evidence = List.copyOf(evidence);
    }
}