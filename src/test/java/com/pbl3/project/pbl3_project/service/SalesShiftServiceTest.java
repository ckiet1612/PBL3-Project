package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.SalesShift;
import com.pbl3.project.pbl3_project.entity.SalesShiftStatus;
import com.pbl3.project.pbl3_project.entity.User;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:pbl3_shift_test;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "spring.flyway.baseline-on-migrate=true",
    "spring.flyway.baseline-version=0",
    "app.seed.enabled=false",
    "app.version-gate.enabled=false"
})
class SalesShiftServiceTest {

    private static final List<String> TABLES = List.of(
        "sales_shift_refunds",
        "order_items",
        "orders",
        "inventory_transactions",
        "inventory_position_baselines",
        "expenses",
        "operational_audit_logs",
        "account_audit_logs",
        "products",
        "categories",
        "sales_shifts",
        "users"
    );

    @Autowired
    private SalesShiftService salesShiftService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clearTables();
        seedUsersAndProduct();
    }

    @Test
    void checkoutRequiresOpenShiftThenOrderIsAttachedAndCloseComputesCash() {
        User staff = staff();

        assertThatThrownBy(() -> orderService.createOrder(cashOrderRequest(staff.getId())))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Open a sales shift");

        SalesShift opened = salesShiftService.openShift(staff, new BigDecimal("100000"), "start");
        assertThat(opened.getStatus()).isEqualTo(SalesShiftStatus.OPEN);

        Order order = orderService.createOrder(cashOrderRequest(staff.getId()));
        assertThat(order.getSalesShift()).isNotNull();
        assertThat(order.getSalesShift().getId()).isEqualTo(opened.getId());

        jdbcTemplate.update("""
            INSERT INTO expenses (id, spent_on, category, title, amount, payment_method, note, created_by_user_id, created_at, updated_at)
            VALUES (1, CURRENT_DATE, 'OTHER', 'Shift expense', 20000.00, 'CASH', 'cash expense', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """);

        salesShiftService.recordRefundEvent(staff, order, new BigDecimal("30000"), "partial refund");

        SalesShift closed = salesShiftService.closeOwnShift(staff, new BigDecimal("170000"), "close");

        assertThat(closed.getStatus()).isEqualTo(SalesShiftStatus.CLOSED);
        assertThat(closed.getExpectedCashAmount()).isEqualByComparingTo("150000.00");
        assertThat(closed.getCashVarianceAmount()).isEqualByComparingTo("20000.00");
        assertThat(closed.getSalesRevenueAmount()).isEqualByComparingTo("100000.00");
        assertThat(closed.getRefundAmount()).isEqualByComparingTo("30000.00");
        assertThat(closed.getExpenseAmount()).isEqualByComparingTo("20000.00");
        assertThat(closed.getOrderCount()).isEqualTo(1L);
        assertThat(closed.getRefundCount()).isEqualTo(1L);

        SalesShiftService.ShiftSummary summary = salesShiftService.getShiftSummary(staff, opened.getId());
        assertThat(summary.expectedCashAmount()).isEqualByComparingTo("150000.00");
        assertThat(summary.cashSales()).isEqualByComparingTo("100000.00");
        assertThat(summary.cashRefunds()).isEqualByComparingTo("30000.00");
        assertThat(summary.cashExpenses()).isEqualByComparingTo("20000.00");
    }

    @Test
    void cannotOpenSecondShiftAndOnlyManagerCanCloseOtherShiftWithNote() {
        User staff = staff();
        User manager = manager();
        User otherStaff = otherStaff();

        SalesShift opened = salesShiftService.openShift(staff, new BigDecimal("50000"), "start");

        assertThatThrownBy(() -> salesShiftService.openShift(staff, BigDecimal.ZERO, "again"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Close the current shift");

        assertThatThrownBy(() -> salesShiftService.closeShiftAsManager(otherStaff, opened.getId(), new BigDecimal("50000"), "help close"))
            .isInstanceOf(AuthorizationException.class);

        assertThatThrownBy(() -> salesShiftService.closeShiftAsManager(manager, opened.getId(), new BigDecimal("50000"), " "))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("note");

        SalesShift closedByManager = salesShiftService.closeShiftAsManager(
            manager,
            opened.getId(),
            new BigDecimal("50000"),
            "manager verified cash"
        );

        assertThat(closedByManager.getStatus()).isEqualTo(SalesShiftStatus.CLOSED);
        assertThat(closedByManager.getClosedBy().getId()).isEqualTo(manager.getId());
        assertThat(closedByManager.getCloseNote()).isEqualTo("manager verified cash");
    }

    @Test
    void concurrentOpenShiftCreatesOnlyOneOpenShiftForUser() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = List.of(
                executor.submit(() -> openShiftWhenReleased(ready, start)),
                executor.submit(() -> openShiftWhenReleased(ready, start))
            );
            assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
            }

            assertThat(results).containsExactlyInAnyOrder(true, false);
            assertThat(queryLong("SELECT COUNT(*) FROM sales_shifts")).isEqualTo(1L);
            assertThat(queryLong("SELECT COUNT(*) FROM sales_shifts WHERE status = 'OPEN'")).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void cancelAndReturnCreateRefundEventsForProcessingShift() {
        User staff = staff();
        salesShiftService.openShift(staff, BigDecimal.ZERO, "start");

        Order canceledOrder = orderService.createOrder(cashOrderRequest(staff.getId()));
        orderService.cancelOrder(canceledOrder.getId(), staff.getId(), "void sale");

        assertThat(queryLong("SELECT COUNT(*) FROM sales_shift_refunds")).isEqualTo(1L);
        assertThat(queryBigDecimal("SELECT COALESCE(SUM(amount), 0) FROM sales_shift_refunds"))
            .isEqualByComparingTo("100000.00");

        Order returnedOrder = orderService.createOrder(cashOrderRequest(staff.getId()));
        Long itemId = returnedOrder.getOrderItems().get(0).getId();
        orderService.returnOrderItems(returnedOrder.getId(), staff.getId(), Map.of(itemId, 1), "return one item");

        assertThat(queryLong("SELECT COUNT(*) FROM sales_shift_refunds")).isEqualTo(2L);
        assertThat(queryBigDecimal("SELECT COALESCE(SUM(amount), 0) FROM sales_shift_refunds"))
            .isEqualByComparingTo("150000.00");
    }

    private void clearTables() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : TABLES) {
            try {
                jdbcTemplate.update("DELETE FROM " + table);
            } catch (RuntimeException ignored) {
            }
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private boolean openShiftWhenReleased(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(2, TimeUnit.SECONDS);
        try {
            salesShiftService.openShift(staff(), BigDecimal.ZERO, "concurrent start");
            return true;
        } catch (ValidationException ex) {
            return false;
        }
    }

    private void seedUsersAndProduct() {
        jdbcTemplate.update("INSERT INTO users (id, username, password, full_name, role, enabled) VALUES (1, 'staff', 'hash', 'Staff User', 'STAFF', TRUE)");
        jdbcTemplate.update("INSERT INTO users (id, username, password, full_name, role, enabled) VALUES (2, 'manager', 'hash', 'Manager User', 'MANAGER', TRUE)");
        jdbcTemplate.update("INSERT INTO users (id, username, password, full_name, role, enabled) VALUES (3, 'other', 'hash', 'Other Staff', 'STAFF', TRUE)");
        jdbcTemplate.update("INSERT INTO categories (id, name) VALUES (1, 'Beverage')");
        jdbcTemplate.update("""
            INSERT INTO products (
                id, name, description, price, quantity, image_url, sku, barcode, import_price,
                brand_id, origin_id, unit_id, category_id, is_deleted, min_stock_level, version
            )
            VALUES (1, 'Coffee', 'Demo product', 50000.00, 20, NULL, 'COF-001', '8930001', 30000.00,
                NULL, NULL, NULL, 1, FALSE, 5, 0)
            """);
    }

    private CreateOrderRequest cashOrderRequest(Long userId) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(userId);
        request.setPaymentMethod(PaymentMethod.CASH);
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);
        request.setItems(new ArrayList<>(List.of(item)));
        return request;
    }

    private User staff() {
        return new User(1L, "staff", "hash", "Staff User", Role.STAFF, true);
    }

    private User manager() {
        return new User(2L, "manager", "hash", "Manager User", Role.MANAGER, true);
    }

    private User otherStaff() {
        return new User(3L, "other", "hash", "Other Staff", Role.STAFF, true);
    }

    private long queryLong(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private BigDecimal queryBigDecimal(String sql) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }
}
