package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
    name = "import_orders",
    indexes = {
        @Index(name = "idx_import_orders_created_at", columnList = "created_at"),
        @Index(name = "idx_import_orders_status", columnList = "status"),
        @Index(name = "idx_import_orders_supplier_created_at", columnList = "supplier_id, created_at")
    }
)
@Check(constraints = "total_cost >= 0")
public class ImportOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ImportOrderStatus status = ImportOrderStatus.COMPLETED;
    
    @Column(columnDefinition = "TEXT")
    private String statusNote;
    
    private String notes;

    private String createdByNameSnapshot;

    private String supplierNameSnapshot;

    @OneToMany(mappedBy = "importOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportOrderItem> items;

    public ImportOrder() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public ImportOrderStatus getStatus() { return status; }
    public void setStatus(ImportOrderStatus status) { this.status = status; }

    public String getStatusNote() { return statusNote; }
    public void setStatusNote(String statusNote) { this.statusNote = statusNote; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedByNameSnapshot() { return createdByNameSnapshot; }
    public void setCreatedByNameSnapshot(String createdByNameSnapshot) { this.createdByNameSnapshot = createdByNameSnapshot; }

    public String getSupplierNameSnapshot() { return supplierNameSnapshot; }
    public void setSupplierNameSnapshot(String supplierNameSnapshot) { this.supplierNameSnapshot = supplierNameSnapshot; }

    public List<ImportOrderItem> getItems() { return items; }
    public void setItems(List<ImportOrderItem> items) { this.items = items; }

    @Transient
    public String getCreatedByDisplayName() {
        if (createdByNameSnapshot != null && !createdByNameSnapshot.isBlank()) {
            return createdByNameSnapshot;
        }
        if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return "System";
    }

    @Transient
    public String getSupplierDisplayName() {
        if (supplierNameSnapshot != null && !supplierNameSnapshot.isBlank()) {
            return supplierNameSnapshot;
        }
        return supplier != null && supplier.getName() != null && !supplier.getName().isBlank()
            ? supplier.getName()
            : "-";
    }
}
