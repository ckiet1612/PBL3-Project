package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findAllByIsDeletedFalse();
}
