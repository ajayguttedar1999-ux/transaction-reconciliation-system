package com.recon.stale.consumer;

import com.recon.common.event.TransactionBookedEvent;
import com.recon.stale.service.StaleDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LateTransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(LateTransactionConsumer.class);

    private final StaleDetectionService staleDetectionService;

    public LateTransactionConsumer(StaleDetectionService staleDetectionService) {
        this.staleDetectionService = staleDetectionService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.transaction-booked}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onTransactionBooked(TransactionBookedEvent event) {
        log.info("Stale Detection received event: txnId={}", event.transactionId());
        staleDetectionService.evaluateTransactionForStaleness(event);
    }
}
