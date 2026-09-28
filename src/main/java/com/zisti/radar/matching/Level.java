package com.zisti.radar.matching;

/**
 * How strongly a single comparator agreed.
 *
 * <p>Comparators return a decimal, but Fellegi-Sunter needs a stated agreement
 * and disagreement rate for each possible outcome. Three buckets keeps the
 * number of configured parameters manageable while preserving the distinction
 * that matters: decisive agreement, partial agreement, and none.
 */
public enum Level {

    /** Decisive agreement. */
    STRONG,

    /** Partial agreement — some signal, but not conclusive. */
    WEAK,

    /** No meaningful agreement. Contributes no evidence. */
    NONE
}