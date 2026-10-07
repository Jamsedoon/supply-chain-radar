package com.zisti.radar.queue;

import java.util.List;

/**
 * A queue of packages waiting to be scored.
 *
 * <p>Deliberately minimal: send, receive, acknowledge. Anything richer would
 * leak the implementation into the services that use it, which is the one thing
 * this interface exists to prevent.
 *
 * <p>Two implementations: {@code InMemoryQueueClient} for local development and
 * tests, {@code SqsQueueClient} for AWS. Neither the collector nor the scorer
 * knows which one it has.
 *
 * <p>Delivery is <strong>at least once</strong>. A message may arrive twice if
 * a consumer crashes after processing but before acknowledging. Consumers must
 * tolerate that — here, a unique constraint on the alert table makes a repeat
 * harmless.
 */
public interface QueueClient {

    /** Puts one package on the queue. */
    void send(PackageMessage message);

    /**
     * Takes up to {@code maxMessages} off the queue.
     *
     * <p>Returns an empty list when the queue is empty rather than blocking.
     * Messages stay invisible to other consumers until acknowledged or until an
     * implementation-defined timeout returns them to the queue.
     */
    List<ReceivedMessage> receive(int maxMessages);

    /** Confirms a message was handled, removing it permanently. */
    void acknowledge(ReceivedMessage message);

    /** How many messages are waiting. Approximate, and the key health metric. */
    int depth();

    /**
     * A message plus the handle needed to acknowledge it.
     *
     * @param receiptHandle implementation-specific token identifying this delivery
     */
    record ReceivedMessage(PackageMessage payload, String receiptHandle) {
    }
}