package com.pbl3.project.pbl3_project.dto;

import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.service.MoneySupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreatedDto(
    Long id,
    BigDecimal totalPrice,
    PaymentMethod paymentMethod,
    OrderStatus status,
    LocalDateTime createdAt,
    Long salesShiftId
) {
    public static OrderCreatedDto from(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderCreatedDto(
            order.getId(),
            MoneySupport.normalize(order.getTotalPrice()),
            order.getPaymentMethod(),
            order.getStatus(),
            order.getCreatedAt(),
            order.getSalesShift() != null ? order.getSalesShift().getId() : null
        );
    }
}
