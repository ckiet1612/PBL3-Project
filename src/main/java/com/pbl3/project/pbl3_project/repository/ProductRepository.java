package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    java.util.List<Product> findAllByIsDeletedFalse();

    @org.springframework.data.jpa.repository.Query("""
        SELECT p
        FROM Product p
        LEFT JOIN FETCH p.category
        WHERE p.isDeleted = false
    """)
    java.util.List<Product> findAllActiveWithCategory();
    
    // Count products with quantity below threshold (low stock)
    long countByQuantityLessThanAndIsDeletedFalse(int threshold);

    // Dynamic low stock: each product has its own minStockLevel
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.quantity <= p.minStockLevel AND p.isDeleted = false ORDER BY p.quantity ASC")
    java.util.List<Product> findLowStockProducts();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM Product p WHERE p.quantity <= p.minStockLevel AND p.isDeleted = false")
    long countLowStockProducts();

    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT b.name
        FROM Product p
        LEFT JOIN p.brand b
        WHERE p.isDeleted = false
          AND p.category.id = :categoryId
          AND b.name IS NOT NULL
        ORDER BY b.name
    """)
    java.util.List<String> findDistinctBrandNamesByCategoryId(@org.springframework.data.repository.query.Param("categoryId") Long categoryId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(MAX(p.price), 0)
        FROM Product p
        WHERE p.isDeleted = false
          AND p.category.id = :categoryId
    """)
    BigDecimal findMaxPriceByCategoryId(@org.springframework.data.repository.query.Param("categoryId") Long categoryId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(MAX(p.quantity), 0)
        FROM Product p
        WHERE p.isDeleted = false
          AND p.category.id = :categoryId
    """)
    Integer findMaxQuantityByCategoryId(@org.springframework.data.repository.query.Param("categoryId") Long categoryId);
}
