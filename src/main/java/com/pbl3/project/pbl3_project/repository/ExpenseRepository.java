package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.dto.IdLabelOption;
import com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow;
import com.pbl3.project.pbl3_project.entity.Expense;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    @Query("SELECT COALESCE(MAX(e.amount), 0) FROM Expense e")
    BigDecimal findMaxAmount();

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.spentOn = :spentOn
    """)
    BigDecimal sumAmountOn(@org.springframework.data.repository.query.Param("spentOn") LocalDate spentOn);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE (:startDate IS NULL OR e.spentOn >= :startDate)
          AND (:endDate IS NULL OR e.spentOn <= :endDate)
    """)
    BigDecimal sumAmountBetween(
        @org.springframework.data.repository.query.Param("startDate") LocalDate startDate,
        @org.springframework.data.repository.query.Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT new com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow(
            e.category,
            COALESCE(SUM(e.amount), 0),
            COUNT(e)
        )
        FROM Expense e
        WHERE (:startDate IS NULL OR e.spentOn >= :startDate)
          AND (:endDate IS NULL OR e.spentOn <= :endDate)
        GROUP BY e.category
    """)
    List<ExpenseCategorySummaryRow> findCategorySummariesBetween(
        @org.springframework.data.repository.query.Param("startDate") LocalDate startDate,
        @org.springframework.data.repository.query.Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT DISTINCT new com.pbl3.project.pbl3_project.dto.IdLabelOption(
            e.createdBy.id,
            CONCAT(
                COALESCE(NULLIF(e.createdBy.fullName, ''), 'System'),
                ' (',
                COALESCE(NULLIF(e.createdBy.username, ''), 'system'),
                ')'
            )
        )
        FROM Expense e
        WHERE e.createdBy IS NOT NULL
    """)
    List<IdLabelOption> findDistinctCreatorOptions();

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.createdBy.id = :userId
          AND e.createdAt >= :start
          AND e.createdAt <= :end
    """)
    BigDecimal sumAmountCreatedByUserBetween(
        @org.springframework.data.repository.query.Param("userId") Long userId,
        @org.springframework.data.repository.query.Param("start") LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.createdBy.id = :userId
          AND e.createdAt >= :start
          AND e.createdAt <= :end
          AND e.paymentMethod = :paymentMethod
    """)
    BigDecimal sumAmountCreatedByUserBetweenAndPaymentMethod(
        @org.springframework.data.repository.query.Param("userId") Long userId,
        @org.springframework.data.repository.query.Param("start") LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") LocalDateTime end,
        @org.springframework.data.repository.query.Param("paymentMethod") PaymentMethod paymentMethod
    );
}
