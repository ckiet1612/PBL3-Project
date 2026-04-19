package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.dto.IdLabelOption;
import com.pbl3.project.pbl3_project.entity.ImportOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ImportOrderRepository extends JpaRepository<ImportOrder, Long>, JpaSpecificationExecutor<ImportOrder> {
    
    @Query("SELECT o FROM ImportOrder o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<ImportOrder> findByIdWithItems(Long id);

    @Query("""
        SELECT DISTINCT new com.pbl3.project.pbl3_project.dto.IdLabelOption(
            o.supplier.id,
            COALESCE(NULLIF(o.supplier.name, ''), '-')
        )
        FROM ImportOrder o
        WHERE o.supplier IS NOT NULL
    """)
    java.util.List<IdLabelOption> findDistinctSupplierOptions();

    @Query("SELECT COALESCE(MAX(o.totalCost), 0) FROM ImportOrder o")
    BigDecimal findMaxTotalCost();
}
