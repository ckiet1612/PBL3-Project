package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Table(
    name = "order_items",
    indexes = {
        @Index(name = "idx_order_items_order_id", columnList = "order_id"),
        @Index(name = "idx_order_items_product_id", columnList = "product_id")
    }
)
@Check(constraints = "quantity > 0 and returned_quantity >= 0 and returned_quantity <= quantity and price >= 0 and (cost_at_sale is null or cost_at_sale >= 0)")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price; // Price at the time of purchase

    @Column(precision = 19, scale = 2)
    private BigDecimal originalUnitPrice;

    @Column(precision = 19, scale = 2)
    private BigDecimal linePromotionDiscountAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal orderLevelDiscountAllocatedAmount;

    private Long appliedProductPromotionIdSnapshot;

    private String appliedProductPromotionNameSnapshot;

    @Column(precision = 19, scale = 2)
    private BigDecimal costAtSale;

    @Column(nullable = false)
    private Integer returnedQuantity = 0;

    private String productNameSnapshot;

    private String skuSnapshot;

    private String barcodeSnapshot;

    private String categoryNameSnapshot;

    private String brandNameSnapshot;

    private String originNameSnapshot;

    private String unitNameSnapshot;

    public OrderItem() {}

    public OrderItem(Long id, Order order, Product product, Integer quantity, BigDecimal price) {
        this.id = id;
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getOriginalUnitPrice() { return originalUnitPrice; }
    public void setOriginalUnitPrice(BigDecimal originalUnitPrice) { this.originalUnitPrice = originalUnitPrice; }

    public BigDecimal getLinePromotionDiscountAmount() { return linePromotionDiscountAmount; }
    public void setLinePromotionDiscountAmount(BigDecimal linePromotionDiscountAmount) { this.linePromotionDiscountAmount = linePromotionDiscountAmount; }

    public BigDecimal getOrderLevelDiscountAllocatedAmount() { return orderLevelDiscountAllocatedAmount; }
    public void setOrderLevelDiscountAllocatedAmount(BigDecimal orderLevelDiscountAllocatedAmount) { this.orderLevelDiscountAllocatedAmount = orderLevelDiscountAllocatedAmount; }

    public Long getAppliedProductPromotionIdSnapshot() { return appliedProductPromotionIdSnapshot; }
    public void setAppliedProductPromotionIdSnapshot(Long appliedProductPromotionIdSnapshot) { this.appliedProductPromotionIdSnapshot = appliedProductPromotionIdSnapshot; }

    public String getAppliedProductPromotionNameSnapshot() { return appliedProductPromotionNameSnapshot; }
    public void setAppliedProductPromotionNameSnapshot(String appliedProductPromotionNameSnapshot) { this.appliedProductPromotionNameSnapshot = appliedProductPromotionNameSnapshot; }

    public BigDecimal getCostAtSale() { return costAtSale; }
    public void setCostAtSale(BigDecimal costAtSale) { this.costAtSale = costAtSale; }

    public Integer getReturnedQuantity() { return returnedQuantity; }
    public void setReturnedQuantity(Integer returnedQuantity) { this.returnedQuantity = returnedQuantity; }

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
    public BigDecimal getOriginalUnitPriceSnapshot() {
        return com.pbl3.project.pbl3_project.service.MoneySupport.normalize(originalUnitPrice != null ? originalUnitPrice : price);
    }

    @Transient
    public BigDecimal getLinePromotionDiscountAmountSnapshot() {
        return com.pbl3.project.pbl3_project.service.MoneySupport.normalize(linePromotionDiscountAmount);
    }

    @Transient
    public BigDecimal getOrderLevelDiscountAllocatedAmountSnapshot() {
        return com.pbl3.project.pbl3_project.service.MoneySupport.normalize(orderLevelDiscountAllocatedAmount);
    }

    @Transient
    public BigDecimal getLineGrossAmount() {
        return com.pbl3.project.pbl3_project.service.MoneySupport.multiply(
            getOriginalUnitPriceSnapshot(),
            quantity != null ? quantity : 0
        );
    }

    @Transient
    public BigDecimal getLineSubtotalBeforeOrderDiscount() {
        return com.pbl3.project.pbl3_project.service.MoneySupport.multiply(
            price,
            quantity != null ? quantity : 0
        );
    }

    @Transient
    public BigDecimal getLineNetAmount() {
        BigDecimal net = com.pbl3.project.pbl3_project.service.MoneySupport.subtract(
            getLineSubtotalBeforeOrderDiscount(),
            getOrderLevelDiscountAllocatedAmountSnapshot()
        );
        return net.compareTo(com.pbl3.project.pbl3_project.service.MoneySupport.ZERO) < 0
            ? com.pbl3.project.pbl3_project.service.MoneySupport.ZERO
            : net;
    }

    @Transient
    public BigDecimal getOrderLevelDiscountAllocatedForQuantity(int requestedQuantity) {
        int ordered = quantity != null ? quantity : 0;
        if (ordered <= 0 || requestedQuantity <= 0) {
            return com.pbl3.project.pbl3_project.service.MoneySupport.ZERO;
        }
        int clamped = Math.min(requestedQuantity, ordered);
        BigDecimal totalAllocated = getOrderLevelDiscountAllocatedAmountSnapshot();
        if (clamped == ordered) {
            return totalAllocated;
        }
        return totalAllocated
            .multiply(BigDecimal.valueOf(clamped))
            .divide(BigDecimal.valueOf(ordered), com.pbl3.project.pbl3_project.service.MoneySupport.MONEY_SCALE, com.pbl3.project.pbl3_project.service.MoneySupport.MONEY_ROUNDING);
    }

    @Transient
    public BigDecimal calculateRefundForReturnedQuantity(int totalReturnedQuantity) {
        int ordered = quantity != null ? quantity : 0;
        int clamped = Math.max(0, Math.min(totalReturnedQuantity, ordered));
        if (clamped <= 0) {
            return com.pbl3.project.pbl3_project.service.MoneySupport.ZERO;
        }
        BigDecimal subtotalAfterProductDiscount = com.pbl3.project.pbl3_project.service.MoneySupport.multiply(price, clamped);
        BigDecimal allocatedOrderDiscount = getOrderLevelDiscountAllocatedForQuantity(clamped);
        BigDecimal refund = com.pbl3.project.pbl3_project.service.MoneySupport.subtract(subtotalAfterProductDiscount, allocatedOrderDiscount);
        return refund.compareTo(com.pbl3.project.pbl3_project.service.MoneySupport.ZERO) < 0
            ? com.pbl3.project.pbl3_project.service.MoneySupport.ZERO
            : refund;
    }

    @Transient
    public BigDecimal getNetRevenueForQuantity(int netQuantity) {
        int ordered = quantity != null ? quantity : 0;
        int clamped = Math.max(0, Math.min(netQuantity, ordered));
        if (clamped <= 0) {
            return com.pbl3.project.pbl3_project.service.MoneySupport.ZERO;
        }
        BigDecimal revenueBeforeOrderDiscount = com.pbl3.project.pbl3_project.service.MoneySupport.multiply(price, clamped);
        BigDecimal orderDiscount = getOrderLevelDiscountAllocatedForQuantity(clamped);
        BigDecimal revenue = com.pbl3.project.pbl3_project.service.MoneySupport.subtract(revenueBeforeOrderDiscount, orderDiscount);
        return revenue.compareTo(com.pbl3.project.pbl3_project.service.MoneySupport.ZERO) < 0
            ? com.pbl3.project.pbl3_project.service.MoneySupport.ZERO
            : revenue;
    }

    @Transient
    public int getReturnableQuantity() {
        int ordered = quantity != null ? quantity : 0;
        int returned = returnedQuantity != null ? returnedQuantity : 0;
        return Math.max(0, ordered - returned);
    }

    @Transient
    public String getProductDisplayName() {
        if (productNameSnapshot != null && !productNameSnapshot.isBlank()) {
            return productNameSnapshot;
        }
        return product != null && product.getName() != null && !product.getName().isBlank()
            ? product.getName()
            : "Unknown";
    }

    @Transient
    public String getCategoryDisplayName() {
        if (categoryNameSnapshot != null && !categoryNameSnapshot.isBlank()) {
            return categoryNameSnapshot;
        }
        return product != null && product.getCategory() != null && product.getCategory().getName() != null
            ? product.getCategory().getName()
            : "Uncategorized";
    }
}
