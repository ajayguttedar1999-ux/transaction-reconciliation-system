package com.recon.position.consumer;

import com.recon.common.event.TransactionBookedEvent;
import com.recon.position.service.PositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final PositionService positionService;

    public TransactionEventConsumer(PositionService positionService) {
        this.positionService = positionService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.transaction-booked}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onTransactionBooked(TransactionBookedEvent event) {
        log.info("Position consumer received: txnId={}, product={}", event.transactionId(), event.productId());
        positionService.processTransactionBooked(event);
    }
}
