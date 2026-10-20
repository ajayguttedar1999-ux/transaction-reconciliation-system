package com.recon.reconciliation.producer;

import com.recon.common.event.ReconciliationCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicName;

    public ReconciliationEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.reconciliation-completed}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publishReconciliationCompleted(ReconciliationCompletedEvent event) {
        log.info("Publishing ReconciliationCompletedEvent: runId={}, status={}", event.runId(), event.overallStatus());
        kafkaTemplate.send(topicName, event.runId(), event);
    }
}
