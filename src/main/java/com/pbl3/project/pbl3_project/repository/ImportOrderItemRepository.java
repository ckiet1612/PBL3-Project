package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.ImportOrderItem;
import com.pbl3.project.pbl3_project.entity.ImportOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ImportOrderItemRepository extends JpaRepository<ImportOrderItem, Long> {
    @org.springframework.data.jpa.repository.Query("""
        SELECT item
        FROM ImportOrderItem item
        JOIN FETCH item.importOrder importOrder
        LEFT JOIN FETCH importOrder.supplier
        WHERE item.product.id IN :productIds
          AND importOrder.status = :status
        ORDER BY importOrder.createdAt DESC, item.id DESC
    """)
    List<ImportOrderItem> findCompletedImportSnapshotsForProducts(
        @org.springframework.data.repository.query.Param("productIds") Collection<Long> productIds,
        @org.springframework.data.repository.query.Param("status") ImportOrderStatus status
    );
}
