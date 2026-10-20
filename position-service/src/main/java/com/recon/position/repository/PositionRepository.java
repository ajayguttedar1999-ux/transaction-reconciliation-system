package com.recon.position.repository;

import com.recon.position.entity.PositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<PositionEntity, Long> {
    Optional<PositionEntity> findByProductId(String productId);
}
