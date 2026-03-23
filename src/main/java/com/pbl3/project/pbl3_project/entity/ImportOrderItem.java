package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "import_order_items")
public class ImportOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "import_order_id")
    private ImportOrder importOrder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double importPrice;

    public ImportOrderItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ImportOrder getImportOrder() { return importOrder; }
    public void setImportOrder(ImportOrder importOrder) { this.importOrder = importOrder; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getImportPrice() { return importPrice; }
    public void setImportPrice(Double importPrice) { this.importPrice = importPrice; }
}
