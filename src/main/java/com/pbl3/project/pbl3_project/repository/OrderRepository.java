package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.dto.IdLabelOption;
import com.pbl3.project.pbl3_project.dto.CustomerOrderAggregate;
import com.pbl3.project.pbl3_project.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
    java.util.Optional<Order> findByIdWithItems(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT new com.pbl3.project.pbl3_project.dto.IdLabelOption(
            o.user.id,
            CONCAT(
                COALESCE(NULLIF(o.user.fullName, ''), 'System'),
                ' (',
                COALESCE(NULLIF(o.user.username, ''), 'system'),
                ')'
            )
        )
        FROM Order o
        WHERE o.user IS NOT NULL
    """)
    java.util.List<IdLabelOption> findDistinctCreatorOptions();

    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT new com.pbl3.project.pbl3_project.dto.IdLabelOption(
            o.user.id,
            CONCAT(
                COALESCE(NULLIF(o.user.fullName, ''), 'System'),
                ' (',
                COALESCE(NULLIF(o.user.username, ''), 'system'),
                ')'
            )
        )
        FROM Order o
        WHERE o.user.id = :userId
    """)
    java.util.List<IdLabelOption> findDistinctCreatorOptionsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(MAX(o.totalPrice), 0) FROM Order o")
    BigDecimal findMaxTotalPrice();

    @org.springframework.data.jpa.repository.Query("""
        SELECT SUM(
            CASE
                WHEN o.status = com.pbl3.project.pbl3_project.entity.OrderStatus.CANCELED THEN 0
                ELSE (o.totalPrice - COALESCE(o.refundedAmount, 0))
            END
        )
        FROM Order o
        WHERE o.createdAt BETWEEN :start AND :end
    """)
    BigDecimal sumRevenueBetween(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(o)
        FROM Order o
        WHERE o.createdAt BETWEEN :start AND :end
          AND (o.status IS NULL OR o.status <> com.pbl3.project.pbl3_project.entity.OrderStatus.CANCELED)
    """)
    Long countOrdersBetween(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("""
        SELECT o
        FROM Order o
        WHERE (:start IS NULL OR o.createdAt >= :start)
          AND (:end IS NULL OR o.createdAt <= :end)
        ORDER BY o.createdAt ASC
    """)
    java.util.List<Order> findAllWithinDateRange(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT o
        FROM Order o
        WHERE o.user.id = :userId
          AND (:start IS NULL OR o.createdAt >= :start)
          AND (:end IS NULL OR o.createdAt <= :end)
        ORDER BY o.createdAt ASC
    """)
    java.util.List<Order> findAllWithinDateRangeAndUserId(
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end,
        @org.springframework.data.repository.query.Param("userId") Long userId
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT new com.pbl3.project.pbl3_project.dto.CustomerOrderAggregate(
            o.customer.id,
            COUNT(o),
            COALESCE(SUM(o.totalPrice - COALESCE(o.refundedAmount, 0)), 0),
            MAX(o.createdAt)
        )
        FROM Order o
        WHERE o.customer.id IN :customerIds
          AND (o.status IS NULL OR o.status <> com.pbl3.project.pbl3_project.entity.OrderStatus.CANCELED)
        GROUP BY o.customer.id
    """)
    java.util.List<CustomerOrderAggregate> findCustomerOrderAggregates(
        @org.springframework.data.repository.query.Param("customerIds") Collection<Long> customerIds
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(o)
        FROM Order o
        WHERE o.appliedOrderPromotionIdSnapshot = :promotionId
    """)
    long countByAppliedOrderPromotionIdSnapshot(@org.springframework.data.repository.query.Param("promotionId") Long promotionId);
}
