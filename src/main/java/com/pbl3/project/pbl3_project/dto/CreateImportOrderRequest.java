package com.pbl3.project.pbl3_project.dto;

import java.util.List;

public class CreateImportOrderRequest {
    private Long supplierId;
    private Long userId;
    private String notes;
    private List<ImportOrderItemRequest> items;

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<ImportOrderItemRequest> getItems() { return items; }
    public void setItems(List<ImportOrderItemRequest> items) { this.items = items; }

    public static class ImportOrderItemRequest {
        private Long productId;
        private Integer quantity;
        private Double importPrice;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Double getImportPrice() { return importPrice; }
        public void setImportPrice(Double importPrice) { this.importPrice = importPrice; }
    }
}
