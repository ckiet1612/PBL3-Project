package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_created_at", columnList = "created_at"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_user_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_orders_customer_created_at", columnList = "customer_id, created_at")
    }
)
@Check(constraints = "total_price >= 0 and refunded_amount >= 0 and refunded_amount <= total_price")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(precision = 19, scale = 2)
    private BigDecimal grossSubtotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountTotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal orderLevelDiscountTotal;

    private Long appliedOrderPromotionIdSnapshot;

    private String appliedOrderPromotionNameSnapshot;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.COMPLETED;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String statusNote;

    private String createdByNameSnapshot;

    private String customerNameSnapshot;

    private String customerPhoneSnapshot;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    public Order() {}

    public Order(Long id, LocalDateTime createdAt, BigDecimal totalPrice, User user, PaymentMethod paymentMethod, List<OrderItem> orderItems) {
        this.id = id;
        this.createdAt = createdAt;
        this.totalPrice = totalPrice;
        this.user = user;
        this.paymentMethod = paymentMethod;
        this.orderItems = orderItems;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public BigDecimal getGrossSubtotal() { return grossSubtotal; }
    public void setGrossSubtotal(BigDecimal grossSubtotal) { this.grossSubtotal = grossSubtotal; }

    public BigDecimal getDiscountTotal() { return discountTotal; }
    public void setDiscountTotal(BigDecimal discountTotal) { this.discountTotal = discountTotal; }

    public BigDecimal getOrderLevelDiscountTotal() { return orderLevelDiscountTotal; }
    public void setOrderLevelDiscountTotal(BigDecimal orderLevelDiscountTotal) { this.orderLevelDiscountTotal = orderLevelDiscountTotal; }

    public Long getAppliedOrderPromotionIdSnapshot() { return appliedOrderPromotionIdSnapshot; }
    public void setAppliedOrderPromotionIdSnapshot(Long appliedOrderPromotionIdSnapshot) { this.appliedOrderPromotionIdSnapshot = appliedOrderPromotionIdSnapshot; }

    public String getAppliedOrderPromotionNameSnapshot() { return appliedOrderPromotionNameSnapshot; }
    public void setAppliedOrderPromotionNameSnapshot(String appliedOrderPromotionNameSnapshot) { this.appliedOrderPromotionNameSnapshot = appliedOrderPromotionNameSnapshot; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }

    public String getStatusNote() { return statusNote; }
    public void setStatusNote(String statusNote) { this.statusNote = statusNote; }

    public String getCreatedByNameSnapshot() { return createdByNameSnapshot; }
    public void setCreatedByNameSnapshot(String createdByNameSnapshot) { this.createdByNameSnapshot = createdByNameSnapshot; }

    public String getCustomerNameSnapshot() { return customerNameSnapshot; }
    public void setCustomerNameSnapshot(String customerNameSnapshot) { this.customerNameSnapshot = customerNameSnapshot; }

    public String getCustomerPhoneSnapshot() { return customerPhoneSnapshot; }
    public void setCustomerPhoneSnapshot(String customerPhoneSnapshot) { this.customerPhoneSnapshot = customerPhoneSnapshot; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    @Transient
    public BigDecimal getNetTotal() {
        java.math.BigDecimal gross = totalPrice != null ? totalPrice : java.math.BigDecimal.ZERO;
        java.math.BigDecimal refunded = refundedAmount != null ? refundedAmount : java.math.BigDecimal.ZERO;
        java.math.BigDecimal net = gross.subtract(refunded);
        return net.compareTo(java.math.BigDecimal.ZERO) < 0 ? java.math.BigDecimal.ZERO : net;
    }

    @Transient
    public BigDecimal getGrossSubtotalSnapshot() {
        return com.pbl3.project.pbl3_project.service.MoneySupport.normalize(grossSubtotal != null ? grossSubtotal : totalPrice);
    }

    @Transient
    public BigDecimal getDiscountTotalSnapshot() {
        return com.pbl3.project.pbl3_project.service.MoneySupport.normalize(discountTotal);
    }

    @Transient
    public BigDecimal getOrderLevelDiscountTotalSnapshot() {
        return com.pbl3.project.pbl3_project.service.MoneySupport.normalize(orderLevelDiscountTotal);
    }

    @Transient
    public BigDecimal getProductLevelDiscountTotalSnapshot() {
        java.math.BigDecimal value = getDiscountTotalSnapshot().subtract(getOrderLevelDiscountTotalSnapshot());
        return value.compareTo(java.math.BigDecimal.ZERO) < 0
            ? java.math.BigDecimal.ZERO.setScale(com.pbl3.project.pbl3_project.service.MoneySupport.MONEY_SCALE, com.pbl3.project.pbl3_project.service.MoneySupport.MONEY_ROUNDING)
            : value.setScale(com.pbl3.project.pbl3_project.service.MoneySupport.MONEY_SCALE, com.pbl3.project.pbl3_project.service.MoneySupport.MONEY_ROUNDING);
    }

    @Transient
    public boolean hasPromotionApplied() {
        return getDiscountTotalSnapshot().compareTo(com.pbl3.project.pbl3_project.service.MoneySupport.ZERO) > 0;
    }

    @Transient
    public String getCreatedByDisplayName() {
        if (createdByNameSnapshot != null && !createdByNameSnapshot.isBlank()) {
            return createdByNameSnapshot;
        }
        if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return "System";
    }

    @Transient
    public boolean hasCustomer() {
        return (customerNameSnapshot != null && !customerNameSnapshot.isBlank())
            || (customerPhoneSnapshot != null && !customerPhoneSnapshot.isBlank())
            || customer != null;
    }

    @Transient
    public String getCustomerDisplayName() {
        if (customerNameSnapshot != null && !customerNameSnapshot.isBlank()) {
            return customerNameSnapshot;
        }
        if (customer != null && customer.getFullName() != null && !customer.getFullName().isBlank()) {
            return customer.getFullName();
        }
        return "Guest";
    }

    @Transient
    public String getCustomerPhoneDisplay() {
        if (customerPhoneSnapshot != null && !customerPhoneSnapshot.isBlank()) {
            return customerPhoneSnapshot;
        }
        if (customer != null && customer.getPhone() != null && !customer.getPhone().isBlank()) {
            return customer.getPhone();
        }
        return "-";
    }
}
