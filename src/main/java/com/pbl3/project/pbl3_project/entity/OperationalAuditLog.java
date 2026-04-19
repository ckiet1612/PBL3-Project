package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "operational_audit_logs",
    indexes = {
        @Index(name = "idx_operational_audit_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_operational_audit_logs_actor_user_id", columnList = "actor_user_id"),
        @Index(name = "idx_operational_audit_logs_action_created", columnList = "action, created_at"),
        @Index(name = "idx_operational_audit_logs_subject_type", columnList = "subject_type")
    }
)
public class OperationalAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 50)
    private OperationalAuditAction action;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 50)
    private OperationalSubjectType subjectType;

    @Column(nullable = false)
    private Long subjectId;

    private String subjectLabel;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public OperationalAuditAction getAction() {
        return action;
    }

    public void setAction(OperationalAuditAction action) {
        this.action = action;
    }

    public OperationalSubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(OperationalSubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectLabel() {
        return subjectLabel;
    }

    public void setSubjectLabel(String subjectLabel) {
        this.subjectLabel = subjectLabel;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
