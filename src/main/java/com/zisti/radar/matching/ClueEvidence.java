package com.zisti.radar.matching;

/**
 * What one comparator contributed to a scoring decision.
 *
 * <p>Stored alongside every alert so the dashboard can show why the alert
 * fired. An alert with a number and no explanation is something a reviewer has
 * to take on faith; an alert with its evidence is something they can check.
 *
 * @param comparator the comparator's stable name, e.g. {@code "edit_distance"}
 * @param level      which bucket its raw score fell into
 * @param rawScore   the undivided score the comparator returned, kept for tuning
 * @param weight     the evidence this contributed, in bits
 */
public record ClueEvidence(
    String comparator,
    Level level,
    double rawScore,
    double weight) {
}