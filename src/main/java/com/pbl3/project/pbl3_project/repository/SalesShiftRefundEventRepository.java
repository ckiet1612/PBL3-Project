package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.SalesShiftRefundEvent;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesShiftRefundEventRepository extends JpaRepository<SalesShiftRefundEvent, Long> {

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM SalesShiftRefundEvent e WHERE e.shift.id = :shiftId")
    BigDecimal sumAmountByShiftId(@Param("shiftId") Long shiftId);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM SalesShiftRefundEvent e
        WHERE e.shift.id = :shiftId
          AND e.paymentMethod = :paymentMethod
    """)
    BigDecimal sumAmountByShiftIdAndPaymentMethod(
        @Param("shiftId") Long shiftId,
        @Param("paymentMethod") PaymentMethod paymentMethod
    );

    long countByShiftId(Long shiftId);
}
