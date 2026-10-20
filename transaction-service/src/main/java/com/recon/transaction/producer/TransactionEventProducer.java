package com.recon.transaction.producer;

import com.recon.common.event.TransactionBookedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicName;

    public TransactionEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.transaction-booked}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publishTransactionBooked(TransactionBookedEvent event) {
        log.info("Publishing TransactionBookedEvent: txnId={}, productId={}", event.transactionId(), event.productId());
        kafkaTemplate.send(topicName, event.productId(), event);
    }
}
