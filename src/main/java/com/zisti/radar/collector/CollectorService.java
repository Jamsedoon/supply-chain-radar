package com.zisti.radar.collector;

import com.zisti.radar.queue.PackageMessage;
import com.zisti.radar.queue.QueueClient;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Polls the npm registry and queues what it finds.
 *
 * <p>Runs only when {@code APP_ROLE=collector}. The same jar runs as collector,
 * scorer, or api depending on that one variable, so one image is built and
 * deployed three times rather than three images being maintained.
 *
 * <p>Deliberately does no scoring and holds no database connection. This is the
 * only service that talks to the public internet, so it is the one with the
 * least to lose if it is compromised. On AWS its IAM role grants write access to
 * one queue and nothing else.
 */
@Service
@ConditionalOnExpression("'${radar.role}' == 'collector' or '${radar.role}' == 'all'")
public class CollectorService {

    private static final Logger log = LoggerFactory.getLogger(CollectorService.class);

    private final NpmRegistryClient registry;
    private final QueueClient queue;

    @Value("${radar.collector.batch-size:20}")
    private int batchSize;

    public CollectorService(NpmRegistryClient registry, QueueClient queue) {
        this.registry = registry;
        this.queue = queue;
        log.info("collector started");
    }

    /**
     * Fetches a batch and queues it.
     *
     * <p>{@code fixedDelay} counts from the end of the previous run, not the
     * start. With {@code fixedRate}, a slow run would overlap the next one and
     * the overlap would compound.
     */
    @Scheduled(fixedDelayString = "${radar.collector.interval-ms:60000}",
               initialDelayString = "${radar.collector.initial-delay-ms:5000}")
    public void pollRegistry() {
        try {
            List<PackageMessage> packages = registry.fetchRecent(batchSize);

            for (PackageMessage pkg : packages) {
                queue.send(pkg);
            }

            log.info("queued {} packages, depth now {}", packages.size(), queue.depth());

        } catch (Exception e) {
            // A failed poll must not kill the scheduler. The next tick retries.
            log.error("poll failed: {}", e.getMessage(), e);
        }
    }
}