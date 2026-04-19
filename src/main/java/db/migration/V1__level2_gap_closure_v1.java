package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class V1__level2_gap_closure_v1 extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        ensureReferenceTables(connection);
        ensureUsersTable(connection);
        ensureProductTable(connection);
        ensureOrderTables(connection);
        ensureImportTables(connection);
        ensureAccountAuditTable(connection);
        ensureInventoryPositionBaselinesTable(connection);
        ensureInventoryTransactionsTable(connection);
        ensureOperationalAuditTable(connection);
        ensureStocktakeTables(connection);

        ensureProductColumns(connection);
        ensureOrderColumns(connection);
        ensureImportColumns(connection);
        ensureLedgerColumns(connection);

        ensureIndexes(connection);
        ensureChecks(connection);
        backfillSnapshots(connection);
    }

    private void ensureReferenceTables(Connection connection) throws SQLException {
        execute(connection, """
            CREATE TABLE IF NOT EXISTS categories (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE
            )
            """);
        execute(connection, """
            CREATE TABLE IF NOT EXISTS brands (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                is_deleted BOOLEAN NOT NULL DEFAULT FALSE
            )
            """);
        execute(connection, """
            CREATE TABLE IF NOT EXISTS origins (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                is_deleted BOOLEAN NOT NULL DEFAULT FALSE
            )
            """);
        execute(connection, """
            CREATE TABLE IF NOT EXISTS units (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                is_deleted BOOLEAN NOT NULL DEFAULT FALSE
            )
            """);
        execute(connection, """
            CREATE TABLE IF NOT EXISTS suppliers (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                phone VARCHAR(255) NULL,
                address VARCHAR(255) NULL,
                is_deleted BOOLEAN NOT NULL DEFAULT FALSE
            )
            """);
    }

    private void ensureUsersTable(Connection connection) throws SQLException {
        execute(connection, """
            CREATE TABLE IF NOT EXISTS users (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(255) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                full_name VARCHAR(255) NOT NULL,
                role VARCHAR(20) NOT NULL,
                enabled BOOLEAN NOT NULL DEFAULT TRUE
            )
            """);
    }

    private void ensureProductTable(Connection connection) throws SQLException {
        execute(connection, """
            CREATE TABLE IF NOT EXISTS products (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description VARCHAR(255) NULL,
                price DECIMAL(19,2) NOT NULL,
                quantity INT NOT NULL,
                image_url VARCHAR(255) NULL,
                sku VARCHAR(255) NULL UNIQUE,
                barcode VARCHAR(255) NULL,
                import_price DECIMAL(19,2) NULL,
                brand_id BIGINT NULL,
                origin_id BIGINT NULL,
                unit_id BIGINT NULL,
                category_id BIGINT NULL,
                is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                min_stock_level INT NOT NULL DEFAULT 10,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(id),
                CONSTRAINT fk_products_origin FOREIGN KEY (origin_id) REFERENCES origins(id),
                CONSTRAINT fk_products_unit FOREIGN KEY (unit_id) REFERENCES units(id),
                CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
            )
            """);
    }

    private void ensureOrderTables(Connection connection) throws SQLException {
        execute(connection, """
            CREATE TABLE IF NOT EXISTS orders (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                created_at DATETIME NOT NULL,
                total_price DECIMAL(19,2) NOT NULL,
                user_id BIGINT NULL,
                payment_method VARCHAR(20) NOT NULL,
                status VARCHAR(30) NOT NULL,
                refunded_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00,
                status_note TEXT NULL,
                created_by_name_snapshot VARCHAR(255) NULL,
                CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """);
        execute(connection, """
            CREATE TABLE IF NOT EXISTS order_items (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                order_id BIGINT NULL,
                product_id BIGINT NULL,
                quantity INT NOT NULL,
                price DECIMAL(19,2) NOT NULL,
                cost_at_sale DECIMAL(19,2) NULL,
                returned_quantity INT NOT NULL DEFAULT 0,
                product_name_snapshot VARCHAR(255) NULL,
                sku_snapshot VARCHAR(255) NULL,
                barcode_snapshot VARCHAR(255) NULL,
                category_name_snapshot VARCHAR(255) NULL,
                brand_name_snapshot VARCHAR(255) NULL,
                origin_name_snapshot VARCHAR(255) NULL,
                unit_name_snapshot VARCHAR(255) NULL,
                CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
                CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
            )
            """);
    }

    private void ensureImportTables(Connection connection) throws SQLException {
        execute(connection, """
            CREATE TABLE IF NOT EXISTS import_orders (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                supplier_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                created_at DATETIME NOT NULL,
                total_cost DECIMAL(19,2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                status_note TEXT NULL,
                notes VARCHAR(255) NULL,
                created_by_name_snapshot VARCHAR(255) NULL,
                supplier_name_snapshot VARCHAR(255) NULL,
                CONSTRAINT fk_import_orders_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
                CONSTRAINT fk_import_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """);
        execute(connection, """
            CREATE TABLE IF NOT EXISTS import_order_items (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                import_order_id BIGINT NOT NULL,
                product_id BIGINT NOT NULL,
                quantity INT NOT NULL,
                import_price DECIMAL(19,2) NOT NULL,
                product_name_snapshot VARCHAR(255) NULL,
                sku_snapshot VARCHAR(255) NULL,
                barcode_snapshot VARCHAR(255) NULL,
                category_name_snapshot VARCHAR(255) NULL,
                brand_name_snapshot VARCHAR(255) NULL,
                origin_name_snapshot VARCHAR(255) NULL,
                unit_name_snapshot VARCHAR(255) NULL,
                CONSTRAINT fk_import_order_items_order FOREIGN KEY (import_order_id) REFERENCES import_orders(id),
                CONSTRAINT fk_import_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
            )
            """);
    }

    private void ensureAccountAuditTable(Connection connection) throws SQLException {
        execute(connection, """
            CREATE TABLE IF NOT EXISTS account_audit_logs (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                actor_user_id BIGINT NULL,
                target_user_id BIGINT NOT NULL,
                action VARCHAR(50) NOT NULL,
                details TEXT NULL,
                created_at DATETIME NOT NULL,
                CONSTRAINT fk_account_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users(id),
                CONSTRAINT fk_account_audit_logs_target FOREIGN KEY (target_user_id) REFERENCES users(id)
            )
            """);
    }

    private void ensureInventoryPositionBaselinesTable(Connection connection) throws SQLException {
        execute(connection, """
            CREATE TABLE IF NOT EXISTS inventory_position_baselines (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                product_id BIGINT NOT NULL UNIQUE,
                baseline_at DATETIME NOT NULL,
                quantity INT NOT NULL,
                inventory_value DECIMAL(19,2) NOT NULL,
                average_cost DECIMAL(19,2) NOT NULL,
                CONSTRAINT fk_inventory_position_baselines_product
                    FOREIGN KEY (product_id) REFERENCES products(id)
            )
            """);
    }

    private void ensureInventoryTransactionsTable(Connection connection) throws SQLException {
        execute(connection, """
            CREATE TABLE IF NOT EXISTS inventory_transactions (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                product_id BIGINT NOT NULL,
                quantity_change INT NOT NULL,
                transaction_type VARCHAR(50) NOT NULL,
                reference_id BIGINT NULL,
                order_id BIGINT NULL,
                import_order_id BIGINT NULL,
                user_id BIGINT NULL,
                notes TEXT NULL,
                unit_cost_snapshot DECIMAL(19,2) NULL,
                inventory_value_change DECIMAL(19,2) NULL,
                created_at DATETIME NOT NULL,
                CONSTRAINT fk_inventory_transactions_product FOREIGN KEY (product_id) REFERENCES products(id),
                CONSTRAINT fk_inventory_transactions_order FOREIGN KEY (order_id) REFERENCES orders(id),
                CONSTRAINT fk_inventory_transactions_import_order FOREIGN KEY (import_order_id) REFERENCES import_orders(id),
                CONSTRAINT fk_inventory_transactions_user FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """);
    }

    private void ensureOperationalAuditTable(Connection connection) throws SQLException {
        execute(connection, """
            CREATE TABLE IF NOT EXISTS operational_audit_logs (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                actor_user_id BIGINT NULL,
                action VARCHAR(50) NOT NULL,
                subject_type VARCHAR(50) NOT NULL,
                subject_id BIGINT NOT NULL,
                subject_label VARCHAR(255) NULL,
                details TEXT NULL,
                created_at DATETIME NOT NULL,
                CONSTRAINT fk_operational_audit_logs_actor
                    FOREIGN KEY (actor_user_id) REFERENCES users(id)
            )
            """);
    }

    private void ensureStocktakeTables(Connection connection) throws SQLException {
        execute(connection, """
            CREATE TABLE IF NOT EXISTS stocktake_sessions (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                created_by_user_id BIGINT NOT NULL,
                scope_type VARCHAR(30) NOT NULL,
                category_id BIGINT NULL,
                status VARCHAR(30) NOT NULL,
                notes TEXT NULL,
                created_at DATETIME NOT NULL,
                applied_at DATETIME NULL,
                CONSTRAINT fk_stocktake_sessions_created_by
                    FOREIGN KEY (created_by_user_id) REFERENCES users(id),
                CONSTRAINT fk_stocktake_sessions_category
                    FOREIGN KEY (category_id) REFERENCES categories(id)
            )
            """);
        execute(connection, """
            CREATE TABLE IF NOT EXISTS stocktake_items (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                session_id BIGINT NOT NULL,
                product_id BIGINT NOT NULL,
                system_quantity INT NOT NULL,
                unit_cost_snapshot DECIMAL(19,2) NOT NULL,
                counted_quantity INT NULL,
                notes TEXT NULL,
                CONSTRAINT fk_stocktake_items_session
                    FOREIGN KEY (session_id) REFERENCES stocktake_sessions(id),
                CONSTRAINT fk_stocktake_items_product
                    FOREIGN KEY (product_id) REFERENCES products(id)
            )
            """);
    }

    private void ensureProductColumns(Connection connection) throws SQLException {
        if (tableExists(connection, "products")) {
            modifyColumnIfExists(connection, "products", "price", "DECIMAL(19,2) NOT NULL");
            modifyColumnIfExists(connection, "products", "import_price", "DECIMAL(19,2) NULL");
            addColumnIfMissing(connection, "products", "version", "BIGINT NOT NULL DEFAULT 0");
        }
    }

    private void ensureOrderColumns(Connection connection) throws SQLException {
        if (tableExists(connection, "orders")) {
            modifyColumnIfExists(connection, "orders", "total_price", "DECIMAL(19,2) NOT NULL");
            addColumnIfMissing(connection, "orders", "refunded_amount", "DECIMAL(19,2) NOT NULL DEFAULT 0.00");
            addColumnIfMissing(connection, "orders", "status_note", "TEXT NULL");
            addColumnIfMissing(connection, "orders", "created_by_name_snapshot", "VARCHAR(255) NULL");
        }
        if (tableExists(connection, "order_items")) {
            modifyColumnIfExists(connection, "order_items", "price", "DECIMAL(19,2) NOT NULL");
            addColumnIfMissing(connection, "order_items", "cost_at_sale", "DECIMAL(19,2) NULL");
            addColumnIfMissing(connection, "order_items", "returned_quantity", "INT NOT NULL DEFAULT 0");
            addColumnIfMissing(connection, "order_items", "product_name_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "order_items", "sku_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "order_items", "barcode_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "order_items", "category_name_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "order_items", "brand_name_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "order_items", "origin_name_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "order_items", "unit_name_snapshot", "VARCHAR(255) NULL");
        }
    }

    private void ensureImportColumns(Connection connection) throws SQLException {
        if (tableExists(connection, "import_orders")) {
            modifyColumnIfExists(connection, "import_orders", "total_cost", "DECIMAL(19,2) NOT NULL");
            addColumnIfMissing(connection, "import_orders", "created_by_name_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "import_orders", "supplier_name_snapshot", "VARCHAR(255) NULL");
        }
        if (tableExists(connection, "import_order_items")) {
            modifyColumnIfExists(connection, "import_order_items", "import_price", "DECIMAL(19,2) NOT NULL");
            addColumnIfMissing(connection, "import_order_items", "product_name_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "import_order_items", "sku_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "import_order_items", "barcode_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "import_order_items", "category_name_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "import_order_items", "brand_name_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "import_order_items", "origin_name_snapshot", "VARCHAR(255) NULL");
            addColumnIfMissing(connection, "import_order_items", "unit_name_snapshot", "VARCHAR(255) NULL");
        }
    }

    private void ensureLedgerColumns(Connection connection) throws SQLException {
        if (tableExists(connection, "inventory_transactions")) {
            modifyColumnIfExists(connection, "inventory_transactions", "transaction_type", "VARCHAR(50) NOT NULL");
            addColumnIfMissing(connection, "inventory_transactions", "order_id", "BIGINT NULL");
            addColumnIfMissing(connection, "inventory_transactions", "import_order_id", "BIGINT NULL");
            addColumnIfMissing(connection, "inventory_transactions", "unit_cost_snapshot", "DECIMAL(19,2) NULL");
            addColumnIfMissing(connection, "inventory_transactions", "inventory_value_change", "DECIMAL(19,2) NULL");
        }
        if (tableExists(connection, "account_audit_logs")) {
            modifyColumnIfExists(connection, "account_audit_logs", "action", "VARCHAR(50) NOT NULL");
        }
        if (tableExists(connection, "users")) {
            modifyColumnIfExists(connection, "users", "role", "VARCHAR(20) NOT NULL");
        }
    }

    private void ensureIndexes(Connection connection) throws SQLException {
        addIndexIfMissing(connection, "orders", "idx_orders_created_at", "created_at");
        addIndexIfMissing(connection, "orders", "idx_orders_status", "status");
        addIndexIfMissing(connection, "orders", "idx_orders_user_created_at", "user_id, created_at");
        addIndexIfMissing(connection, "order_items", "idx_order_items_order_id", "order_id");
        addIndexIfMissing(connection, "order_items", "idx_order_items_product_id", "product_id");
        addIndexIfMissing(connection, "import_orders", "idx_import_orders_created_at", "created_at");
        addIndexIfMissing(connection, "import_orders", "idx_import_orders_status", "status");
        addIndexIfMissing(connection, "import_orders", "idx_import_orders_supplier_created_at", "supplier_id, created_at");
        addIndexIfMissing(connection, "import_order_items", "idx_import_order_items_import_order_id", "import_order_id");
        addIndexIfMissing(connection, "import_order_items", "idx_import_order_items_product_id", "product_id");
        addIndexIfMissing(connection, "inventory_transactions", "idx_inventory_transactions_product_created_id", "product_id, created_at, id");
        addIndexIfMissing(connection, "inventory_transactions", "idx_inventory_transactions_order_id", "order_id");
        addIndexIfMissing(connection, "inventory_transactions", "idx_inventory_transactions_import_order_id", "import_order_id");
        addIndexIfMissing(connection, "inventory_transactions", "idx_inventory_transactions_user_id", "user_id");
        addIndexIfMissing(connection, "inventory_transactions", "idx_inventory_transactions_type_created", "transaction_type, created_at");
        addIndexIfMissing(connection, "account_audit_logs", "idx_account_audit_logs_created_at", "created_at");
        addIndexIfMissing(connection, "account_audit_logs", "idx_account_audit_logs_actor_user_id", "actor_user_id");
        addIndexIfMissing(connection, "account_audit_logs", "idx_account_audit_logs_target_user_id", "target_user_id");
        addIndexIfMissing(connection, "products", "idx_products_category_id", "category_id");
        addIndexIfMissing(connection, "products", "idx_products_brand_id", "brand_id");
        addIndexIfMissing(connection, "operational_audit_logs", "idx_operational_audit_logs_created_at", "created_at");
        addIndexIfMissing(connection, "operational_audit_logs", "idx_operational_audit_logs_actor_user_id", "actor_user_id");
        addIndexIfMissing(connection, "operational_audit_logs", "idx_operational_audit_logs_action_created", "action, created_at");
        addIndexIfMissing(connection, "operational_audit_logs", "idx_operational_audit_logs_subject_type", "subject_type");
        addIndexIfMissing(connection, "stocktake_sessions", "idx_stocktake_sessions_created_at", "created_at");
        addIndexIfMissing(connection, "stocktake_sessions", "idx_stocktake_sessions_status", "status");
        addIndexIfMissing(connection, "stocktake_sessions", "idx_stocktake_sessions_created_by_user_id", "created_by_user_id");
        addIndexIfMissing(connection, "stocktake_sessions", "idx_stocktake_sessions_category_id", "category_id");
        addIndexIfMissing(connection, "stocktake_items", "idx_stocktake_items_session_id", "session_id");
        addIndexIfMissing(connection, "stocktake_items", "idx_stocktake_items_product_id", "product_id");
    }

    private void ensureChecks(Connection connection) throws SQLException {
        addCheckIfMissing(connection, "products", "chk_products_non_negative",
            "quantity >= 0 and price >= 0 and (import_price is null or import_price >= 0) and min_stock_level >= 0");
        addCheckIfMissing(connection, "orders", "chk_orders_amounts",
            "total_price >= 0 and refunded_amount >= 0 and refunded_amount <= total_price");
        addCheckIfMissing(connection, "order_items", "chk_order_items_values",
            "quantity > 0 and returned_quantity >= 0 and returned_quantity <= quantity and price >= 0 and (cost_at_sale is null or cost_at_sale >= 0)");
        addCheckIfMissing(connection, "import_orders", "chk_import_orders_total_cost", "total_cost >= 0");
        addCheckIfMissing(connection, "import_order_items", "chk_import_order_items_values", "quantity > 0 and import_price >= 0");
        addCheckIfMissing(connection, "inventory_transactions", "chk_inventory_transactions_cost", "unit_cost_snapshot is null or unit_cost_snapshot >= 0");
    }

    private void backfillSnapshots(Connection connection) throws SQLException {
        if (tableExists(connection, "orders") && columnExists(connection, "orders", "created_by_name_snapshot")) {
            execute(connection, """
                UPDATE orders o
                SET o.created_by_name_snapshot = COALESCE(
                    NULLIF(o.created_by_name_snapshot, ''),
                    (
                        SELECT NULLIF(u.full_name, '')
                        FROM users u
                        WHERE u.id = o.user_id
                    ),
                    (
                        SELECT u.username
                        FROM users u
                        WHERE u.id = o.user_id
                    )
                )
                WHERE o.created_by_name_snapshot IS NULL OR o.created_by_name_snapshot = ''
                """);
        }

        if (tableExists(connection, "import_orders")) {
            if (columnExists(connection, "import_orders", "created_by_name_snapshot")) {
                execute(connection, """
                    UPDATE import_orders io
                    SET io.created_by_name_snapshot = COALESCE(
                        NULLIF(io.created_by_name_snapshot, ''),
                        (
                            SELECT NULLIF(u.full_name, '')
                            FROM users u
                            WHERE u.id = io.user_id
                        ),
                        (
                            SELECT u.username
                            FROM users u
                            WHERE u.id = io.user_id
                        )
                    )
                    WHERE io.created_by_name_snapshot IS NULL OR io.created_by_name_snapshot = ''
                    """);
            }
            if (columnExists(connection, "import_orders", "supplier_name_snapshot")) {
                execute(connection, """
                    UPDATE import_orders io
                    SET io.supplier_name_snapshot = COALESCE(
                        NULLIF(io.supplier_name_snapshot, ''),
                        (
                            SELECT s.name
                            FROM suppliers s
                            WHERE s.id = io.supplier_id
                        )
                    )
                    WHERE io.supplier_name_snapshot IS NULL OR io.supplier_name_snapshot = ''
                    """);
            }
        }

        if (tableExists(connection, "order_items")) {
            execute(connection, """
                UPDATE order_items oi
                SET
                    oi.product_name_snapshot = COALESCE(
                        NULLIF(oi.product_name_snapshot, ''),
                        (
                            SELECT p.name
                            FROM products p
                            WHERE p.id = oi.product_id
                        )
                    ),
                    oi.sku_snapshot = COALESCE(
                        NULLIF(oi.sku_snapshot, ''),
                        (
                            SELECT p.sku
                            FROM products p
                            WHERE p.id = oi.product_id
                        )
                    ),
                    oi.barcode_snapshot = COALESCE(
                        NULLIF(oi.barcode_snapshot, ''),
                        (
                            SELECT p.barcode
                            FROM products p
                            WHERE p.id = oi.product_id
                        )
                    ),
                    oi.category_name_snapshot = COALESCE(
                        NULLIF(oi.category_name_snapshot, ''),
                        (
                            SELECT c.name
                            FROM products p
                            LEFT JOIN categories c ON c.id = p.category_id
                            WHERE p.id = oi.product_id
                        )
                    ),
                    oi.brand_name_snapshot = COALESCE(
                        NULLIF(oi.brand_name_snapshot, ''),
                        (
                            SELECT b.name
                            FROM products p
                            LEFT JOIN brands b ON b.id = p.brand_id
                            WHERE p.id = oi.product_id
                        )
                    ),
                    oi.origin_name_snapshot = COALESCE(
                        NULLIF(oi.origin_name_snapshot, ''),
                        (
                            SELECT o.name
                            FROM products p
                            LEFT JOIN origins o ON o.id = p.origin_id
                            WHERE p.id = oi.product_id
                        )
                    ),
                    oi.unit_name_snapshot = COALESCE(
                        NULLIF(oi.unit_name_snapshot, ''),
                        (
                            SELECT u.name
                            FROM products p
                            LEFT JOIN units u ON u.id = p.unit_id
                            WHERE p.id = oi.product_id
                        )
                    )
                WHERE
                    oi.product_name_snapshot IS NULL OR oi.product_name_snapshot = ''
                    OR oi.sku_snapshot IS NULL OR oi.sku_snapshot = ''
                    OR oi.barcode_snapshot IS NULL OR oi.barcode_snapshot = ''
                    OR oi.category_name_snapshot IS NULL OR oi.category_name_snapshot = ''
                    OR oi.brand_name_snapshot IS NULL OR oi.brand_name_snapshot = ''
                    OR oi.origin_name_snapshot IS NULL OR oi.origin_name_snapshot = ''
                    OR oi.unit_name_snapshot IS NULL OR oi.unit_name_snapshot = ''
                """);
        }

        if (tableExists(connection, "import_order_items")) {
            execute(connection, """
                UPDATE import_order_items ioi
                SET
                    ioi.product_name_snapshot = COALESCE(
                        NULLIF(ioi.product_name_snapshot, ''),
                        (
                            SELECT p.name
                            FROM products p
                            WHERE p.id = ioi.product_id
                        )
                    ),
                    ioi.sku_snapshot = COALESCE(
                        NULLIF(ioi.sku_snapshot, ''),
                        (
                            SELECT p.sku
                            FROM products p
                            WHERE p.id = ioi.product_id
                        )
                    ),
                    ioi.barcode_snapshot = COALESCE(
                        NULLIF(ioi.barcode_snapshot, ''),
                        (
                            SELECT p.barcode
                            FROM products p
                            WHERE p.id = ioi.product_id
                        )
                    ),
                    ioi.category_name_snapshot = COALESCE(
                        NULLIF(ioi.category_name_snapshot, ''),
                        (
                            SELECT c.name
                            FROM products p
                            LEFT JOIN categories c ON c.id = p.category_id
                            WHERE p.id = ioi.product_id
                        )
                    ),
                    ioi.brand_name_snapshot = COALESCE(
                        NULLIF(ioi.brand_name_snapshot, ''),
                        (
                            SELECT b.name
                            FROM products p
                            LEFT JOIN brands b ON b.id = p.brand_id
                            WHERE p.id = ioi.product_id
                        )
                    ),
                    ioi.origin_name_snapshot = COALESCE(
                        NULLIF(ioi.origin_name_snapshot, ''),
                        (
                            SELECT o.name
                            FROM products p
                            LEFT JOIN origins o ON o.id = p.origin_id
                            WHERE p.id = ioi.product_id
                        )
                    ),
                    ioi.unit_name_snapshot = COALESCE(
                        NULLIF(ioi.unit_name_snapshot, ''),
                        (
                            SELECT u.name
                            FROM products p
                            LEFT JOIN units u ON u.id = p.unit_id
                            WHERE p.id = ioi.product_id
                        )
                    )
                WHERE
                    ioi.product_name_snapshot IS NULL OR ioi.product_name_snapshot = ''
                    OR ioi.sku_snapshot IS NULL OR ioi.sku_snapshot = ''
                    OR ioi.barcode_snapshot IS NULL OR ioi.barcode_snapshot = ''
                    OR ioi.category_name_snapshot IS NULL OR ioi.category_name_snapshot = ''
                    OR ioi.brand_name_snapshot IS NULL OR ioi.brand_name_snapshot = ''
                    OR ioi.origin_name_snapshot IS NULL OR ioi.origin_name_snapshot = ''
                    OR ioi.unit_name_snapshot IS NULL OR ioi.unit_name_snapshot = ''
                """);
        }
    }

    private void addColumnIfMissing(Connection connection, String tableName, String columnName, String definition) throws SQLException {
        if (tableExists(connection, tableName) && !columnExists(connection, tableName, columnName)) {
            execute(connection, "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void modifyColumnIfExists(Connection connection, String tableName, String columnName, String definition) throws SQLException {
        if (tableExists(connection, tableName) && columnExists(connection, tableName, columnName)) {
            String alterSql;
            if (isH2(connection)) {
                alterSql = "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " " + definition;
            } else {
                alterSql = "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + definition;
            }
            execute(connection, alterSql);
        }
    }

    private void addIndexIfMissing(Connection connection, String tableName, String indexName, String columnList) throws SQLException {
        if (tableExists(connection, tableName) && !indexExists(connection, tableName, indexName)) {
            execute(connection, "CREATE INDEX " + indexName + " ON " + tableName + " (" + columnList + ")");
        }
    }

    private void addCheckIfMissing(Connection connection, String tableName, String constraintName, String expression) throws SQLException {
        if (tableExists(connection, tableName) && !constraintExists(connection, tableName, constraintName)) {
            execute(connection, "ALTER TABLE " + tableName + " ADD CONSTRAINT " + constraintName + " CHECK (" + expression + ")");
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        for (String candidate : new String[]{tableName, tableName.toUpperCase(Locale.ROOT), tableName.toLowerCase(Locale.ROOT)}) {
            try (ResultSet rs = metaData.getTables(catalog, null, candidate, new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        for (String tableCandidate : new String[]{tableName, tableName.toUpperCase(Locale.ROOT), tableName.toLowerCase(Locale.ROOT)}) {
            for (String columnCandidate : new String[]{columnName, columnName.toUpperCase(Locale.ROOT), columnName.toLowerCase(Locale.ROOT)}) {
                try (ResultSet rs = metaData.getColumns(catalog, null, tableCandidate, columnCandidate)) {
                    if (rs.next()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        for (String tableCandidate : new String[]{tableName, tableName.toUpperCase(Locale.ROOT), tableName.toLowerCase(Locale.ROOT)}) {
            try (ResultSet rs = metaData.getIndexInfo(catalog, null, tableCandidate, false, false)) {
                while (rs.next()) {
                    String existing = rs.getString("INDEX_NAME");
                    if (existing != null && existing.equalsIgnoreCase(indexName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean constraintExists(Connection connection, String tableName, String constraintName) throws SQLException {
        String[] queries = {
            """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
            WHERE lower(table_name) = lower(?)
              AND lower(constraint_name) = lower(?)
            """,
            """
            SELECT COUNT(*)
            FROM information_schema.table_constraints
            WHERE lower(table_name) = lower(?)
              AND lower(constraint_name) = lower(?)
            """
        };
        for (String query : queries) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, tableName);
                statement.setString(2, constraintName);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                }
            } catch (SQLException ignored) {
                // Some engines or modes expose constraint metadata differently.
            }
        }
        return false;
    }

    private boolean isH2(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("h2");
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
