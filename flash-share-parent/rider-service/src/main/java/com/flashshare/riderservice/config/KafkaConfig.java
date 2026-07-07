package com.flashshare.riderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String RIDER_TOPIC = "rider-events";

    @Bean
    public NewTopic riderEventsTopic() {
        return TopicBuilder.name(RIDER_TOPIC)
                .partitions(3)         // Supports parallel processing across multiple consumers
                .replicas(1)           // Matches our single-broker local docker setup
                .build();
    }
}