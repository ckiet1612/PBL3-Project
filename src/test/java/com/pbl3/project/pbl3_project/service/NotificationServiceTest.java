package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.NotificationCategory;
import com.pbl3.project.pbl3_project.entity.NotificationSeverity;
import com.pbl3.project.pbl3_project.entity.NotificationType;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.NotificationService.CreateTaskRequest;
import com.pbl3.project.pbl3_project.service.NotificationService.NotificationFilter;
import com.pbl3.project.pbl3_project.service.NotificationService.NotificationView;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:pbl3_notification_test;DB_CLOSE_DELAY=-1",
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
class NotificationServiceTest {
    private static final List<String> TABLES = List.of(
        "notification_user_states",
        "notifications",
        "qr_payments",
        "sales_shift_refunds",
        "order_items",
        "orders",
        "inventory_transactions",
        "inventory_position_baselines",
        "stocktake_items",
        "stocktake_sessions",
        "promotions",
        "products",
        "categories",
        "sales_shifts",
        "users"
    );

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clearTables();
        seedUsersAndProduct();
    }

    @Test
    void refreshUpsertsLowStockNotificationForManagersOnlyAndResolvesWhenConditionClears() {
        notificationService.refreshSystemNotifications();
        notificationService.refreshSystemNotifications();

        assertThat(queryLong("SELECT COUNT(*) FROM notifications WHERE event_key = 'product-stock:1:LOW_STOCK'"))
            .isEqualTo(1L);
        assertThat(notificationService.listForUser(admin(), NotificationFilter.ALL))
            .anyMatch(view -> view.type() == NotificationType.LOW_STOCK && view.title().contains("Coffee"));
        assertThat(notificationService.listForUser(manager(), NotificationFilter.ALL))
            .anyMatch(view -> view.type() == NotificationType.LOW_STOCK);
        assertThat(notificationService.listForUser(staff(), NotificationFilter.ALL))
            .noneMatch(view -> view.type() == NotificationType.LOW_STOCK);

        jdbcTemplate.update("UPDATE products SET quantity = 50 WHERE id = 1");
        notificationService.refreshSystemNotifications();

        assertThat(queryLong("""
            SELECT COUNT(*)
            FROM notifications
            WHERE event_key = 'product-stock:1:LOW_STOCK'
              AND resolved_at IS NOT NULL
            """)).isEqualTo(1L);
    }

    @Test
    void readAndDismissAreTrackedPerUser() {
        notificationService.refreshSystemNotifications();
        Long notificationId = queryLongObject("SELECT id FROM notifications WHERE event_key = 'product-stock:1:LOW_STOCK'");

        long adminUnreadBefore = notificationService.countUnread(admin());
        long managerUnreadBefore = notificationService.countUnread(manager());

        notificationService.markRead(admin(), notificationId);

        assertThat(notificationService.countUnread(admin())).isEqualTo(adminUnreadBefore - 1);
        assertThat(notificationService.countUnread(manager())).isEqualTo(managerUnreadBefore);

        notificationService.dismiss(admin(), notificationId);

        assertThat(notificationService.listForUser(admin(), NotificationFilter.ALL))
            .noneMatch(view -> notificationId.equals(view.id()));
        assertThat(notificationService.listForUser(manager(), NotificationFilter.ALL))
            .anyMatch(view -> notificationId.equals(view.id()));
    }

    @Test
    void managerCanCreateTaskForSpecificAndRoleRecipientsButStaffCannotCreateTasks() {
        NotificationView directTask = notificationService.createTask(
            manager(),
            new CreateTaskRequest(
                "Count cash drawer",
                "Verify the drawer before closing.",
                Set.of(3L),
                Set.of(),
                NotificationSeverity.WARNING,
                null,
                null,
                null
            )
        );

        assertThat(directTask.category()).isEqualTo(NotificationCategory.TASK);
        assertThat(notificationService.listForUser(staff(), NotificationFilter.TASKS))
            .anyMatch(view -> view.title().equals("Count cash drawer"));
        assertThat(notificationService.listForUser(admin(), NotificationFilter.TASKS))
            .noneMatch(view -> view.title().equals("Count cash drawer"));

        Long directTaskId = queryLongObject("SELECT id FROM notifications WHERE title = 'Count cash drawer'");
        notificationService.completeTask(staff(), directTaskId);
        assertThat(notificationService.listForUser(staff(), NotificationFilter.TASKS))
            .anyMatch(NotificationView::completed);
        assertThatThrownBy(() -> notificationService.completeTask(manager(), directTaskId))
            .isInstanceOf(AuthorizationException.class);

        notificationService.createTask(
            admin(),
            new CreateTaskRequest(
                "Clean POS area",
                "Reset the checkout counter.",
                Set.of(),
                Set.of(Role.STAFF),
                NotificationSeverity.INFO,
                null,
                null,
                null
            )
        );

        assertThat(queryLong("""
            SELECT COUNT(*)
            FROM notification_user_states state
            JOIN notifications notification ON notification.id = state.notification_id
            JOIN users recipient ON recipient.id = state.user_id
            WHERE notification.title = 'Clean POS area'
              AND recipient.role = 'STAFF'
            """)).isEqualTo(1L);

        assertThatThrownBy(() -> notificationService.createTask(
            staff(),
            new CreateTaskRequest("Unauthorized", "", Set.of(1L), Set.of(), NotificationSeverity.INFO, null, null, null)
        )).isInstanceOf(AuthorizationException.class);
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

    private void seedUsersAndProduct() {
        jdbcTemplate.update("INSERT INTO users (id, username, password, full_name, role, enabled) VALUES (1, 'admin', 'hash', 'Admin User', 'ADMIN', TRUE)");
        jdbcTemplate.update("INSERT INTO users (id, username, password, full_name, role, enabled) VALUES (2, 'manager', 'hash', 'Manager User', 'MANAGER', TRUE)");
        jdbcTemplate.update("INSERT INTO users (id, username, password, full_name, role, enabled) VALUES (3, 'staff', 'hash', 'Staff User', 'STAFF', TRUE)");
        jdbcTemplate.update("INSERT INTO categories (id, name) VALUES (1, 'Beverage')");
        jdbcTemplate.update("""
            INSERT INTO products (
                id, name, description, price, quantity, image_url, sku, barcode, import_price,
                brand_id, origin_id, unit_id, category_id, is_deleted, min_stock_level, version
            )
            VALUES (1, 'Coffee', 'Demo product', 50000.00, 2, NULL, 'COF-001', '8930001', 30000.00,
                NULL, NULL, NULL, 1, FALSE, 5, 0)
            """);
    }

    private User admin() {
        return new User(1L, "admin", "hash", "Admin User", Role.ADMIN, true);
    }

    private User manager() {
        return new User(2L, "manager", "hash", "Manager User", Role.MANAGER, true);
    }

    private User staff() {
        return new User(3L, "staff", "hash", "Staff User", Role.STAFF, true);
    }

    private long queryLong(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private Long queryLongObject(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
