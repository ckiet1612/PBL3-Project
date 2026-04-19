package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class V7__add_promotions extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        if (!tableExists(connection, "promotions")) {
            execute(connection, """
                CREATE TABLE promotions (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    scope VARCHAR(20) NOT NULL,
                    discount_type VARCHAR(20) NOT NULL,
                    discount_value DECIMAL(19, 2) NOT NULL,
                    enabled BOOLEAN NOT NULL,
                    starts_at DATETIME NULL,
                    ends_at DATETIME NULL,
                    target_product_id BIGINT NULL,
                    min_order_total DECIMAL(19, 2) NULL,
                    created_by_user_id BIGINT NOT NULL,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL,
                    CONSTRAINT fk_promotions_target_product
                        FOREIGN KEY (target_product_id) REFERENCES products(id),
                    CONSTRAINT fk_promotions_created_by_user
                        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
                )
                """);
        }
        createIndexIfMissing(connection, "promotions", "idx_promotions_scope_enabled", "CREATE INDEX idx_promotions_scope_enabled ON promotions(scope, enabled)");
        createIndexIfMissing(connection, "promotions", "idx_promotions_schedule", "CREATE INDEX idx_promotions_schedule ON promotions(starts_at, ends_at)");
        createIndexIfMissing(connection, "promotions", "idx_promotions_target_product", "CREATE INDEX idx_promotions_target_product ON promotions(target_product_id)");
        createIndexIfMissing(connection, "promotions", "idx_promotions_created_by_user", "CREATE INDEX idx_promotions_created_by_user ON promotions(created_by_user_id)");

        addColumnIfMissing(connection, "orders", "gross_subtotal", "ALTER TABLE orders ADD COLUMN gross_subtotal DECIMAL(19, 2) NULL");
        addColumnIfMissing(connection, "orders", "discount_total", "ALTER TABLE orders ADD COLUMN discount_total DECIMAL(19, 2) NULL");
        addColumnIfMissing(connection, "orders", "order_level_discount_total", "ALTER TABLE orders ADD COLUMN order_level_discount_total DECIMAL(19, 2) NULL");
        addColumnIfMissing(connection, "orders", "applied_order_promotion_id_snapshot", "ALTER TABLE orders ADD COLUMN applied_order_promotion_id_snapshot BIGINT NULL");
        addColumnIfMissing(connection, "orders", "applied_order_promotion_name_snapshot", "ALTER TABLE orders ADD COLUMN applied_order_promotion_name_snapshot VARCHAR(255) NULL");
        createIndexIfMissing(connection, "orders", "idx_orders_applied_order_promotion_id_snapshot", "CREATE INDEX idx_orders_applied_order_promotion_id_snapshot ON orders(applied_order_promotion_id_snapshot)");

        addColumnIfMissing(connection, "order_items", "original_unit_price", "ALTER TABLE order_items ADD COLUMN original_unit_price DECIMAL(19, 2) NULL");
        addColumnIfMissing(connection, "order_items", "line_promotion_discount_amount", "ALTER TABLE order_items ADD COLUMN line_promotion_discount_amount DECIMAL(19, 2) NULL");
        addColumnIfMissing(connection, "order_items", "order_level_discount_allocated_amount", "ALTER TABLE order_items ADD COLUMN order_level_discount_allocated_amount DECIMAL(19, 2) NULL");
        addColumnIfMissing(connection, "order_items", "applied_product_promotion_id_snapshot", "ALTER TABLE order_items ADD COLUMN applied_product_promotion_id_snapshot BIGINT NULL");
        addColumnIfMissing(connection, "order_items", "applied_product_promotion_name_snapshot", "ALTER TABLE order_items ADD COLUMN applied_product_promotion_name_snapshot VARCHAR(255) NULL");
        createIndexIfMissing(connection, "order_items", "idx_order_items_applied_product_promotion_id_snapshot", "CREATE INDEX idx_order_items_applied_product_promotion_id_snapshot ON order_items(applied_product_promotion_id_snapshot)");
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

    private void addColumnIfMissing(Connection connection, String tableName, String columnName, String sql) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, sql);
        }
    }

    private void createIndexIfMissing(Connection connection, String tableName, String indexName, String sql) throws SQLException {
        if (!indexExists(connection, tableName, indexName)) {
            execute(connection, sql);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
