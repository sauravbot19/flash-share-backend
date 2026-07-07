package com.flashshare.riderservice.service;

import com.flashshare.riderservice.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendRiderCreatedEvent(String riderId, String payload) {
        log.info("Publishing rider-created event to Kafka for ID: {}", riderId);

        kafkaTemplate.send(KafkaConfig.RIDER_TOPIC, riderId, payload)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully sent message to partition [{}] at offset [{}]",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to deliver message to Kafka due to: {}", ex.getMessage());
                    }
                });
    }
}