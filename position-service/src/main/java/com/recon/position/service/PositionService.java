package com.recon.position.service;

import com.recon.common.event.PositionCreatedEvent;
import com.recon.common.event.TransactionBookedEvent;
import com.recon.position.entity.PositionEntity;
import com.recon.position.producer.PositionEventProducer;
import com.recon.position.repository.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PositionService {

    private static final Logger log = LoggerFactory.getLogger(PositionService.class);

    private final PositionRepository positionRepository;
    private final PositionEventProducer eventProducer;

    public PositionService(PositionRepository positionRepository, PositionEventProducer eventProducer) {
        this.positionRepository = positionRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public PositionEntity processTransactionBooked(TransactionBookedEvent event) {
        log.info("Processing transaction for position: txnId={}, product={}", event.transactionId(), event.productId());

        BigDecimal txnAmount = event.quantity().multiply(event.price());

        PositionEntity position = positionRepository.findByProductId(event.productId())
                .orElseGet(() -> new PositionEntity("POS-" + event.productId(), event.productId(), BigDecimal.ZERO, BigDecimal.ZERO));

        position.addTransaction(event.quantity(), txnAmount);
        PositionEntity saved = positionRepository.save(position);

        PositionCreatedEvent posEvent = new PositionCreatedEvent(
                UUID.randomUUID().toString(),
                saved.getPositionId(),
                saved.getProductId(),
                saved.getTotalQuantity(),
                saved.getTotalAmount(),
                saved.getVersion() != null ? saved.getVersion() : 0,
                saved.getUpdatedAt()
        );
        eventProducer.publishPositionCreated(posEvent);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<PositionEntity> getAllPositions() {
        return positionRepository.findAll();
    }
}
