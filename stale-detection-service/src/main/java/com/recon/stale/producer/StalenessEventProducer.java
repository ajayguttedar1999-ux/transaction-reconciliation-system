package com.recon.stale.producer;

import com.recon.common.event.StalenessDetectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class StalenessEventProducer {

    private static final Logger log = LoggerFactory.getLogger(StalenessEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicName;

    public StalenessEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.staleness-detected}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publishStalenessDetected(StalenessDetectedEvent event) {
        log.warn("Publishing StalenessDetectedEvent: runId={}, txnId={}", event.runId(), event.triggeredByTransactionId());
        kafkaTemplate.send(topicName, event.runId(), event);
    }
}
