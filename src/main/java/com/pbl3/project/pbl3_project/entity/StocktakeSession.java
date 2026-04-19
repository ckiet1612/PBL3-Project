package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
    name = "stocktake_sessions",
    indexes = {
        @Index(name = "idx_stocktake_sessions_created_at", columnList = "created_at"),
        @Index(name = "idx_stocktake_sessions_status", columnList = "status"),
        @Index(name = "idx_stocktake_sessions_created_by_user_id", columnList = "created_by_user_id"),
        @Index(name = "idx_stocktake_sessions_category_id", columnList = "category_id")
    }
)
public class StocktakeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private StocktakeScopeType scopeType;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private StocktakeSessionStatus status = StocktakeSessionStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime appliedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StocktakeItem> items;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public StocktakeScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(StocktakeScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public StocktakeSessionStatus getStatus() {
        return status;
    }

    public void setStatus(StocktakeSessionStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public List<StocktakeItem> getItems() {
        return items;
    }

    public void setItems(List<StocktakeItem> items) {
        this.items = items;
    }
}
