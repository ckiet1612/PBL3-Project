package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "account_audit_logs",
    indexes = {
        @Index(name = "idx_account_audit_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_account_audit_logs_actor_user_id", columnList = "actor_user_id"),
        @Index(name = "idx_account_audit_logs_target_user_id", columnList = "target_user_id")
    }
)
public class AccountAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 50)
    private AccountAuditAction action;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }

    public User getTargetUser() { return targetUser; }
    public void setTargetUser(User targetUser) { this.targetUser = targetUser; }

    public AccountAuditAction getAction() { return action; }
    public void setAction(AccountAuditAction action) { this.action = action; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
