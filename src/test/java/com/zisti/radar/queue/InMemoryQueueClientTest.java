package com.zisti.radar.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryQueueClientTest {

    private InMemoryQueueClient queue;

    @BeforeEach
    void setUp() {
        queue = new InMemoryQueueClient();
    }

    private static PackageMessage message(String name) {
        return new PackageMessage(name, 100, "someone", null, Instant.now());
    }

    @Test
    @DisplayName("a sent message can be received")
    void sendThenReceive() {
        queue.send(message("lodahs"));

        List<QueueClient.ReceivedMessage> batch = queue.receive(10);

        assertEquals(1, batch.size());
        assertEquals("lodahs", batch.get(0).payload().name());
    }

    @Test
    @DisplayName("depth reports how many messages are waiting")
    void depthCountsWaitingMessages() {
        assertEquals(0, queue.depth());

        queue.send(message("a"));
        queue.send(message("b"));
        queue.send(message("c"));

        assertEquals(3, queue.depth());
    }

    @Test
    @DisplayName("receiving removes messages from the waiting count")
    void receivingReducesDepth() {
        queue.send(message("a"));
        queue.send(message("b"));

        queue.receive(1);

        assertEquals(1, queue.depth());
    }

    @Test
    @DisplayName("a received message stays in flight until acknowledged")
    void unacknowledgedMessagesStayInFlight() {
        queue.send(message("lodahs"));

        List<QueueClient.ReceivedMessage> batch = queue.receive(1);
        assertEquals(1, queue.inFlightCount(),
            "a received message is owed until acknowledged");

        queue.acknowledge(batch.get(0));
        assertEquals(0, queue.inFlightCount());
    }

    @Test
    @DisplayName("receive honours the batch size")
    void respectsBatchSize() {
        for (int i = 0; i < 10; i++) {
            queue.send(message("pkg-" + i));
        }

        assertEquals(3, queue.receive(3).size());
        assertEquals(7, queue.depth());
    }

    @Test
    @DisplayName("an empty queue returns an empty list rather than blocking")
    void emptyQueueReturnsEmptyList() {
        assertTrue(queue.receive(10).isEmpty());
    }

    @Test
    @DisplayName("messages come back in the order they were sent")
    void preservesOrder() {
        queue.send(message("first"));
        queue.send(message("second"));
        queue.send(message("third"));

        List<QueueClient.ReceivedMessage> batch = queue.receive(3);

        assertEquals("first", batch.get(0).payload().name());
        assertEquals("second", batch.get(1).payload().name());
        assertEquals("third", batch.get(2).payload().name());
    }

    @Test
    @DisplayName("a message without a name is rejected")
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
            () -> new PackageMessage("", 0, null, null, null));
    }

    @Test
    @DisplayName("a negative download count is rejected")
    void rejectsNegativeDownloads() {
        assertThrows(IllegalArgumentException.class,
            () -> new PackageMessage("lodash", -1, null, null, null));
    }
}