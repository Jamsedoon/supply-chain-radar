package com.zisti.radar.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.ObjectMapper;

/**
 * The queue on AWS: Amazon SQS.
 *
 * <p>Used when {@code QUEUE_TYPE=sqs}. Unlike the in-memory queue, it lives
 * outside every container, so the collector, scorer, and API can run as
 * separate ECS services and still share one queue.
 *
 * <p>Credentials and region are never set in code. The AWS SDK finds them
 * itself: on ECS from the task's IAM role, on a laptop from {@code aws configure}.
 *
 * <p>A message the scorer cannot read is never acknowledged. SQS returns it to
 * the queue, and after several failed attempts moves it to the dead-letter queue
 * set up in Terraform, so one bad message cannot block the rest.
 */
@Component
@ConditionalOnProperty(name = "radar.queue.type", havingValue = "sqs")
public class SqsQueueClient implements QueueClient {

    private static final Logger log = LoggerFactory.getLogger(SqsQueueClient.class);

    /** SQS returns at most 10 messages per request. */
    private static final int SQS_MAX_BATCH = 10;

    /**
     * Long polling: wait up to this long for a message before returning empty.
     * Far fewer empty requests than asking every 2 seconds, which keeps the
     * request count well inside the SQS free tier.
     */
    private static final int WAIT_SECONDS = 10;

    private final SqsClient sqs;
    private final String queueUrl;
    private final ObjectMapper json = new ObjectMapper();

    public SqsQueueClient(@Value("${radar.queue.url}") String queueUrl) {
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException(
                "QUEUE_TYPE=sqs needs SQS_QUEUE_URL to be set");
        }
        this.queueUrl = queueUrl;
        this.sqs = SqsClient.create();
        log.info("queue: Amazon SQS ({})", queueUrl);
    }

    @Override
    public void send(PackageMessage message) {
        sqs.sendMessage(SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(json.writeValueAsString(message))
            .build());
    }

    @Override
    public List<ReceivedMessage> receive(int maxMessages) {
        List<Message> messages = sqs.receiveMessage(ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(Math.max(1, Math.min(maxMessages, SQS_MAX_BATCH)))
            .waitTimeSeconds(WAIT_SECONDS)
            .build())
            .messages();

        List<ReceivedMessage> batch = new ArrayList<>(messages.size());

        for (Message m : messages) {
            try {
                PackageMessage payload = json.readValue(m.body(), PackageMessage.class);
                batch.add(new ReceivedMessage(payload, m.receiptHandle()));
            } catch (Exception e) {
                // Not acknowledged on purpose: SQS retries it, then moves it to
                // the dead-letter queue.
                log.error("unreadable message {}: {}", m.messageId(), e.getMessage());
            }
        }

        return batch;
    }

    @Override
    public void acknowledge(ReceivedMessage message) {
        sqs.deleteMessage(DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(message.receiptHandle())
            .build());
    }

    @Override
    public int depth() {
        Map<QueueAttributeName, String> attributes = sqs.getQueueAttributes(
            GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                .build())
            .attributes();

        return Integer.parseInt(
            attributes.getOrDefault(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES, "0"));
    }
}