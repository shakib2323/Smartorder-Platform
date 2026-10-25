package com.shakib.smartorder.product.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.shakib.smartorder.product.messaging.Topics;

// Scheduling for the outbox publisher and the topic declaration. Off with messaging.enabled=false. 
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true", matchIfMissing = true)
public class MessagingConfig {

    @Bean
    public NewTopic productEventsTopic() {
        return TopicBuilder.name(Topics.PRODUCT_EVENTS).partitions(3).replicas(1).build();
    }
}
