package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.OperationalAuditLog;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OperationalAuditLogRepository extends JpaRepository<OperationalAuditLog, Long>, JpaSpecificationExecutor<OperationalAuditLog> {

    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT a.actor.username
        FROM OperationalAuditLog a
        WHERE a.actor IS NOT NULL
        ORDER BY a.actor.username
    """)
    java.util.List<String> findDistinctActorUsernames();

    java.util.List<OperationalAuditLog> findBySubjectTypeAndSubjectIdOrderByCreatedAtAscIdAsc(
        OperationalSubjectType subjectType,
        Long subjectId
    );

    @Modifying
    @Transactional
    @Query("UPDATE OperationalAuditLog a SET a.createdAt = :createdAt WHERE a.id = :id")
    void overrideCreatedAt(
        @org.springframework.data.repository.query.Param("id") Long id,
        @org.springframework.data.repository.query.Param("createdAt") java.time.LocalDateTime createdAt
    );
}
