package com.recon.position.producer;

import com.recon.common.event.PositionCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PositionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PositionEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicName;

    public PositionEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.position-created}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publishPositionCreated(PositionCreatedEvent event) {
        log.info("Publishing PositionCreatedEvent: positionId={}, totalQty={}", event.positionId(), event.totalQuantity());
        kafkaTemplate.send(topicName, event.productId(), event);
    }
}
