package com.shakib.smartorder.product.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/*
  Transactional outbox row. It is written in the same transaction as the business change,
  so the change and the event either both happen or neither does. A publisher (added in the
  Kafka step) later reads PENDING rows and sends them to Kafka.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    public static final String PENDING = "PENDING";
    public static final String SENT = "SENT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, columnDefinition = "CHAR(36)", updatable = false)
    private String eventId;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    @Column(nullable = false, length = 100, updatable = false)
    private String topic;

    @Column(nullable = false, columnDefinition = "json", updatable = false)
    private String payload;

    @Column(name = "correlation_id", length = 64, updatable = false)
    private String correlationId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected OutboxEvent() {
        // required by JPA
    }

    public OutboxEvent(String eventId, Long aggregateId, String eventType, String topic,
            String payload, String correlationId) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.correlationId = correlationId;
        this.status = PENDING;
        this.attempts = 0;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { 
    	return id; 
    }
    public String getEventId() { 
    	return eventId; 
    }
    public Long getAggregateId() { 
    	return aggregateId; 
    }
    public String getEventType() { 
    	return eventType; 
    }
    public String getTopic() { 
    	return topic;
    }
    public String getPayload() { 
    	return payload; 
    }
    public String getCorrelationId() { 
    	return correlationId; 
    }
    public String getStatus() { 
    	return status; 
    }
    public int getAttempts() { 
    	return attempts; 
    }
    public Instant getCreatedAt() { 
    	return createdAt; 
    }
    public Instant getSentAt() { 
    	return sentAt; 
    	
    }
}
