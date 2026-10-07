package com.zisti.radar.collector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zisti.radar.queue.PackageMessage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Exercises the real npm API.
 *
 * <p>Tagged {@code external} because it needs network access and depends on a
 * third-party service. Excluded from the normal build so a registry outage or a
 * flight with no wifi does not fail an unrelated commit.
 *
 * <p>Run deliberately:
 * {@code ./gradlew test --tests "...NpmRegistryClientTest" -PwithExternal}
 */
@Tag("external")
class NpmRegistryClientTest {

    private final NpmRegistryClient client = new NpmRegistryClient();

    @Test
    @DisplayName("fetches a known package's download count")
    void fetchesDownloadsForKnownPackage() {
        long downloads = client.fetchWeeklyDownloads("lodash");

        assertTrue(downloads > 1_000_000,
            "lodash should have millions of weekly downloads, got " + downloads);
    }

    @Test
    @DisplayName("a package that does not exist returns zero, not an error")
    void unknownPackageReturnsZero() {
        long downloads = client.fetchWeeklyDownloads(
            "this-package-definitely-does-not-exist-xyzzy-42");

        assertTrue(downloads == 0,
            "an unknown package must return 0 rather than throwing");
    }

    @Test
    @DisplayName("fetches a batch of recent packages with usable metadata")
    void fetchesRecentPackages() {
        List<PackageMessage> packages = client.fetchRecent(5);

        assertFalse(packages.isEmpty(), "expected at least one package");
        assertTrue(packages.size() <= 5, "should respect the requested limit");

        for (PackageMessage pkg : packages) {
            assertNotNull(pkg.name(), "every package needs a name");
            assertFalse(pkg.name().isBlank());
            assertTrue(pkg.weeklyDownloads() >= 0);
        }

        System.out.println();
        System.out.println("  fetched from npm:");
        for (PackageMessage pkg : packages) {
            System.out.printf("    %-40s %,12d downloads/week repo=%s%n",
                pkg.name(), pkg.weeklyDownloads(),
                pkg.repositoryUrl() == null ? "(none)" : "yes");
        }
        System.out.println();
    }
}