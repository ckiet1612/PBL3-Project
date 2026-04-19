package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "expenses",
    indexes = {
        @Index(name = "idx_expenses_spent_on", columnList = "spent_on"),
        @Index(name = "idx_expenses_category", columnList = "category"),
        @Index(name = "idx_expenses_created_by_user", columnList = "created_by_user_id"),
        @Index(name = "idx_expenses_spent_on_category", columnList = "spent_on, category")
    }
)
@Check(constraints = "title <> '' and amount > 0")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate spentOn;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private ExpenseCategory category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(columnDefinition = "TEXT")
    private String note;

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

    public LocalDate getSpentOn() {
        return spentOn;
    }

    public void setSpentOn(LocalDate spentOn) {
        this.spentOn = spentOn;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
