package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.dto.payment.QrPaymentCreateRequest;
import com.pbl3.project.pbl3_project.dto.payment.QrPaymentStatusDto;
import com.pbl3.project.pbl3_project.dto.payment.SePayWebhookPayload;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.QrPaymentStatus;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:pbl3_qr_payment_test;DB_CLOSE_DELAY=-1",
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
class QrPaymentServiceTest {
    private static final List<String> TABLES = List.of(
        "qr_payments",
        "sales_shift_refunds",
        "order_items",
        "orders",
        "inventory_transactions",
        "inventory_position_baselines",
        "operational_audit_logs",
        "account_audit_logs",
        "products",
        "categories",
        "sales_shifts",
        "users"
    );

    @Autowired
    private QrPaymentService qrPaymentService;

    @Autowired
    private SalesShiftService salesShiftService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clearTables();
        seedUserAndProduct();
        salesShiftService.openShift(staff(), BigDecimal.ZERO, "start");
    }

    @Test
    void paidQrPaymentFinalizesOrderWithQrMethod() {
        QrPaymentStatusDto created = qrPaymentService.createPayment(qrPaymentRequest());

        assertThat(created.status()).isEqualTo(QrPaymentStatus.PENDING);
        assertThat(created.qrCode()).isNotBlank();
        assertThat(created.amount()).isEqualByComparingTo("100000.00");

        QrPaymentStatusDto paid = qrPaymentService.handleSePayWebhook(webhook(created), "Apikey valid", null);
        assertThat(paid.status()).isEqualTo(QrPaymentStatus.PAID);

        Order order = qrPaymentService.finalizePaidPayment(created.id());

        assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.QR);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("100000.00");
        assertThat(queryLong("SELECT quantity FROM products WHERE id = 1")).isEqualTo(18L);
        assertThat(queryString("SELECT status FROM qr_payments WHERE id = " + created.id())).isEqualTo("ORDER_CREATED");
        assertThat(queryLong("SELECT created_order_id FROM qr_payments WHERE id = " + created.id())).isEqualTo(order.getId());
    }

    @Test
    void invalidWebhookSignatureDoesNotChangePaymentStatus() {
        QrPaymentStatusDto created = qrPaymentService.createPayment(qrPaymentRequest());

        assertThatThrownBy(() -> qrPaymentService.handleSePayWebhook(webhook(created), "Apikey invalid", null))
            .isInstanceOf(QrPaymentException.class)
            .hasMessageContaining("Invalid SePay webhook authentication");

        assertThat(queryString("SELECT status FROM qr_payments WHERE id = " + created.id())).isEqualTo("PENDING");
        assertThat(queryLong("SELECT COUNT(*) FROM orders")).isZero();
    }

    @Test
    void concurrentFinalizeCreatesOnlyOneOrder() throws Exception {
        QrPaymentStatusDto created = qrPaymentService.createPayment(qrPaymentRequest());
        qrPaymentService.handleSePayWebhook(webhook(created), "Apikey valid", null);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Long>> futures = List.of(
                executor.submit(() -> finalizeWhenReleased(created.id(), ready, start)),
                executor.submit(() -> finalizeWhenReleased(created.id(), ready, start))
            );
            assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Long> results = new ArrayList<>();
            for (Future<Long> future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
            }

            assertThat(queryLong("SELECT COUNT(*) FROM orders")).isEqualTo(1L);
            assertThat(queryString("SELECT status FROM qr_payments WHERE id = " + created.id())).isEqualTo("ORDER_CREATED");
            assertThat(results.stream().filter(id -> id > 0).distinct().count()).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    private QrPaymentCreateRequest qrPaymentRequest() {
        QrPaymentCreateRequest request = new QrPaymentCreateRequest();
        request.setUserId(1L);
        request.setAmount(new BigDecimal("100000"));
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);
        request.setItems(new ArrayList<>(List.of(item)));
        return request;
    }

    private SePayWebhookPayload webhook(QrPaymentStatusDto payment) {
        SePayWebhookPayload payload = new SePayWebhookPayload();
        payload.setId(777L);
        payload.setGateway("Vietcombank");
        payload.setAccountNumber("0010000000355");
        payload.setCode(SePayClient.buildPaymentCode(payment.orderCode()));
        payload.setContent(SePayClient.buildPaymentCode(payment.orderCode()) + " chuyen tien");
        payload.setTransferType("in");
        payload.setTransferAmount(new BigDecimal("100000"));
        payload.setReferenceCode("FTTEST");
        return payload;
    }

    private Long finalizeWhenReleased(Long paymentId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(2, TimeUnit.SECONDS);
        try {
            return qrPaymentService.finalizePaidPayment(paymentId).getId();
        } catch (QrPaymentException ex) {
            return -1L;
        }
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

    private void seedUserAndProduct() {
        jdbcTemplate.update("INSERT INTO users (id, username, password, full_name, role, enabled) VALUES (1, 'staff', 'hash', 'Staff User', 'STAFF', TRUE)");
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

    private User staff() {
        return new User(1L, "staff", "hash", "Staff User", Role.STAFF, true);
    }

    private long queryLong(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private String queryString(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    @TestConfiguration
    static class QrPaymentTestConfig {
        @Bean
        @Primary
        QrPaymentGateway fakeQrPaymentGateway() {
            return new QrPaymentGateway() {
                @Override
                public QrPaymentLink createPaymentLink(QrPaymentRequest request) {
                    return new QrPaymentLink(
                        SePayClient.buildPaymentCode(request.orderCode()),
                        "https://qr.sepay.vn/img?acc=0010000000355&bank=Vietcombank&amount=100000&des=" + SePayClient.buildPaymentCode(request.orderCode()),
                        "https://qr.sepay.vn/img",
                        "PENDING"
                    );
                }

                @Override
                public QrPaymentProviderStatus getPaymentStatus(Long orderCode) {
                    return new QrPaymentProviderStatus("PENDING", BigDecimal.ZERO);
                }

                @Override
                public void cancelPayment(Long orderCode, String reason) {
                }

                @Override
                public boolean verifyWebhook(SePayWebhookPayload payload, String authorizationHeader, String rawBody) {
                    return payload != null && "Apikey valid".equals(authorizationHeader);
                }

                @Override
                public int paymentExpirySeconds() {
                    return 300;
                }
            };
        }
    }
}
