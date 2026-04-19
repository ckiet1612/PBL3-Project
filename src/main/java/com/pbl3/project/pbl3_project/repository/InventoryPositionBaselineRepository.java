package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.InventoryPositionBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryPositionBaselineRepository extends JpaRepository<InventoryPositionBaseline, Long> {
    Optional<InventoryPositionBaseline> findByProductId(Long productId);
}
