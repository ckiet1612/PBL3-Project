package com.pbl3.project.pbl3_project.entity;

import java.util.EnumSet;
import java.util.Set;

public enum ReturnRefundScope {
    PROCESSED("Processed", EnumSet.of(OrderStatus.PARTIALLY_RETURNED, OrderStatus.RETURNED, OrderStatus.CANCELED)),
    PROCESSED_PLUS_ELIGIBLE(
        "Processed + Eligible",
        EnumSet.of(OrderStatus.COMPLETED, OrderStatus.PARTIALLY_RETURNED, OrderStatus.RETURNED, OrderStatus.CANCELED)
    ),
    ELIGIBLE_ONLY("Eligible Only", EnumSet.of(OrderStatus.COMPLETED, OrderStatus.PARTIALLY_RETURNED));

    private final String label;
    private final EnumSet<OrderStatus> statuses;

    ReturnRefundScope(String label, EnumSet<OrderStatus> statuses) {
        this.label = label;
        this.statuses = statuses;
    }

    public String getLabel() {
        return label;
    }

    public Set<OrderStatus> getStatuses() {
        return EnumSet.copyOf(statuses);
    }
}
