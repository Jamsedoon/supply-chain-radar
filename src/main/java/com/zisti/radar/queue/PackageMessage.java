package com.zisti.radar.queue;

import java.time.Instant;

/**
 * One package observed on the registry, queued for scoring.
 *
 * <p>Carries everything the scorer needs. The scorer never calls the npm API —
 * if it did, a registry outage would stop scoring as well as collection, and
 * the two failures would be indistinguishable.
 *
 * @param name           the package name exactly as published
 * @param weeklyDownloads downloads in the last week, 0 for a brand-new package
 * @param publisher      the npm account that published it, may be null
 * @param repositoryUrl  the declared source repository, may be null
 * @param publishedAt    when this version was published, may be null
 */
public record PackageMessage(
    String name,
    long weeklyDownloads,
    String publisher,
    String repositoryUrl,
    Instant publishedAt) {

    public PackageMessage {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("package name is required");
        }
        if (weeklyDownloads < 0) {
            throw new IllegalArgumentException(
                "weeklyDownloads cannot be negative: " + weeklyDownloads);
        }
    }
}