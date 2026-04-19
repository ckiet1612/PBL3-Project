package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.AccountAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AccountAuditLogRepository extends JpaRepository<AccountAuditLog, Long>, JpaSpecificationExecutor<AccountAuditLog> {

    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT a.actor.username
        FROM AccountAuditLog a
        WHERE a.actor IS NOT NULL
        ORDER BY a.actor.username
    """)
    java.util.List<String> findDistinctActorUsernames();

    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT a.targetUser.username
        FROM AccountAuditLog a
        WHERE a.targetUser IS NOT NULL
        ORDER BY a.targetUser.username
    """)
    java.util.List<String> findDistinctTargetUsernames();

    @Modifying
    @Transactional
    @Query("UPDATE AccountAuditLog a SET a.createdAt = :createdAt WHERE a.id = :id")
    void overrideCreatedAt(
        @org.springframework.data.repository.query.Param("id") Long id,
        @org.springframework.data.repository.query.Param("createdAt") java.time.LocalDateTime createdAt
    );
}
