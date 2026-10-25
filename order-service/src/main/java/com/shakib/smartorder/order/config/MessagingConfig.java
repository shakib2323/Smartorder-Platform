package com.shakib.smartorder.order.config;

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

import com.shakib.smartorder.order.exception.InvalidOrderStateException;
import com.shakib.smartorder.order.exception.OrderNotFoundException;
import com.shakib.smartorder.order.messaging.Topics;

/*
  This configuration manages the outbox publisher, Kafka topics, and consumer errors.
  Set messaging.enabled=false to disable messaging when running database-only tests.
  Events with the same orderId are sent to the same partition, so they are processed
  in order for each order. We use three partitions to allow three consumers to process
  events in parallel. For local development, one Kafka broker is enough. In production,
  we should use three replicas.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true", matchIfMissing = true)
public class MessagingConfig {

    @Bean
    public NewTopic orderEventsTopic()
    {
        return TopicBuilder.name(Topics.ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderEventsDeadLetterTopic()
    {
        return TopicBuilder.name(Topics.ORDER_EVENTS + Topics.DEAD_LETTER_SUFFIX).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryEventsTopic() 
    {
        return TopicBuilder.name(Topics.INVENTORY_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryEventsDeadLetterTopic() 
    {
        return TopicBuilder.name(Topics.INVENTORY_EVENTS + Topics.DEAD_LETTER_SUFFIX).partitions(1).replicas(1).build();
    }

    /*
      Failed message: retry 4 times with growing waits (0.5s, 1s, 2s, 4s), then publish it to
      <topic>.DLT so it is kept for inspection and replay instead of blocking the partition.
      Malformed messages and order-state conflicts skip the retries: repeating them cannot help.
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
        handler.addNotRetryableExceptions(IllegalArgumentException.class,
                InvalidOrderStateException.class, OrderNotFoundException.class);
        return handler;
    }
}

