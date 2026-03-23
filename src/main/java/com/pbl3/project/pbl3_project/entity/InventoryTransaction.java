package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantityChange;

    @Column(nullable = false, length = 50)
    private String transactionType; // IMPORT, SALE, MANUAL_ADJUST, DELETE, RETURN

    @Column(name = "reference_id")
    private Long referenceId; // order_id or import_order_id

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Who made the action

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public InventoryTransaction() {}

    public InventoryTransaction(Product product, Integer quantityChange, String transactionType, Long referenceId, User user, String notes) {
        this.product = product;
        this.quantityChange = quantityChange;
        this.transactionType = transactionType;
        this.referenceId = referenceId;
        this.user = user;
        this.notes = notes;
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
