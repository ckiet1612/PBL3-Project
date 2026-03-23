package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    java.util.List<Product> findAllByIsDeletedFalse();
    
    // Count products with quantity below threshold (low stock)
    long countByQuantityLessThanAndIsDeletedFalse(int threshold);

    // Dynamic low stock: each product has its own minStockLevel
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.quantity <= p.minStockLevel AND p.isDeleted = false ORDER BY p.quantity ASC")
    java.util.List<Product> findLowStockProducts();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM Product p WHERE p.quantity <= p.minStockLevel AND p.isDeleted = false")
    long countLowStockProducts();
}
