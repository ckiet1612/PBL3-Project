package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.dto.IdLabelOption;
import com.pbl3.project.pbl3_project.entity.SalesShift;
import com.pbl3.project.pbl3_project.entity.SalesShiftStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesShiftRepository extends JpaRepository<SalesShift, Long>, JpaSpecificationExecutor<SalesShift> {

    Optional<SalesShift> findFirstByOpenedByIdAndStatusOrderByOpenedAtDesc(Long userId, SalesShiftStatus status);

    boolean existsByOpenedByIdAndStatus(Long userId, SalesShiftStatus status);

    @Query("""
        SELECT s
        FROM SalesShift s
        LEFT JOIN FETCH s.openedBy
        WHERE s.status = :status
          AND s.openedAt <= :openedBefore
    """)
    List<SalesShift> findAllByStatusAndOpenedAtBeforeWithUser(
        @Param("status") SalesShiftStatus status,
        @Param("openedBefore") LocalDateTime openedBefore
    );

    @Query("""
        SELECT DISTINCT new com.pbl3.project.pbl3_project.dto.IdLabelOption(
            s.openedBy.id,
            CONCAT(
                COALESCE(NULLIF(s.openedBy.fullName, ''), 'System'),
                ' (',
                COALESCE(NULLIF(s.openedBy.username, ''), 'system'),
                ')'
            )
        )
        FROM SalesShift s
        WHERE s.openedBy IS NOT NULL
        ORDER BY 2
    """)
    List<IdLabelOption> findDistinctOpenedByOptions();

    @Query("""
        SELECT DISTINCT new com.pbl3.project.pbl3_project.dto.IdLabelOption(
            s.openedBy.id,
            CONCAT(
                COALESCE(NULLIF(s.openedBy.fullName, ''), 'System'),
                ' (',
                COALESCE(NULLIF(s.openedBy.username, ''), 'system'),
                ')'
            )
        )
        FROM SalesShift s
        WHERE s.openedBy.id = :userId
    """)
    List<IdLabelOption> findDistinctOpenedByOptionsForUser(@Param("userId") Long userId);
}
