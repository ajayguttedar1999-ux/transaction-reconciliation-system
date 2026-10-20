package com.recon.transaction.repository;

import com.recon.transaction.entity.RecoveryTransactionQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecoveryTransactionQueueRepository extends JpaRepository<RecoveryTransactionQueue, Long> {
    List<RecoveryTransactionQueue> findByStatus(String status);
}
