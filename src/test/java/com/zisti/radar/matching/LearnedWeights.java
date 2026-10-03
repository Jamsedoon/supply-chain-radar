package com.zisti.radar.matching;

import java.util.List;
import java.util.Properties;

/**
 * A {@link MatchWeights} backed by estimates rather than the shipped file.
 *
 * <p>Lets the engine be run with learned parameters without overwriting the
 * configured ones, so both can be measured against the same truth set in a
 * single pass.
 */
public final class LearnedWeights {

    private LearnedWeights() {
        throw new AssertionError("LearnedWeights is a utility class");
    }

    /**
     * Builds a weights object from estimates.
     *
     * <p>Bucket boundaries and the threshold are carried over from the shipped
     * configuration, so the only thing that changes is the m and u values. That
     * keeps the comparison honest: two runs differing in one variable.
     */
    public static MatchWeights from(List<WeightEstimator.Estimate> estimates,
                                    double strongBoundary,
                                    double weakBoundary,
                                    double threshold) {
        Properties properties = new Properties();

        properties.setProperty("level.strong", String.valueOf(strongBoundary));
        properties.setProperty("level.weak", String.valueOf(weakBoundary));
        properties.setProperty("threshold", String.valueOf(threshold));

        for (WeightEstimator.Estimate estimate : estimates) {
            String prefix = estimate.comparator() + "." + estimate.level();
            properties.setProperty(prefix + ".m", String.valueOf(estimate.m()));
            properties.setProperty(prefix + ".u", String.valueOf(estimate.u()));
        }

        return MatchWeights.fromProperties(properties);
    }
}