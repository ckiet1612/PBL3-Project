package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:pbl3_backup_test;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "spring.flyway.baseline-on-migrate=true",
    "spring.flyway.baseline-version=0",
    "app.seed.enabled=false",
    "app.version-gate.enabled=false",
    "app.backup.default-directory=${java.io.tmpdir}/pbl3-test-backups"
})
class DataBackupServiceTest {

    private static final List<String> TABLES = List.of(
        "categories",
        "brands",
        "origins",
        "units",
        "suppliers",
        "users",
        "customers",
        "user_ui_preferences",
        "sales_shifts",
        "products",
        "promotions",
        "qr_payments",
        "orders",
        "order_items",
        "sales_shift_refunds",
        "import_orders",
        "import_order_items",
        "expenses",
        "inventory_position_baselines",
        "inventory_transactions",
        "operational_audit_logs",
        "account_audit_logs",
        "stocktake_sessions",
        "stocktake_items",
        "system_settings"
    );

    @Autowired
    private DataBackupService dataBackupService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        createSystemSettingsTable();
        clearTables();
        seedTenantData();
    }

    @Test
    void exportAndRestoreFullTenantBackup() throws Exception {
        Path backupPath = tempDir.resolve("tenant-demo.pbl3backup");

        DataBackupService.BackupExportResult exportResult = dataBackupService.exportBackup(admin(), backupPath.toFile());

        assertThat(exportResult.file()).isFile();
        assertThat(exportResult.file().length()).isGreaterThan(0);
        assertThat(exportResult.manifest().formatVersion()).isEqualTo(1);
        assertThat(exportResult.manifest().schemaVersion()).isEqualTo("14");
        assertThat(exportResult.manifest().tables()).extracting(DataBackupService.BackupTableSummary::tableName)
            .contains("users", "products", "orders", "sales_shifts", "sales_shift_refunds", "system_settings");

        clearTables();
        restoreSchemaVersionSetting();

        DataBackupService.BackupRestoreResult restoreResult = dataBackupService.restoreBackup(admin(), exportResult.file());

        assertThat(restoreResult.safetyBackupFile()).isFile();
        assertThat(restoreResult.totalRows()).isEqualTo(exportResult.totalRows());
        assertThat(queryString("SELECT name FROM products WHERE id = 1")).isEqualTo("Cà phê sữa");
        assertThat(queryString("SELECT full_name FROM customers WHERE id = 1")).isEqualTo("Nguyễn Văn A");
        assertThat(queryLong("SELECT COUNT(*) FROM order_items")).isEqualTo(1L);
        assertThat(queryLong("SELECT COUNT(*) FROM import_order_items")).isEqualTo(1L);
        assertThat(queryLong("SELECT COUNT(*) FROM sales_shifts")).isEqualTo(1L);
        assertThat(queryLong("SELECT COUNT(*) FROM sales_shift_refunds")).isEqualTo(1L);
        assertThat(queryString("SELECT setting_value FROM system_settings WHERE setting_key = 'schema_version'")).isEqualTo("14");
    }

    @Test
    void corruptBackupIsRejectedWithoutChangingData() throws Exception {
        Path corruptFile = tempDir.resolve("broken.pbl3backup");
        Files.writeString(corruptFile, "not a zip backup");

        assertThatThrownBy(() -> dataBackupService.restoreBackup(admin(), corruptFile.toFile()))
            .isInstanceOf(BackupException.class);

        assertThat(queryString("SELECT name FROM products WHERE id = 1")).isEqualTo("Cà phê sữa");
    }

    @Test
    void nonAdminCannotExportOrRestoreBackups() {
        Path backupPath = tempDir.resolve("blocked.pbl3backup");

        assertThatThrownBy(() -> dataBackupService.exportBackup(manager(), backupPath.toFile()))
            .isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> dataBackupService.restoreBackup(manager(), backupPath.toFile()))
            .isInstanceOf(AuthorizationException.class);
    }

    private void createSystemSettingsTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS system_settings (
                setting_key VARCHAR(100) NOT NULL PRIMARY KEY,
                setting_value VARCHAR(255) NOT NULL,
                description VARCHAR(500) NOT NULL DEFAULT '',
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }

    private void clearTables() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (int i = TABLES.size() - 1; i >= 0; i--) {
            try {
                jdbcTemplate.update("DELETE FROM " + TABLES.get(i));
            } catch (RuntimeException ignored) {
            }
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private void seedTenantData() {
        restoreSchemaVersionSetting();
        jdbcTemplate.update("INSERT INTO users (id, username, password, full_name, role, enabled) VALUES (1, 'admin', 'hash', 'Admin User', 'ADMIN', TRUE)");
        jdbcTemplate.update("INSERT INTO categories (id, name) VALUES (1, 'Beverage')");
        jdbcTemplate.update("INSERT INTO brands (id, name, is_deleted) VALUES (1, 'PBL3 Brand', FALSE)");
        jdbcTemplate.update("INSERT INTO origins (id, name, is_deleted) VALUES (1, 'Vietnam', FALSE)");
        jdbcTemplate.update("INSERT INTO units (id, name, is_deleted) VALUES (1, 'Cup', FALSE)");
        jdbcTemplate.update("INSERT INTO suppliers (id, name, phone, address, is_deleted) VALUES (1, 'Demo Supplier', '0900000000', 'Da Nang', FALSE)");
        jdbcTemplate.update("""
            INSERT INTO customers (id, full_name, phone, enabled, created_at, updated_at)
            VALUES (1, 'Nguyễn Văn A', '0912345678', TRUE, '2026-05-20 09:00:00', '2026-05-20 09:00:00')
            """);
        jdbcTemplate.update("""
            INSERT INTO user_ui_preferences (
                id, user_id, accent_preset, density_mode, reduced_motion,
                sidebar_collapsed_by_default, dashboard_hidden_sections, dashboard_section_order
            )
            VALUES (1, 1, 'BLUE', 'COMFORTABLE', FALSE, FALSE, '', '')
            """);
        jdbcTemplate.update("""
            INSERT INTO products (
                id, name, description, price, quantity, image_url, sku, barcode, import_price,
                brand_id, origin_id, unit_id, category_id, is_deleted, min_stock_level, version
            )
            VALUES (1, 'Cà phê sữa', 'Demo product', 25000.00, 20, NULL, 'SKU-001', '8930001', 15000.00,
                1, 1, 1, 1, FALSE, 5, 0)
            """);
        jdbcTemplate.update("""
            INSERT INTO promotions (
                id, name, scope, discount_type, discount_value, enabled, starts_at, ends_at,
                target_product_id, min_order_total, created_by_user_id, created_at, updated_at
            )
            VALUES (1, 'Opening Promo', 'PRODUCT', 'FIXED_AMOUNT', 5000.00, TRUE,
                '2026-05-20 00:00:00', NULL, 1, NULL, 1, '2026-05-20 09:00:00', '2026-05-20 09:00:00')
            """);
        jdbcTemplate.update("""
            INSERT INTO sales_shifts (
                id, opened_by_user_id, opened_by_name_snapshot, opened_by_username_snapshot,
                opened_at, opening_cash_amount, open_note, status, closed_at, closed_by_user_id,
                closed_by_name_snapshot, closed_by_username_snapshot, closing_cash_actual,
                expected_cash_amount, cash_variance_amount, close_note, sales_revenue_amount,
                refund_amount, expense_amount, cash_sales_amount, cash_refund_amount,
                cash_expense_amount, order_count, refund_count
            )
            VALUES (
                1, 1, 'Admin User', 'admin', '2026-05-20 09:30:00', 100000.00,
                'opening', 'CLOSED', '2026-05-20 18:00:00', 1, 'Admin User', 'admin',
                135000.00, 135000.00, 0.00, 'closing', 45000.00, 10000.00,
                30000.00, 45000.00, 10000.00, 0.00, 1, 1
            )
            """);
        jdbcTemplate.update("""
            INSERT INTO orders (
                id, created_at, total_price, gross_subtotal, discount_total, order_level_discount_total,
                applied_order_promotion_id_snapshot, applied_order_promotion_name_snapshot,
                user_id, customer_id, payment_method, status, refunded_amount, status_note, sales_shift_id,
                created_by_name_snapshot, customer_name_snapshot, customer_phone_snapshot
            )
            VALUES (1, '2026-05-20 10:00:00', 45000.00, 50000.00, 5000.00, 0.00,
                NULL, NULL, 1, 1, 'CASH', 'COMPLETED', 10000.00, 'partial refund', 1,
                'Admin User', 'Nguyễn Văn A', '0912345678')
            """);
        jdbcTemplate.update("""
            INSERT INTO order_items (
                id, order_id, product_id, quantity, price, original_unit_price,
                line_promotion_discount_amount, order_level_discount_allocated_amount,
                applied_product_promotion_id_snapshot, applied_product_promotion_name_snapshot,
                cost_at_sale, returned_quantity, product_name_snapshot, sku_snapshot, barcode_snapshot,
                category_name_snapshot, brand_name_snapshot, origin_name_snapshot, unit_name_snapshot
            )
            VALUES (1, 1, 1, 2, 22500.00, 25000.00, 5000.00, 0.00, 1, 'Opening Promo',
                15000.00, 1, 'Cà phê sữa', 'SKU-001', '8930001', 'Beverage', 'PBL3 Brand', 'Vietnam', 'Cup')
            """);
        jdbcTemplate.update("""
            INSERT INTO sales_shift_refunds (id, shift_id, order_id, processed_by_user_id, payment_method, amount, created_at, reason)
            VALUES (1, 1, 1, 1, 'CASH', 10000.00, '2026-05-20 11:30:00', 'partial refund')
            """);
        jdbcTemplate.update("""
            INSERT INTO import_orders (
                id, supplier_id, user_id, created_at, total_cost, status, status_note, notes,
                created_by_name_snapshot, supplier_name_snapshot
            )
            VALUES (1, 1, 1, '2026-05-20 08:00:00', 150000.00, 'COMPLETED', NULL, 'demo import',
                'Admin User', 'Demo Supplier')
            """);
        jdbcTemplate.update("""
            INSERT INTO import_order_items (
                id, import_order_id, product_id, quantity, import_price,
                product_name_snapshot, sku_snapshot, barcode_snapshot, category_name_snapshot,
                brand_name_snapshot, origin_name_snapshot, unit_name_snapshot
            )
            VALUES (1, 1, 1, 10, 15000.00, 'Cà phê sữa', 'SKU-001', '8930001',
                'Beverage', 'PBL3 Brand', 'Vietnam', 'Cup')
            """);
        jdbcTemplate.update("""
            INSERT INTO expenses (id, spent_on, category, title, amount, payment_method, note, created_by_user_id, created_at, updated_at)
            VALUES (1, '2026-05-20', 'OTHER', 'Demo expense', 30000.00, 'CASH', 'demo', 1, '2026-05-20 11:00:00', '2026-05-20 11:00:00')
            """);
        jdbcTemplate.update("""
            INSERT INTO inventory_position_baselines (id, product_id, baseline_at, quantity, inventory_value, average_cost)
            VALUES (1, 1, '2026-05-20 08:00:00', 20, 300000.00, 15000.00)
            """);
        jdbcTemplate.update("""
            INSERT INTO inventory_transactions (
                id, product_id, quantity_change, transaction_type, reference_id, order_id, import_order_id,
                user_id, notes, unit_cost_snapshot, inventory_value_change, created_at
            )
            VALUES (1, 1, -2, 'SALE', 1, 1, NULL, 1, 'sale demo', 15000.00, -30000.00, '2026-05-20 10:00:00')
            """);
        jdbcTemplate.update("""
            INSERT INTO operational_audit_logs (id, actor_user_id, action, subject_type, subject_id, subject_label, details, created_at)
            VALUES (1, 1, 'PRODUCT_CREATED', 'PRODUCT', 1, 'Cà phê sữa', 'created for backup test', '2026-05-20 09:00:00')
            """);
        jdbcTemplate.update("""
            INSERT INTO account_audit_logs (id, actor_user_id, target_user_id, action, details, created_at)
            VALUES (1, 1, 1, 'CREATE_ACCOUNT', 'created admin', '2026-05-20 09:00:00')
            """);
        jdbcTemplate.update("""
            INSERT INTO stocktake_sessions (id, created_by_user_id, scope_type, category_id, status, notes, created_at, applied_at)
            VALUES (1, 1, 'ALL_PRODUCTS', NULL, 'OPEN', 'demo stocktake', '2026-05-20 12:00:00', NULL)
            """);
        jdbcTemplate.update("""
            INSERT INTO stocktake_items (id, session_id, product_id, system_quantity, unit_cost_snapshot, counted_quantity, notes)
            VALUES (1, 1, 1, 20, 15000.00, 19, 'one missing')
            """);
    }

    private void restoreSchemaVersionSetting() {
        jdbcTemplate.update("""
            INSERT INTO system_settings (setting_key, setting_value, description, updated_at)
            VALUES ('schema_version', '14', 'Application-managed database schema version marker.', CURRENT_TIMESTAMP)
            """);
    }

    private User admin() {
        return new User(1L, "admin", "hash", "Admin User", Role.ADMIN, true);
    }

    private User manager() {
        return new User(2L, "manager", "hash", "Manager User", Role.MANAGER, true);
    }

    private String queryString(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    private long queryLong(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }
}
