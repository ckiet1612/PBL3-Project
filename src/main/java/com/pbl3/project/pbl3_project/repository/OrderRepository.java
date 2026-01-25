package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
    java.util.Optional<Order> findByIdWithItems(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    Double sumRevenueBetween(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    Long countOrdersBetween(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);
}
