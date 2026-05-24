package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.IdLabelOption;
import com.pbl3.project.pbl3_project.entity.OperationalAuditAction;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.SalesShift;
import com.pbl3.project.pbl3_project.entity.SalesShiftRefundEvent;
import com.pbl3.project.pbl3_project.entity.SalesShiftStatus;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.ExpenseRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.SalesShiftRefundEventRepository;
import com.pbl3.project.pbl3_project.repository.SalesShiftRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesShiftService {

    public record ShiftSummary(
        Long shiftId,
        Long openedByUserId,
        String openedByName,
        String openedByUsername,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        SalesShiftStatus status,
        BigDecimal openingCashAmount,
        BigDecimal salesRevenue,
        BigDecimal refundAmount,
        BigDecimal expenseAmount,
        BigDecimal cashSales,
        BigDecimal cashRefunds,
        BigDecimal cashExpenses,
        BigDecimal expectedCashAmount,
        BigDecimal closingCashActual,
        BigDecimal cashVarianceAmount,
        long orderCount,
        long refundCount,
        String closedByName,
        String closeNote
    ) {
    }

    public record ShiftReportRow(
        Long shiftId,
        String openedByName,
        String openedByUsername,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        SalesShiftStatus status,
        BigDecimal openingCashAmount,
        BigDecimal salesRevenue,
        BigDecimal refundAmount,
        BigDecimal expenseAmount,
        BigDecimal expectedCashAmount,
        BigDecimal closingCashActual,
        BigDecimal cashVarianceAmount,
        long orderCount,
        long refundCount,
        String closedByName,
        String closeNote
    ) {
    }

    private record ShiftTotals(
        BigDecimal salesRevenue,
        BigDecimal refundAmount,
        BigDecimal expenseAmount,
        BigDecimal cashSales,
        BigDecimal cashRefunds,
        BigDecimal cashExpenses,
        BigDecimal expectedCash,
        long orderCount,
        long refundCount
    ) {
    }

    private final SalesShiftRepository salesShiftRepository;
    private final SalesShiftRefundEventRepository refundEventRepository;
    private final OrderRepository orderRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final OperationalAuditLogService operationalAuditLogService;

    public SalesShiftService(
        SalesShiftRepository salesShiftRepository,
        SalesShiftRefundEventRepository refundEventRepository,
        OrderRepository orderRepository,
        ExpenseRepository expenseRepository,
        UserRepository userRepository,
        AuthorizationService authorizationService,
        OperationalAuditLogService operationalAuditLogService
    ) {
        this.salesShiftRepository = salesShiftRepository;
        this.refundEventRepository = refundEventRepository;
        this.orderRepository = orderRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.operationalAuditLogService = operationalAuditLogService;
    }

    @Transactional
    public SalesShift openShift(User user, BigDecimal openingCash, String note) {
        authorizationService.requireSalesShiftAccess(user);
        User managedUser = resolveUserForUpdate(user);
        if (salesShiftRepository.existsByOpenedByIdAndStatus(managedUser.getId(), SalesShiftStatus.OPEN)) {
            throw new ValidationException("Close the current shift before opening a new one");
        }
        BigDecimal normalizedOpeningCash = MoneySupport.normalize(openingCash);
        if (normalizedOpeningCash.signum() < 0) {
            throw new ValidationException("Opening cash cannot be negative");
        }

        SalesShift shift = new SalesShift();
        shift.setOpenedBy(managedUser);
        shift.setOpenedByNameSnapshot(resolveDisplayName(managedUser));
        shift.setOpenedByUsernameSnapshot(managedUser.getUsername());
        shift.setOpenedAt(LocalDateTime.now());
        shift.setOpeningCashAmount(normalizedOpeningCash);
        shift.setOpenNote(clean(note));
        shift.setStatus(SalesShiftStatus.OPEN);
        SalesShift saved = salesShiftRepository.save(shift);

        operationalAuditLogService.record(
            managedUser,
            OperationalAuditAction.SHIFT_OPENED,
            OperationalSubjectType.SALES_SHIFT,
            saved.getId(),
            "Shift #" + saved.getId(),
            "Shift opened"
        );
        return saved;
    }

    @Transactional
    public SalesShift closeOwnShift(User user, BigDecimal closingCashActual, String note) {
        authorizationService.requireSalesShiftAccess(user);
        User managedUser = resolveUser(user);
        SalesShift shift = getOpenShift(managedUser)
            .orElseThrow(() -> new ValidationException("No open shift to close"));
        return closeShift(shift, managedUser, closingCashActual, note, false);
    }

    @Transactional
    public SalesShift closeShiftAsManager(User actor, Long shiftId, BigDecimal closingCashActual, String note) {
        authorizationService.requireSalesShiftManagerAccess(actor);
        if (note == null || note.trim().isEmpty()) {
            throw new ValidationException("Manager close note is required");
        }
        User managedActor = resolveUser(actor);
        SalesShift shift = salesShiftRepository.findById(shiftId)
            .orElseThrow(() -> new ValidationException("Shift not found: " + shiftId));
        if (shift.getStatus() != SalesShiftStatus.OPEN) {
            throw new ValidationException("Only open shifts can be closed");
        }
        return closeShift(shift, managedActor, closingCashActual, note, true);
    }

    @Transactional(readOnly = true)
    public Optional<SalesShift> getOpenShift(User user) {
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }
        return salesShiftRepository.findFirstByOpenedByIdAndStatusOrderByOpenedAtDesc(user.getId(), SalesShiftStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public SalesShift requireOpenShiftForSale(User user) {
        authorizationService.requireSalesShiftAccess(user);
        return getOpenShift(user).orElseThrow(() -> new ValidationException("Open a sales shift before checkout"));
    }

    @Transactional(readOnly = true)
    public ShiftSummary getCurrentShiftSummary(User user) {
        authorizationService.requireSalesShiftAccess(user);
        return getOpenShift(user).map(this::toSummary).orElse(null);
    }

    @Transactional(readOnly = true)
    public ShiftSummary getShiftSummary(User viewer, Long shiftId) {
        SalesShift shift = salesShiftRepository.findById(shiftId)
            .orElseThrow(() -> new ValidationException("Shift not found: " + shiftId));
        requireCanViewShift(viewer, shift);
        return toSummary(shift);
    }

    @Transactional(readOnly = true)
    public List<ShiftReportRow> searchShifts(
        User viewer,
        LocalDate startDate,
        LocalDate endDate,
        Long openedByUserId,
        SalesShiftStatus status,
        Long shiftId
    ) {
        authorizationService.requireSalesShiftAccess(viewer);
        Specification<SalesShift> spec = (root, query, cb) -> {
            root.fetch("openedBy", JoinType.LEFT);
            root.fetch("closedBy", JoinType.LEFT);
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!authorizationService.canManageAllSalesShifts(viewer)) {
                predicates.add(cb.equal(root.get("openedBy").get("id"), viewer.getId()));
            } else if (openedByUserId != null) {
                predicates.add(cb.equal(root.get("openedBy").get("id"), openedByUserId));
            }
            if (shiftId != null) {
                predicates.add(cb.equal(root.get("id"), shiftId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("openedAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("openedAt"), endDate.atTime(23, 59, 59)));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return salesShiftRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "openedAt")).stream()
            .map(this::toReportRow)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<IdLabelOption> getShiftUserOptions(User viewer) {
        authorizationService.requireSalesShiftAccess(viewer);
        List<IdLabelOption> options = authorizationService.canManageAllSalesShifts(viewer)
            ? salesShiftRepository.findDistinctOpenedByOptions()
            : salesShiftRepository.findDistinctOpenedByOptionsForUser(viewer.getId());
        return options.stream()
            .sorted(Comparator.comparing(IdLabelOption::label, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
    }

    @Transactional
    public SalesShiftRefundEvent recordRefundEvent(User processor, Order order, BigDecimal amount, String reason) {
        authorizationService.requireSalesShiftAccess(processor);
        if (order == null || order.getId() == null) {
            throw new ValidationException("Order is required for refund tracking");
        }
        BigDecimal normalizedAmount = MoneySupport.normalize(amount);
        if (!MoneySupport.isPositive(normalizedAmount)) {
            throw new ValidationException("Refund amount must be positive");
        }
        User managedProcessor = resolveUser(processor);
        SalesShift shift = getOpenShift(managedProcessor)
            .orElseThrow(() -> new ValidationException("Open a sales shift before processing refunds"));

        SalesShiftRefundEvent event = new SalesShiftRefundEvent();
        event.setShift(shift);
        event.setOrder(order);
        event.setProcessedBy(managedProcessor);
        event.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod() : PaymentMethod.CASH);
        event.setAmount(normalizedAmount);
        event.setCreatedAt(LocalDateTime.now());
        event.setReason(clean(reason));
        return refundEventRepository.save(event);
    }

    private SalesShift closeShift(
        SalesShift shift,
        User closedBy,
        BigDecimal closingCashActual,
        String note,
        boolean managerClose
    ) {
        if (shift.getStatus() != SalesShiftStatus.OPEN) {
            throw new ValidationException("Only open shifts can be closed");
        }
        BigDecimal normalizedClosingCash = MoneySupport.normalize(closingCashActual);
        if (normalizedClosingCash.signum() < 0) {
            throw new ValidationException("Closing cash cannot be negative");
        }
        LocalDateTime closedAt = LocalDateTime.now();
        ShiftTotals totals = calculateLiveTotals(shift, closedAt);

        shift.setStatus(SalesShiftStatus.CLOSED);
        shift.setClosedAt(closedAt);
        shift.setClosedBy(closedBy);
        shift.setClosedByNameSnapshot(resolveDisplayName(closedBy));
        shift.setClosedByUsernameSnapshot(closedBy.getUsername());
        shift.setClosingCashActual(normalizedClosingCash);
        shift.setExpectedCashAmount(totals.expectedCash());
        shift.setCashVarianceAmount(MoneySupport.subtract(normalizedClosingCash, totals.expectedCash()));
        shift.setCloseNote(clean(note));
        shift.setSalesRevenueAmount(totals.salesRevenue());
        shift.setRefundAmount(totals.refundAmount());
        shift.setExpenseAmount(totals.expenseAmount());
        shift.setCashSalesAmount(totals.cashSales());
        shift.setCashRefundAmount(totals.cashRefunds());
        shift.setCashExpenseAmount(totals.cashExpenses());
        shift.setOrderCount(totals.orderCount());
        shift.setRefundCount(totals.refundCount());
        SalesShift saved = salesShiftRepository.save(shift);

        operationalAuditLogService.record(
            closedBy,
            managerClose ? OperationalAuditAction.SHIFT_CLOSED_BY_MANAGER : OperationalAuditAction.SHIFT_CLOSED,
            OperationalSubjectType.SALES_SHIFT,
            saved.getId(),
            "Shift #" + saved.getId(),
            managerClose ? clean(note) : "Shift closed"
        );
        return saved;
    }

    private ShiftSummary toSummary(SalesShift shift) {
        ShiftTotals totals = shift.getStatus() == SalesShiftStatus.CLOSED
            ? totalsFromSnapshot(shift)
            : calculateLiveTotals(shift, LocalDateTime.now());
        BigDecimal closingCash = MoneySupport.normalize(shift.getClosingCashActual());
        BigDecimal variance = shift.getStatus() == SalesShiftStatus.CLOSED
            ? MoneySupport.normalize(shift.getCashVarianceAmount())
            : MoneySupport.ZERO;
        return new ShiftSummary(
            shift.getId(),
            shift.getOpenedBy() != null ? shift.getOpenedBy().getId() : null,
            resolveOpenedByName(shift),
            resolveOpenedByUsername(shift),
            shift.getOpenedAt(),
            shift.getClosedAt(),
            shift.getStatus(),
            MoneySupport.normalize(shift.getOpeningCashAmount()),
            totals.salesRevenue(),
            totals.refundAmount(),
            totals.expenseAmount(),
            totals.cashSales(),
            totals.cashRefunds(),
            totals.cashExpenses(),
            totals.expectedCash(),
            shift.getStatus() == SalesShiftStatus.CLOSED ? closingCash : null,
            variance,
            totals.orderCount(),
            totals.refundCount(),
            resolveClosedByName(shift),
            shift.getCloseNote()
        );
    }

    private ShiftReportRow toReportRow(SalesShift shift) {
        ShiftSummary summary = toSummary(shift);
        return new ShiftReportRow(
            summary.shiftId(),
            summary.openedByName(),
            summary.openedByUsername(),
            summary.openedAt(),
            summary.closedAt(),
            summary.status(),
            summary.openingCashAmount(),
            summary.salesRevenue(),
            summary.refundAmount(),
            summary.expenseAmount(),
            summary.expectedCashAmount(),
            summary.closingCashActual(),
            summary.cashVarianceAmount(),
            summary.orderCount(),
            summary.refundCount(),
            summary.closedByName(),
            summary.closeNote()
        );
    }

    private ShiftTotals calculateLiveTotals(SalesShift shift, LocalDateTime end) {
        Long shiftId = shift.getId();
        Long userId = shift.getOpenedBy() != null ? shift.getOpenedBy().getId() : null;
        LocalDateTime start = shift.getOpenedAt();
        LocalDateTime safeEnd = end != null ? end : LocalDateTime.now();
        BigDecimal salesRevenue = MoneySupport.normalize(orderRepository.sumSalesRevenueByShiftId(shiftId));
        BigDecimal cashSales = MoneySupport.normalize(orderRepository.sumSalesByShiftIdAndPaymentMethod(shiftId, PaymentMethod.CASH));
        BigDecimal refundAmount = MoneySupport.normalize(refundEventRepository.sumAmountByShiftId(shiftId));
        BigDecimal cashRefunds = MoneySupport.normalize(refundEventRepository.sumAmountByShiftIdAndPaymentMethod(shiftId, PaymentMethod.CASH));
        BigDecimal expenseAmount = userId == null
            ? MoneySupport.ZERO
            : MoneySupport.normalize(expenseRepository.sumAmountCreatedByUserBetween(userId, start, safeEnd));
        BigDecimal cashExpenses = userId == null
            ? MoneySupport.ZERO
            : MoneySupport.normalize(expenseRepository.sumAmountCreatedByUserBetweenAndPaymentMethod(userId, start, safeEnd, PaymentMethod.CASH));
        BigDecimal expectedCash = MoneySupport.subtract(
            MoneySupport.subtract(MoneySupport.add(shift.getOpeningCashAmount(), cashSales), cashRefunds),
            cashExpenses
        );
        return new ShiftTotals(
            salesRevenue,
            refundAmount,
            expenseAmount,
            cashSales,
            cashRefunds,
            cashExpenses,
            expectedCash,
            shiftId == null ? 0L : orderRepository.countBySalesShiftId(shiftId),
            shiftId == null ? 0L : refundEventRepository.countByShiftId(shiftId)
        );
    }

    private ShiftTotals totalsFromSnapshot(SalesShift shift) {
        return new ShiftTotals(
            MoneySupport.normalize(shift.getSalesRevenueAmount()),
            MoneySupport.normalize(shift.getRefundAmount()),
            MoneySupport.normalize(shift.getExpenseAmount()),
            MoneySupport.normalize(shift.getCashSalesAmount()),
            MoneySupport.normalize(shift.getCashRefundAmount()),
            MoneySupport.normalize(shift.getCashExpenseAmount()),
            MoneySupport.normalize(shift.getExpectedCashAmount()),
            shift.getOrderCount() == null ? 0L : shift.getOrderCount(),
            shift.getRefundCount() == null ? 0L : shift.getRefundCount()
        );
    }

    private void requireCanViewShift(User viewer, SalesShift shift) {
        authorizationService.requireSalesShiftAccess(viewer);
        if (authorizationService.canManageAllSalesShifts(viewer)) {
            return;
        }
        Long viewerId = viewer != null ? viewer.getId() : null;
        Long ownerId = shift.getOpenedBy() != null ? shift.getOpenedBy().getId() : null;
        if (viewerId == null || ownerId == null || !viewerId.equals(ownerId)) {
            throw new AuthorizationException("You are not allowed to view this shift");
        }
    }

    private User resolveUser(User user) {
        if (user == null || user.getId() == null) {
            throw new ValidationException("User not found");
        }
        return userRepository.findById(user.getId())
            .orElseThrow(() -> new ValidationException("User not found"));
    }

    private User resolveUserForUpdate(User user) {
        if (user == null || user.getId() == null) {
            throw new ValidationException("User not found");
        }
        return userRepository.findByIdForUpdate(user.getId())
            .orElseThrow(() -> new ValidationException("User not found"));
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "System";
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "System";
    }

    private String resolveOpenedByName(SalesShift shift) {
        if (shift.getOpenedByNameSnapshot() != null && !shift.getOpenedByNameSnapshot().isBlank()) {
            return shift.getOpenedByNameSnapshot();
        }
        return resolveDisplayName(shift.getOpenedBy());
    }

    private String resolveOpenedByUsername(SalesShift shift) {
        if (shift.getOpenedByUsernameSnapshot() != null && !shift.getOpenedByUsernameSnapshot().isBlank()) {
            return shift.getOpenedByUsernameSnapshot();
        }
        return shift.getOpenedBy() != null ? shift.getOpenedBy().getUsername() : "";
    }

    private String resolveClosedByName(SalesShift shift) {
        if (shift.getClosedByNameSnapshot() != null && !shift.getClosedByNameSnapshot().isBlank()) {
            return shift.getClosedByNameSnapshot();
        }
        return shift.getClosedBy() != null ? resolveDisplayName(shift.getClosedBy()) : "";
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
