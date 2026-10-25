package com.shakib.smartorder.order.messaging;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Kafka entry point for Inventory's answers. Reads headers and hands over to InventoryEventHandler.
@Component
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true", matchIfMissing = true)
public class InventoryEventConsumer {

    private final InventoryEventHandler handler;

    public InventoryEventConsumer(InventoryEventHandler handler) 
    {
        this.handler = handler;
    }

    @KafkaListener(topics = Topics.INVENTORY_EVENTS, groupId = "order-service")
    public void onMessage(ConsumerRecord<String, String> record)
    {
        String eventId = header(record, "eventId");
        String eventType = header(record, "eventType");
        String correlationId = header(record, "correlationId");
        if (eventId == null || eventType == null) {
            throw new IllegalArgumentException("Event is missing the eventId or eventType header");
        }
        if (correlationId != null)
        {
            MDC.put("correlationId", correlationId);
        }
        try {
            handler.handle(eventId, eventType, record.value());
        } finally {
            MDC.remove("correlationId");
        }
    }

    private static String header(ConsumerRecord<String, String> record, String name)
    {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
