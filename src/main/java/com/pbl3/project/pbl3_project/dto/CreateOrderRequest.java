package com.pbl3.project.pbl3_project.dto;

public class CreateOrderRequest {
    private Long userId;
    private com.pbl3.project.pbl3_project.entity.PaymentMethod paymentMethod;
    private java.util.ArrayList<OrderItemRequest> items;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public com.pbl3.project.pbl3_project.entity.PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(com.pbl3.project.pbl3_project.entity.PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public java.util.ArrayList<OrderItemRequest> getItems() { return items; }
    public void setItems(java.util.ArrayList<OrderItemRequest> items) { this.items = items; }

    public static class OrderItemRequest {
        private Long productId;
        private Integer quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
