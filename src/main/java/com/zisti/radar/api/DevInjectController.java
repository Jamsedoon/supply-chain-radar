package com.zisti.radar.api;

import com.zisti.radar.queue.PackageMessage;
import com.zisti.radar.queue.QueueClient;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Puts a hand-made package on the queue.
 *
 * <p>Two uses: proving the alert path fires (real typosquats are too rare to
 * wait for), and flooding the queue for the scaling demo.
 *
 * <p>Only exists when {@code APP_ROLE=all}, the local development mode. The
 * deployed ECS services run as collector, scorer, or api, so this endpoint is
 * never reachable in AWS.
 */
@RestController
@RequestMapping("/dev")
@ConditionalOnExpression("'${radar.role}' == 'all'")
public class DevInjectController {

    private final QueueClient queue;

    public DevInjectController(QueueClient queue) {
        this.queue = queue;
    }

    /** Queues one package, e.g. POST /dev/inject?name=lodahs&downloads=12 */
    @PostMapping("/inject")
    public Map<String, Object> inject(@RequestParam String name,
                                      @RequestParam(defaultValue = "0") long downloads) {
        queue.send(new PackageMessage(name, downloads, "dev-inject", null, Instant.now()));
        return Map.of("queued", name, "depth", queue.depth());
    }

    /** Queues many copies at once, for the scaling demo. */
    @PostMapping("/flood")
    public Map<String, Object> flood(@RequestParam(defaultValue = "1000") int count) {
        for (int i = 0; i < count; i++) {
            queue.send(new PackageMessage(
                "flood-test-" + i, 0, "dev-inject", null, Instant.now()));
        }
        return Map.of("queued", count, "depth", queue.depth());
    }
}