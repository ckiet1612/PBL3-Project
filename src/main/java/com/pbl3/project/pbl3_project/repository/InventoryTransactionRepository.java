package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.InventoryTransaction;
import com.pbl3.project.pbl3_project.entity.InventoryTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long>, JpaSpecificationExecutor<InventoryTransaction> {
    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<InventoryTransaction> findByProductIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAscIdAsc(Long productId, LocalDateTime createdAt);
    List<InventoryTransaction> findByImportOrderIdAndTransactionTypeOrderByCreatedAtAscIdAsc(Long importOrderId, InventoryTransactionType transactionType);
    List<InventoryTransaction> findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);
    List<InventoryTransaction> findByImportOrderIdOrderByCreatedAtAscIdAsc(Long importOrderId);

    boolean existsByProductIdAndCreatedAtAfter(Long productId, java.time.LocalDateTime createdAt);
    List<InventoryTransaction> findAllByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("""
        SELECT tx
        FROM InventoryTransaction tx
        LEFT JOIN FETCH tx.product p
        LEFT JOIN FETCH p.category
        ORDER BY tx.createdAt DESC
    """)
    List<InventoryTransaction> findAllWithProductOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("""
        SELECT tx
        FROM InventoryTransaction tx
        LEFT JOIN FETCH tx.product p
        LEFT JOIN FETCH p.category
        WHERE tx.createdAt > :createdAt
        ORDER BY tx.createdAt DESC
    """)
    List<InventoryTransaction> findAllWithProductAfterOrderByCreatedAtDesc(
        @org.springframework.data.repository.query.Param("createdAt") LocalDateTime createdAt
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT tx
        FROM InventoryTransaction tx
        LEFT JOIN FETCH tx.product p
        LEFT JOIN FETCH p.category
        WHERE tx.quantityChange > 0
        ORDER BY tx.createdAt DESC
    """)
    List<InventoryTransaction> findInboundWithProductOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT u.username
        FROM InventoryTransaction tx
        LEFT JOIN tx.user u
        WHERE u.username IS NOT NULL
        ORDER BY u.username
    """)
    java.util.List<String> findDistinctUsernames();

    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(MAX(ABS(tx.quantityChange)), 0)
        FROM InventoryTransaction tx
    """)
    Double findMaxAbsoluteQuantityChange();

    @Modifying
    @Transactional
    @Query("UPDATE InventoryTransaction tx SET tx.createdAt = :createdAt WHERE tx.id = :id")
    void overrideCreatedAt(
        @org.springframework.data.repository.query.Param("id") Long id,
        @org.springframework.data.repository.query.Param("createdAt") LocalDateTime createdAt
    );
}
