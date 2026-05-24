package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    java.util.List<Product> findAllByIsDeletedFalse();

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    @org.springframework.data.jpa.repository.Query("""
        SELECT p
        FROM Product p
        LEFT JOIN FETCH p.category
        WHERE p.isDeleted = false
    """)
    java.util.List<Product> findAllActiveWithCategory();
    
    long countByQuantityLessThanAndIsDeletedFalse(int threshold);

    // Low-stock checks use each product's own threshold, not one global number.
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
        SELECT DISTINCT b.name
        FROM Product p
        LEFT JOIN p.brand b
        WHERE p.isDeleted = false
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:lowStockOnly = false OR p.quantity <= p.minStockLevel)
          AND b.name IS NOT NULL
          AND b.name <> ''
        ORDER BY b.name
    """)
    java.util.List<String> findCatalogBrandNames(
        @org.springframework.data.repository.query.Param("categoryId") Long categoryId,
        @org.springframework.data.repository.query.Param("lowStockOnly") boolean lowStockOnly
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(MAX(p.price), 0)
        FROM Product p
        WHERE p.isDeleted = false
          AND p.category.id = :categoryId
    """)
    BigDecimal findMaxPriceByCategoryId(@org.springframework.data.repository.query.Param("categoryId") Long categoryId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(MAX(p.price), 0)
        FROM Product p
        WHERE p.isDeleted = false
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:lowStockOnly = false OR p.quantity <= p.minStockLevel)
    """)
    BigDecimal findCatalogMaxPrice(
        @org.springframework.data.repository.query.Param("categoryId") Long categoryId,
        @org.springframework.data.repository.query.Param("lowStockOnly") boolean lowStockOnly
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(MAX(p.quantity), 0)
        FROM Product p
        WHERE p.isDeleted = false
          AND p.category.id = :categoryId
    """)
    Integer findMaxQuantityByCategoryId(@org.springframework.data.repository.query.Param("categoryId") Long categoryId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(MAX(p.quantity), 0)
        FROM Product p
        WHERE p.isDeleted = false
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:lowStockOnly = false OR p.quantity <= p.minStockLevel)
    """)
    Integer findCatalogMaxQuantity(
        @org.springframework.data.repository.query.Param("categoryId") Long categoryId,
        @org.springframework.data.repository.query.Param("lowStockOnly") boolean lowStockOnly
    );
}
