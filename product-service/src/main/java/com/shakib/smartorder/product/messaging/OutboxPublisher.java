package com.shakib.smartorder.product.messaging;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.shakib.smartorder.product.entity.OutboxEvent;
import com.shakib.smartorder.product.repository.OutboxEventRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/*
Reads pending events from the outbox table and sends them to Kafka, starting with the oldest.

Delivery: An event is marked as SENT only after Kafka confirms that it was received.
If the service crashes before updating the status, the same event may be sent again.
Consumers must handle duplicate events safely using the unique eventId included in each message.

Ordering: The aggregate ID is used as the message key, so events for the same product go
to the same Kafka partition and are processed in order. If sending an event fails, the batch
stops to prevent later events from being sent before it.

Kafka failure: Orders can still be accepted when Kafka is down. Events remain PENDING in
the database and are sent when Kafka becomes available again.

Limitation: Run only one instance of this service for now. Multiple instances could send
the same event or cause ordering issues. To support multiple instances, we would need to
claim database rows using SELECT ... FOR UPDATE SKIP LOCKED and maintain ordering per partition.
*/
@Component
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;
    private final long sendTimeoutMs;

    public OutboxPublisher(OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry meterRegistry,
            @Value("${outbox.publisher.batch-size:50}") int batchSize,
            @Value("${outbox.publisher.send-timeout-ms:5000}") long sendTimeoutMs) 
    {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
        this.sendTimeoutMs = sendTimeoutMs;

        // Outbox lag: a number that keeps growing means events are not reaching Kafka.
        Gauge.builder("outbox.pending.events", repository, r -> r.countByStatus(OutboxEvent.PENDING))
                .description("Outbox events written but not yet published to Kafka")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.poll-interval-ms:1000}")
    public void publishPending() 
    {
        List<OutboxEvent> batch = repository.findByStatusOrderByIdAsc(
                OutboxEvent.PENDING, PageRequest.of(0, batchSize));
        for (OutboxEvent event : batch) 
        {
            if (!publish(event)) {
                break;
            }
        }
    }

    private boolean publish(OutboxEvent event)
    {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    event.getTopic(), String.valueOf(event.getAggregateId()), event.getPayload());
            addHeader(record, "eventId", event.getEventId());
            addHeader(record, "eventType", event.getEventType());
            addHeader(record, "correlationId", event.getCorrelationId());

            kafkaTemplate.send(record).get(sendTimeoutMs, TimeUnit.MILLISECONDS);

            repository.markSent(event.getId());
            log.info("Published {} (eventId={}) to {} [correlationId={}]",
                    event.getEventType(), event.getEventId(), event.getTopic(), event.getCorrelationId());
            return true;
        } catch (InterruptedException e) 
        {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            repository.recordFailure(event.getId());
            log.warn("Could not publish {} (eventId={}), will retry on the next poll: {}",
                    event.getEventType(), event.getEventId(), e.toString());
            return false;
        }
    }

    private static void addHeader(ProducerRecord<String, String> record, String name, String value)
    {
        if (value != null) {
            record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
