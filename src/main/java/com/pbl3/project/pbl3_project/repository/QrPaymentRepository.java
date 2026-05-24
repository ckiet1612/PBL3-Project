package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.QrPayment;
import com.pbl3.project.pbl3_project.entity.QrPaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QrPaymentRepository extends JpaRepository<QrPayment, Long> {
    Optional<QrPayment> findByOrderCode(Long orderCode);

    Optional<QrPayment> findByPaymentLinkId(String paymentLinkId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from QrPayment payment left join fetch payment.createdOrder where payment.orderCode = :orderCode")
    Optional<QrPayment> findByOrderCodeForUpdate(@Param("orderCode") Long orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from QrPayment payment left join fetch payment.createdOrder where payment.paymentLinkId = :paymentLinkId")
    Optional<QrPayment> findByPaymentLinkIdForUpdate(@Param("paymentLinkId") String paymentLinkId);

    @Query("""
        SELECT payment
        FROM QrPayment payment
        LEFT JOIN FETCH payment.user
        WHERE payment.status = :status
    """)
    List<QrPayment> findAllByStatusWithUser(@Param("status") QrPaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from QrPayment payment left join fetch payment.createdOrder where payment.id = :id")
    Optional<QrPayment> findByIdForUpdate(@Param("id") Long id);
}
