package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "promotions",
    indexes = {
        @Index(name = "idx_promotions_scope_enabled", columnList = "scope, enabled"),
        @Index(name = "idx_promotions_schedule", columnList = "starts_at, ends_at"),
        @Index(name = "idx_promotions_target_product", columnList = "target_product_id"),
        @Index(name = "idx_promotions_created_by_user", columnList = "created_by_user_id")
    }
)
@Check(constraints = "name <> '' and discount_value > 0")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PromotionScope scope;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PromotionDiscountType discountType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @ManyToOne
    @JoinColumn(name = "target_product_id")
    private Product targetProduct;

    @Column(name = "min_order_total", precision = 19, scale = 2)
    private BigDecimal minOrderTotal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PromotionScope getScope() {
        return scope;
    }

    public void setScope(PromotionScope scope) {
        this.scope = scope;
    }

    public PromotionDiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(PromotionDiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(LocalDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public Product getTargetProduct() {
        return targetProduct;
    }

    public void setTargetProduct(Product targetProduct) {
        this.targetProduct = targetProduct;
    }

    public BigDecimal getMinOrderTotal() {
        return minOrderTotal;
    }

    public void setMinOrderTotal(BigDecimal minOrderTotal) {
        this.minOrderTotal = minOrderTotal;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Transient
    public String getCreatedByDisplayName() {
        if (createdBy == null) {
            return "System";
        }
        if (createdBy.getFullName() != null && !createdBy.getFullName().isBlank()) {
            return createdBy.getFullName();
        }
        if (createdBy.getUsername() != null && !createdBy.getUsername().isBlank()) {
            return createdBy.getUsername();
        }
        return "System";
    }

    @Transient
    public PromotionLifecycleStatus getLifecycleStatus() {
        return getLifecycleStatus(LocalDateTime.now());
    }

    @Transient
    public PromotionLifecycleStatus getLifecycleStatus(LocalDateTime at) {
        LocalDateTime instant = at != null ? at : LocalDateTime.now();
        if (!enabled) {
            return PromotionLifecycleStatus.DISABLED;
        }
        if (startsAt != null && startsAt.isAfter(instant)) {
            return PromotionLifecycleStatus.SCHEDULED;
        }
        if (endsAt != null && endsAt.isBefore(instant)) {
            return PromotionLifecycleStatus.EXPIRED;
        }
        return PromotionLifecycleStatus.ACTIVE;
    }

    @Transient
    public boolean isActiveAt(LocalDateTime at) {
        return getLifecycleStatus(at) == PromotionLifecycleStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
