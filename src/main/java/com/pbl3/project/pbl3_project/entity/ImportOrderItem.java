package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Table(
    name = "import_order_items",
    indexes = {
        @Index(name = "idx_import_order_items_import_order_id", columnList = "import_order_id"),
        @Index(name = "idx_import_order_items_product_id", columnList = "product_id")
    }
)
@Check(constraints = "quantity > 0 and import_price >= 0")
public class ImportOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "import_order_id")
    private ImportOrder importOrder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal importPrice;

    private String productNameSnapshot;

    private String skuSnapshot;

    private String barcodeSnapshot;

    private String categoryNameSnapshot;

    private String brandNameSnapshot;

    private String originNameSnapshot;

    private String unitNameSnapshot;

    public ImportOrderItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ImportOrder getImportOrder() { return importOrder; }
    public void setImportOrder(ImportOrder importOrder) { this.importOrder = importOrder; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getImportPrice() { return importPrice; }
    public void setImportPrice(BigDecimal importPrice) { this.importPrice = importPrice; }

    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }

    public String getSkuSnapshot() { return skuSnapshot; }
    public void setSkuSnapshot(String skuSnapshot) { this.skuSnapshot = skuSnapshot; }

    public String getBarcodeSnapshot() { return barcodeSnapshot; }
    public void setBarcodeSnapshot(String barcodeSnapshot) { this.barcodeSnapshot = barcodeSnapshot; }

    public String getCategoryNameSnapshot() { return categoryNameSnapshot; }
    public void setCategoryNameSnapshot(String categoryNameSnapshot) { this.categoryNameSnapshot = categoryNameSnapshot; }

    public String getBrandNameSnapshot() { return brandNameSnapshot; }
    public void setBrandNameSnapshot(String brandNameSnapshot) { this.brandNameSnapshot = brandNameSnapshot; }

    public String getOriginNameSnapshot() { return originNameSnapshot; }
    public void setOriginNameSnapshot(String originNameSnapshot) { this.originNameSnapshot = originNameSnapshot; }

    public String getUnitNameSnapshot() { return unitNameSnapshot; }
    public void setUnitNameSnapshot(String unitNameSnapshot) { this.unitNameSnapshot = unitNameSnapshot; }

    @Transient
    public String getProductDisplayName() {
        if (productNameSnapshot != null && !productNameSnapshot.isBlank()) {
            return productNameSnapshot;
        }
        return product != null && product.getName() != null && !product.getName().isBlank()
            ? product.getName()
            : "Unknown";
    }
}
