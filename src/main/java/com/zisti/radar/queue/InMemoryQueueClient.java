package com.zisti.radar.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * A queue held in memory, for local development and tests.
 *
 * <p>Replaced by {@code SqsQueueClient} on AWS. Everything in this class is
 * lost when the process stops, which is acceptable locally and is exactly why
 * it is not used in production.
 *
 * <p>Models the send-receive-acknowledge cycle honestly: received messages move
 * to an in-flight map rather than disappearing, so an unacknowledged message is
 * visibly still owed. It does not model a visibility timeout — an
 * unacknowledged message here stays in flight rather than returning to the
 * queue.
 */
@Component
@ConditionalOnProperty(name = "radar.queue.type", havingValue = "memory",
                       matchIfMissing = true)
public class InMemoryQueueClient implements QueueClient {

    private static final Logger log = LoggerFactory.getLogger(InMemoryQueueClient.class);

    private final ConcurrentLinkedQueue<PackageMessage> waiting =
        new ConcurrentLinkedQueue<>();

    private final Map<String, PackageMessage> inFlight = new ConcurrentHashMap<>();

    public InMemoryQueueClient() {
        log.info("queue: in-memory (local development)");
    }

    @Override
    public void send(PackageMessage message) {
        waiting.add(message);
    }

    @Override
    public List<ReceivedMessage> receive(int maxMessages) {
        List<ReceivedMessage> batch = new ArrayList<>(maxMessages);

        for (int i = 0; i < maxMessages; i++) {
            PackageMessage message = waiting.poll();
            if (message == null) {
                break;
            }

            String handle = UUID.randomUUID().toString();
            inFlight.put(handle, message);
            batch.add(new ReceivedMessage(message, handle));
        }

        return batch;
    }

    @Override
    public void acknowledge(ReceivedMessage message) {
        inFlight.remove(message.receiptHandle());
    }

    @Override
    public int depth() {
        return waiting.size();
    }

    /** Messages received but not yet acknowledged. Test support. */
    public int inFlightCount() {
        return inFlight.size();
    }
}