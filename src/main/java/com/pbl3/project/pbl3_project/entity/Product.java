package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_category_id", columnList = "category_id"),
        @Index(name = "idx_products_brand_id", columnList = "brand_id")
    }
)
@Check(constraints = "quantity >= 0 and price >= 0 and (import_price is null or import_price >= 0) and min_stock_level >= 0")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    private String imageUrl;

    @Column(unique = true)
    private String sku;

    private String barcode;

    @Column(precision = 19, scale = 2)
    private BigDecimal importPrice;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne
    @JoinColumn(name = "origin_id")
    private Origin origin;

    @ManyToOne
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column(name = "min_stock_level", nullable = false, columnDefinition = "INTEGER DEFAULT 10")
    private Integer minStockLevel = 10;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public Product() {}

    public Product(Long id, String name, String description, BigDecimal price, BigDecimal importPrice, Integer quantity, String imageUrl,
                   Category category, Brand brand, Origin origin, Unit unit, 
                   String sku, String barcode, boolean isDeleted) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.importPrice = importPrice;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.category = category;
        this.brand = brand;
        this.origin = origin;
        this.unit = unit;
        this.sku = sku;
        this.barcode = barcode;
        this.isDeleted = isDeleted;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getImportPrice() { return importPrice; }
    public void setImportPrice(BigDecimal importPrice) { this.importPrice = importPrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }

    public Origin getOrigin() { return origin; }
    public void setOrigin(Origin origin) { this.origin = origin; }

    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public Integer getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(Integer minStockLevel) { this.minStockLevel = minStockLevel; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
