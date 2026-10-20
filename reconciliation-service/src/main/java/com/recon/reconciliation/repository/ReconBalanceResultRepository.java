package com.recon.reconciliation.repository;

import com.recon.reconciliation.entity.ReconBalanceResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconBalanceResultRepository extends JpaRepository<ReconBalanceResultEntity, Long> {
    List<ReconBalanceResultEntity> findByReconRun_RunId(String runId);
}
