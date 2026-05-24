package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.Promotion;
import com.pbl3.project.pbl3_project.entity.PromotionScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {

    @Query("""
        SELECT p
        FROM Promotion p
        LEFT JOIN FETCH p.targetProduct tp
        WHERE p.scope = com.pbl3.project.pbl3_project.entity.PromotionScope.PRODUCT
          AND p.enabled = true
          AND tp.id IN :productIds
          AND (p.startsAt IS NULL OR p.startsAt <= :at)
          AND (p.endsAt IS NULL OR p.endsAt >= :at)
    """)
    List<Promotion> findActiveProductPromotionsForProductIds(
        @org.springframework.data.repository.query.Param("productIds") Collection<Long> productIds,
        @org.springframework.data.repository.query.Param("at") LocalDateTime at
    );

    @Query("""
        SELECT p
        FROM Promotion p
        LEFT JOIN FETCH p.targetProduct
        WHERE p.scope = com.pbl3.project.pbl3_project.entity.PromotionScope.ORDER
          AND p.enabled = true
          AND (p.startsAt IS NULL OR p.startsAt <= :at)
          AND (p.endsAt IS NULL OR p.endsAt >= :at)
          AND (p.minOrderTotal IS NULL OR p.minOrderTotal <= :subtotal)
        ORDER BY p.name ASC, p.id ASC
    """)
    List<Promotion> findEligibleOrderPromotions(
        @org.springframework.data.repository.query.Param("subtotal") BigDecimal subtotal,
        @org.springframework.data.repository.query.Param("at") LocalDateTime at
    );

    @Query("""
        SELECT p
        FROM Promotion p
        LEFT JOIN FETCH p.targetProduct
        WHERE p.enabled = true
          AND (p.startsAt IS NULL OR p.startsAt <= :at)
          AND (p.endsAt IS NULL OR p.endsAt >= :at)
        ORDER BY p.scope ASC, p.name ASC
    """)
    List<Promotion> findAllActiveAt(@org.springframework.data.repository.query.Param("at") LocalDateTime at);

    @Query("""
        SELECT p
        FROM Promotion p
        WHERE p.enabled = true
          AND p.endsAt IS NOT NULL
          AND p.endsAt <= :endsBefore
    """)
    List<Promotion> findEnabledEndingBefore(@org.springframework.data.repository.query.Param("endsBefore") LocalDateTime endsBefore);

    @Query("""
        SELECT COUNT(p)
        FROM Promotion p
        WHERE p.scope = :scope
    """)
    long countByScope(@org.springframework.data.repository.query.Param("scope") PromotionScope scope);
}
