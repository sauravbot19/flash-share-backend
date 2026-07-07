package com.flashshare.riderservice.service;

import com.flashshare.riderservice.config.KafkaConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RiderEventConsumer {

    @KafkaListener(topics = KafkaConfig.RIDER_TOPIC, groupId = "flashshare-rider-group")
    public void consumeRiderEvent(ConsumerRecord<String, String> record) {
        log.info(">>>> Kafka Message Received! <<<<");
        log.info("Partition: {}, Offset: {}", record.partition(), record.offset());
        log.info("Message Key (Rider ID): {}", record.key());
        log.info("Message Payload: {}", record.value());

        // This is where business processing logic for other microservices would execute,
        // such as triggering a welcome SMS, initializing background checks, or updating a live analytics board.
    }
}