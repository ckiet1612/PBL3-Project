package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "inventory_transactions",
    indexes = {
        @Index(name = "idx_inventory_transactions_product_created_id", columnList = "product_id, created_at, id"),
        @Index(name = "idx_inventory_transactions_order_id", columnList = "order_id"),
        @Index(name = "idx_inventory_transactions_import_order_id", columnList = "import_order_id"),
        @Index(name = "idx_inventory_transactions_user_id", columnList = "user_id"),
        @Index(name = "idx_inventory_transactions_type_created", columnList = "transaction_type, created_at")
    }
)
@Check(constraints = "unit_cost_snapshot is null or unit_cost_snapshot >= 0")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantityChange;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 50)
    private InventoryTransactionType transactionType;

    @Column(name = "reference_id")
    private Long referenceId; // order_id or import_order_id

    @ManyToOne
    @JoinColumn(name = "order_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;

    @ManyToOne
    @JoinColumn(name = "import_order_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private ImportOrder importOrder;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Who made the action

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(precision = 19, scale = 2)
    private BigDecimal unitCostSnapshot;

    @Column(precision = 19, scale = 2)
    private BigDecimal inventoryValueChange;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public InventoryTransaction() {}

    public InventoryTransaction(
        Product product,
        Integer quantityChange,
        InventoryTransactionType transactionType,
        Long referenceId,
        Order order,
        ImportOrder importOrder,
        User user,
        String notes,
        BigDecimal unitCostSnapshot,
        BigDecimal inventoryValueChange
    ) {
        this.product = product;
        this.quantityChange = quantityChange;
        this.transactionType = transactionType;
        this.referenceId = referenceId;
        this.order = order;
        this.importOrder = importOrder;
        this.user = user;
        this.notes = notes;
        this.unitCostSnapshot = unitCostSnapshot;
        this.inventoryValueChange = inventoryValueChange;
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }

    public InventoryTransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(InventoryTransactionType transactionType) { this.transactionType = transactionType; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public ImportOrder getImportOrder() { return importOrder; }
    public void setImportOrder(ImportOrder importOrder) { this.importOrder = importOrder; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal getUnitCostSnapshot() { return unitCostSnapshot; }
    public void setUnitCostSnapshot(BigDecimal unitCostSnapshot) { this.unitCostSnapshot = unitCostSnapshot; }

    public BigDecimal getInventoryValueChange() { return inventoryValueChange; }
    public void setInventoryValueChange(BigDecimal inventoryValueChange) { this.inventoryValueChange = inventoryValueChange; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
