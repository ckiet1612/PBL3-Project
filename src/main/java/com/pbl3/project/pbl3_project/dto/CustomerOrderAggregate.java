package com.pbl3.project.pbl3_project.dto;

import com.pbl3.project.pbl3_project.service.MoneySupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerOrderAggregate(
    Long customerId,
    long orderCount,
    BigDecimal totalSpent,
    LocalDateTime lastPurchase
) {
    public CustomerOrderAggregate {
        totalSpent = MoneySupport.normalize(totalSpent);
    }

    public static CustomerOrderAggregate empty(Long customerId) {
        return new CustomerOrderAggregate(customerId, 0L, MoneySupport.ZERO, null);
    }
}
