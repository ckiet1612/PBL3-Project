package com.pbl3.project.pbl3_project.dto.payment;

import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class QrPaymentCreateRequest {
    private Long userId;
    private Long customerId;
    private Long selectedOrderPromotionId;
    private BigDecimal amount;
    private List<CreateOrderRequest.OrderItemRequest> items = new ArrayList<>();

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getSelectedOrderPromotionId() { return selectedOrderPromotionId; }
    public void setSelectedOrderPromotionId(Long selectedOrderPromotionId) { this.selectedOrderPromotionId = selectedOrderPromotionId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public List<CreateOrderRequest.OrderItemRequest> getItems() { return items; }
    public void setItems(List<CreateOrderRequest.OrderItemRequest> items) { this.items = items; }
}
