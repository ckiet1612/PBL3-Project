package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @org.springframework.data.jpa.repository.Query("""
        SELECT oi
        FROM OrderItem oi
        JOIN FETCH oi.order o
        JOIN FETCH oi.product p
        LEFT JOIN FETCH p.category
    """)
    java.util.List<OrderItem> findAllWithOrderAndProduct();

    @org.springframework.data.jpa.repository.Query("""
        SELECT oi
        FROM OrderItem oi
        JOIN FETCH oi.order o
        JOIN FETCH oi.product p
        LEFT JOIN FETCH p.category
        WHERE (:start IS NULL OR o.createdAt >= :start)
          AND (:end IS NULL OR o.createdAt <= :end)
    """)
    java.util.List<OrderItem> findAllWithOrderAndProductWithinDateRange(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT oi
        FROM OrderItem oi
        JOIN FETCH oi.order o
        JOIN FETCH oi.product p
        LEFT JOIN FETCH p.category
        WHERE o.user.id = :userId
          AND (:start IS NULL OR o.createdAt >= :start)
          AND (:end IS NULL OR o.createdAt <= :end)
    """)
    java.util.List<OrderItem> findAllWithOrderAndProductWithinDateRangeAndUserId(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end,
        @org.springframework.data.repository.query.Param("userId") Long userId
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(oi)
        FROM OrderItem oi
        WHERE oi.appliedProductPromotionIdSnapshot = :promotionId
    """)
    long countByAppliedProductPromotionIdSnapshot(@org.springframework.data.repository.query.Param("promotionId") Long promotionId);
}
