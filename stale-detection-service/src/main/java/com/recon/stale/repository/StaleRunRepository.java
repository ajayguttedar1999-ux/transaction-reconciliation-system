package com.recon.stale.repository;

import com.recon.stale.entity.ReconRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StaleRunRepository extends JpaRepository<ReconRunEntity, Long> {
    List<ReconRunEntity> findByReconDateAndIsStaleFalse(LocalDate reconDate);
    List<ReconRunEntity> findByIsStaleTrue();
}
