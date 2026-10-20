package com.recon.transaction.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recon.common.event.TransactionBookedEvent;
import com.recon.transaction.entity.RecoveryTransactionQueue;
import com.recon.transaction.repository.RecoveryTransactionQueueRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RecoveryTransactionQueueRepository recoveryRepository;
    private final ObjectMapper objectMapper;
    private final String topicName;

    public TransactionEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            RecoveryTransactionQueueRepository recoveryRepository,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.transaction-booked}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.recoveryRepository = recoveryRepository;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
    }

    /**
     * Publishes event to Kafka with Resilience4j Circuit Breaker & Automatic Retry.
     */
    @Retry(name = "kafkaProducer", fallbackMethod = "fallbackPublishTransactionBooked")
    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "fallbackPublishTransactionBooked")
    public void publishTransactionBooked(TransactionBookedEvent event) {
        log.info("Publishing TransactionBookedEvent: txnId={}, productId={}", event.transactionId(), event.productId());
        kafkaTemplate.send(topicName, event.productId(), event);
    }

    /**
     * Fallback Method executed when Kafka fails or Circuit Breaker is OPEN.
     * Prevents data loss by persisting event to recovery queue for offline replay.
     */
    public void fallbackPublishTransactionBooked(TransactionBookedEvent event, Throwable throwable) {
        log.warn("⚠️ Circuit Breaker / Retry FALLBACK activated for txnId={}. Kafka unavailable: {}",
                event.transactionId(), throwable.getMessage());
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            RecoveryTransactionQueue dlqItem = new RecoveryTransactionQueue(
                    event.transactionId(),
                    event.productId(),
                    jsonPayload,
                    throwable.getMessage()
            );
            recoveryRepository.save(dlqItem);
            log.info("🛡️ Transaction {} saved safely into recovery queue for later replay.", event.transactionId());
        } catch (Exception ex) {
            log.error("Failed to persist transaction to recovery queue", ex);
        }
    }
}
