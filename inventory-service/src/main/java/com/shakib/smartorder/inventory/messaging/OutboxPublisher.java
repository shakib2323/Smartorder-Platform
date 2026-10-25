package com.shakib.smartorder.inventory.messaging;

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

import com.shakib.smartorder.inventory.entity.OutboxEvent;
import com.shakib.smartorder.inventory.repository.OutboxEventRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/*
  Reads PENDING rows from the outbox table and sends them to Kafka, oldest first.
 
  Guarantees:
  - At-least-once: the row is marked SENT only after Kafka acknowledged the message. A crash
    between the send and the mark causes a re-send, so consumers must be idempotent. Every
    message carries a unique eventId header for that purpose.
  - Ordering: the message key is the order id, so all events of one order go to one partition
    in order. On any failure the batch stops, so a later event never overtakes an earlier
    one that has not been sent yet.
  - Business flow never depends on Kafka: if Kafka is down, orders are still accepted, rows
    stay PENDING, and they are published once Kafka is back.
 
  Limitation: run exactly one instance of this service. With several instances the same row
  could be sent twice and ordering is no longer guaranteed. Scaling out would need row
  claiming (SELECT ... FOR UPDATE SKIP LOCKED) with partition-aware ordering.
 */
@Component
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;
    private final long sendTimeoutMs;

    public OutboxPublisher(OutboxEventRepository repository, KafkaTemplate<String, String> kafkaTemplate,
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
        for (OutboxEvent event : batch) {
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
        } catch (InterruptedException e) {
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
