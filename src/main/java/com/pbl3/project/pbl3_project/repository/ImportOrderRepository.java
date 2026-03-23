package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.ImportOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImportOrderRepository extends JpaRepository<ImportOrder, Long> {
    
    @Query("SELECT o FROM ImportOrder o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<ImportOrder> findByIdWithItems(Long id);
}
