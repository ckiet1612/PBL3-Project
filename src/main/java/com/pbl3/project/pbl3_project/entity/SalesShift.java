package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "sales_shifts",
    indexes = {
        @Index(name = "idx_sales_shifts_opened_by_status", columnList = "opened_by_user_id, status"),
        @Index(name = "idx_sales_shifts_opened_at", columnList = "opened_at"),
        @Index(name = "idx_sales_shifts_closed_at", columnList = "closed_at"),
        @Index(name = "idx_sales_shifts_status", columnList = "status")
    }
)
@Check(constraints = "opening_cash_amount >= 0")
public class SalesShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "opened_by_user_id")
    private User openedBy;

    private String openedByNameSnapshot;

    private String openedByUsernameSnapshot;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal openingCashAmount;

    @Column(columnDefinition = "TEXT")
    private String openNote;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SalesShiftStatus status = SalesShiftStatus.OPEN;

    private LocalDateTime closedAt;

    @ManyToOne
    @JoinColumn(name = "closed_by_user_id")
    private User closedBy;

    private String closedByNameSnapshot;

    private String closedByUsernameSnapshot;

    @Column(precision = 19, scale = 2)
    private BigDecimal closingCashActual;

    @Column(precision = 19, scale = 2)
    private BigDecimal expectedCashAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal cashVarianceAmount;

    @Column(columnDefinition = "TEXT")
    private String closeNote;

    @Column(precision = 19, scale = 2)
    private BigDecimal salesRevenueAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal expenseAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal cashSalesAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal cashRefundAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal cashExpenseAmount;

    private Long orderCount;

    private Long refundCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getOpenedBy() { return openedBy; }
    public void setOpenedBy(User openedBy) { this.openedBy = openedBy; }

    public String getOpenedByNameSnapshot() { return openedByNameSnapshot; }
    public void setOpenedByNameSnapshot(String openedByNameSnapshot) { this.openedByNameSnapshot = openedByNameSnapshot; }

    public String getOpenedByUsernameSnapshot() { return openedByUsernameSnapshot; }
    public void setOpenedByUsernameSnapshot(String openedByUsernameSnapshot) { this.openedByUsernameSnapshot = openedByUsernameSnapshot; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public BigDecimal getOpeningCashAmount() { return openingCashAmount; }
    public void setOpeningCashAmount(BigDecimal openingCashAmount) { this.openingCashAmount = openingCashAmount; }

    public String getOpenNote() { return openNote; }
    public void setOpenNote(String openNote) { this.openNote = openNote; }

    public SalesShiftStatus getStatus() { return status; }
    public void setStatus(SalesShiftStatus status) { this.status = status; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public User getClosedBy() { return closedBy; }
    public void setClosedBy(User closedBy) { this.closedBy = closedBy; }

    public String getClosedByNameSnapshot() { return closedByNameSnapshot; }
    public void setClosedByNameSnapshot(String closedByNameSnapshot) { this.closedByNameSnapshot = closedByNameSnapshot; }

    public String getClosedByUsernameSnapshot() { return closedByUsernameSnapshot; }
    public void setClosedByUsernameSnapshot(String closedByUsernameSnapshot) { this.closedByUsernameSnapshot = closedByUsernameSnapshot; }

    public BigDecimal getClosingCashActual() { return closingCashActual; }
    public void setClosingCashActual(BigDecimal closingCashActual) { this.closingCashActual = closingCashActual; }

    public BigDecimal getExpectedCashAmount() { return expectedCashAmount; }
    public void setExpectedCashAmount(BigDecimal expectedCashAmount) { this.expectedCashAmount = expectedCashAmount; }

    public BigDecimal getCashVarianceAmount() { return cashVarianceAmount; }
    public void setCashVarianceAmount(BigDecimal cashVarianceAmount) { this.cashVarianceAmount = cashVarianceAmount; }

    public String getCloseNote() { return closeNote; }
    public void setCloseNote(String closeNote) { this.closeNote = closeNote; }

    public BigDecimal getSalesRevenueAmount() { return salesRevenueAmount; }
    public void setSalesRevenueAmount(BigDecimal salesRevenueAmount) { this.salesRevenueAmount = salesRevenueAmount; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public BigDecimal getExpenseAmount() { return expenseAmount; }
    public void setExpenseAmount(BigDecimal expenseAmount) { this.expenseAmount = expenseAmount; }

    public BigDecimal getCashSalesAmount() { return cashSalesAmount; }
    public void setCashSalesAmount(BigDecimal cashSalesAmount) { this.cashSalesAmount = cashSalesAmount; }

    public BigDecimal getCashRefundAmount() { return cashRefundAmount; }
    public void setCashRefundAmount(BigDecimal cashRefundAmount) { this.cashRefundAmount = cashRefundAmount; }

    public BigDecimal getCashExpenseAmount() { return cashExpenseAmount; }
    public void setCashExpenseAmount(BigDecimal cashExpenseAmount) { this.cashExpenseAmount = cashExpenseAmount; }

    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }

    public Long getRefundCount() { return refundCount; }
    public void setRefundCount(Long refundCount) { this.refundCount = refundCount; }
}
