package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "stocktake_items",
    indexes = {
        @Index(name = "idx_stocktake_items_session_id", columnList = "session_id"),
        @Index(name = "idx_stocktake_items_product_id", columnList = "product_id")
    }
)
public class StocktakeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private StocktakeSession session;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer systemQuantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCostSnapshot;

    private Integer countedQuantity;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StocktakeSession getSession() {
        return session;
    }

    public void setSession(StocktakeSession session) {
        this.session = session;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getSystemQuantity() {
        return systemQuantity;
    }

    public void setSystemQuantity(Integer systemQuantity) {
        this.systemQuantity = systemQuantity;
    }

    public BigDecimal getUnitCostSnapshot() {
        return unitCostSnapshot;
    }

    public void setUnitCostSnapshot(BigDecimal unitCostSnapshot) {
        this.unitCostSnapshot = unitCostSnapshot;
    }

    public Integer getCountedQuantity() {
        return countedQuantity;
    }

    public void setCountedQuantity(Integer countedQuantity) {
        this.countedQuantity = countedQuantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Transient
    public int getVarianceQuantity() {
        int counted = countedQuantity != null ? countedQuantity : 0;
        int system = systemQuantity != null ? systemQuantity : 0;
        return counted - system;
    }
}
