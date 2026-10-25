package com.shakib.smartorder.inventory.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.shakib.smartorder.inventory.messaging.Topics;


/**
 * Scheduling for the outbox publisher, topic declarations, and consumer error handling.
 * Topics are declared in every service that uses them; creating an existing topic is a no-op,
 * so the start order of the services does not matter.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true", matchIfMissing = true)
public class MessagingConfig {

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(Topics.ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderEventsDeadLetterTopic() {
        return TopicBuilder.name(Topics.ORDER_EVENTS + Topics.DEAD_LETTER_SUFFIX).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic productEventsTopic() {
        return TopicBuilder.name(Topics.PRODUCT_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic productEventsDeadLetterTopic() {
        return TopicBuilder.name(Topics.PRODUCT_EVENTS + Topics.DEAD_LETTER_SUFFIX).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(Topics.INVENTORY_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryEventsDeadLetterTopic() {
        return TopicBuilder.name(Topics.INVENTORY_EVENTS + Topics.DEAD_LETTER_SUFFIX).partitions(1).replicas(1).build();
    }

    /*
      Failed message: retry 4 times with growing waits (0.5s, 1s, 2s, 4s), then publish it to
      <topic>.DLT so it is kept for inspection and replay instead of blocking the partition.
      Malformed messages (IllegalArgumentException) skip the retries and go straight to the DLT.
      The DLT has one partition, so the destination is always partition 0.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) 
    {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(record.topic() + Topics.DEAD_LETTER_SUFFIX, 0));

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(4);
        backOff.setInitialInterval(500L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
