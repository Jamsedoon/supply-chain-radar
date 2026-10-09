package com.zisti.radar.scorer;

import com.zisti.radar.matching.MatchEngine;
import com.zisti.radar.matching.MatchResult;
import com.zisti.radar.matching.Normalizer;
import com.zisti.radar.queue.PackageMessage;
import com.zisti.radar.queue.QueueClient;
import com.zisti.radar.queue.QueueClient.ReceivedMessage;
import com.zisti.radar.storage.AlertRepository;
import com.zisti.radar.storage.CandidateFinder;
import com.zisti.radar.storage.CandidateFinder.Candidate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Drains the queue and decides which packages are impersonating popular ones.
 *
 * <p>Two stages, deliberately kept separate:
 * <ol>
 *   <li>Name matching: blocking in Postgres, then the six comparators. Answers
 *       "do these names look alike?"</li>
 *   <li>Popularity check: is the candidate obscure next to the package it
 *       resembles? Answers a question the names cannot.</li>
 * </ol>
 *
 * <p>The second stage resolves the cases the name engine measurably cannot:
 * {@code webpack-cli}/{@code webpack} (legitimate) and {@code jquery-min}/{@code jquery}
 * (an attack) produce the same name evidence. Their download counts do not.
 *
 * <p>A message is acknowledged only after it is fully handled. If scoring throws,
 * the message is left unacknowledged so the queue can redeliver it.
 */
@Service
@ConditionalOnExpression("'${radar.role}' == 'scorer' or '${radar.role}' == 'all'")
public class ScorerService {

    private static final Logger log = LoggerFactory.getLogger(ScorerService.class);

    private final MatchEngine engine = MatchEngine.withDefaults();
    private final QueueClient queue;
    private final CandidateFinder finder;
    private final AlertRepository alerts;

    @Value("${radar.scorer.batch-size:10}")
    private int batchSize;

    /**
     * The candidate must have fewer than this fraction of the reference's
     * downloads to be treated as an impersonation. At 0.1, a package with a
     * tenth of the reference's adoption or more is assumed to be a real sibling.
     */
    @Value("${radar.scorer.popularity-ratio:0.1}")
    private double popularityRatio;

    public ScorerService(QueueClient queue, CandidateFinder finder, AlertRepository alerts) {
        this.queue = queue;
        this.finder = finder;
        this.alerts = alerts;
        log.info("scorer started");
    }

    @Scheduled(fixedDelayString = "${radar.scorer.poll-ms:2000}")
    public void drain() {
        List<ReceivedMessage> batch = queue.receive(batchSize);

        for (ReceivedMessage message : batch) {
            try {
                score(message.payload());
                queue.acknowledge(message);
            } catch (Exception e) {
                // Left unacknowledged on purpose: the queue will redeliver it.
                log.error("failed to score {}: {}",
                    message.payload().name(), e.getMessage(), e);
            }
        }

        if (!batch.isEmpty()) {
            log.info("scored {} packages, depth now {}", batch.size(), queue.depth());
        }
    }

        private void score(PackageMessage pkg) {
        String normalized = Normalizer.normalize(pkg.name());
        alerts.recordObserved(pkg, normalized);

        if (normalized.isEmpty()) {
            return;
        }

        // A package impersonates one thing. Keep only the strongest match, so a
        // single fake produces a single alert rather than one per lookalike.
        MatchResult best = null;

        for (Candidate reference : finder.findCandidates(normalized)) {
            // A popular package is in the reference set itself; never compare it to itself.
            if (reference.name().equals(pkg.name())) {
                continue;
            }

            MatchResult result = engine.score(pkg.name(), reference.name());
            if (!result.isMatch()) {
                continue;
            }

            if (!hasPopularityGap(pkg, reference)) {
                log.debug("suppressed {} vs {}: candidate is comparably popular",
                    pkg.name(), reference.name());
                continue;
            }

            if (best == null || result.totalWeight() > best.totalWeight()) {
                best = result;
            }
        }

        if (best != null) {
            alerts.saveAlert(best);
        }
    }

    /** True when the candidate is obscure next to the package it resembles. */
    private boolean hasPopularityGap(PackageMessage pkg, Candidate reference) {
        return pkg.weeklyDownloads() < reference.weeklyDownloads() * popularityRatio;
    }
}