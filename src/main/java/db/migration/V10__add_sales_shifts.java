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

public class V10__add_sales_shifts extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        if (!tableExists(connection, "sales_shifts")) {
            execute(connection, """
                CREATE TABLE sales_shifts (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    opened_by_user_id BIGINT NOT NULL,
                    opened_by_name_snapshot VARCHAR(255) NULL,
                    opened_by_username_snapshot VARCHAR(255) NULL,
                    opened_at DATETIME NOT NULL,
                    opening_cash_amount DECIMAL(19, 2) NOT NULL,
                    open_note TEXT NULL,
                    status VARCHAR(20) NOT NULL,
                    closed_at DATETIME NULL,
                    closed_by_user_id BIGINT NULL,
                    closed_by_name_snapshot VARCHAR(255) NULL,
                    closed_by_username_snapshot VARCHAR(255) NULL,
                    closing_cash_actual DECIMAL(19, 2) NULL,
                    expected_cash_amount DECIMAL(19, 2) NULL,
                    cash_variance_amount DECIMAL(19, 2) NULL,
                    close_note TEXT NULL,
                    sales_revenue_amount DECIMAL(19, 2) NULL,
                    refund_amount DECIMAL(19, 2) NULL,
                    expense_amount DECIMAL(19, 2) NULL,
                    cash_sales_amount DECIMAL(19, 2) NULL,
                    cash_refund_amount DECIMAL(19, 2) NULL,
                    cash_expense_amount DECIMAL(19, 2) NULL,
                    order_count BIGINT NULL,
                    refund_count BIGINT NULL,
                    CONSTRAINT fk_sales_shifts_opened_by
                        FOREIGN KEY (opened_by_user_id) REFERENCES users(id),
                    CONSTRAINT fk_sales_shifts_closed_by
                        FOREIGN KEY (closed_by_user_id) REFERENCES users(id)
                )
                """);
        }
        createIndexIfMissing(connection, "sales_shifts", "idx_sales_shifts_opened_by_status", "CREATE INDEX idx_sales_shifts_opened_by_status ON sales_shifts(opened_by_user_id, status)");
        createIndexIfMissing(connection, "sales_shifts", "idx_sales_shifts_opened_at", "CREATE INDEX idx_sales_shifts_opened_at ON sales_shifts(opened_at)");
        createIndexIfMissing(connection, "sales_shifts", "idx_sales_shifts_closed_at", "CREATE INDEX idx_sales_shifts_closed_at ON sales_shifts(closed_at)");
        createIndexIfMissing(connection, "sales_shifts", "idx_sales_shifts_status", "CREATE INDEX idx_sales_shifts_status ON sales_shifts(status)");

        addColumnIfMissing(connection, "orders", "sales_shift_id", "ALTER TABLE orders ADD COLUMN sales_shift_id BIGINT NULL");
        createIndexIfMissing(connection, "orders", "idx_orders_sales_shift", "CREATE INDEX idx_orders_sales_shift ON orders(sales_shift_id)");
        createForeignKeyIfMissing(connection, "orders", "fk_orders_sales_shift", """
            ALTER TABLE orders
            ADD CONSTRAINT fk_orders_sales_shift
            FOREIGN KEY (sales_shift_id) REFERENCES sales_shifts(id)
            """);

        if (!tableExists(connection, "sales_shift_refunds")) {
            execute(connection, """
                CREATE TABLE sales_shift_refunds (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    shift_id BIGINT NOT NULL,
                    order_id BIGINT NOT NULL,
                    processed_by_user_id BIGINT NOT NULL,
                    payment_method VARCHAR(20) NOT NULL,
                    amount DECIMAL(19, 2) NOT NULL,
                    created_at DATETIME NOT NULL,
                    reason TEXT NULL,
                    CONSTRAINT fk_sales_shift_refunds_shift
                        FOREIGN KEY (shift_id) REFERENCES sales_shifts(id),
                    CONSTRAINT fk_sales_shift_refunds_order
                        FOREIGN KEY (order_id) REFERENCES orders(id),
                    CONSTRAINT fk_sales_shift_refunds_processed_by
                        FOREIGN KEY (processed_by_user_id) REFERENCES users(id)
                )
                """);
        }
        createIndexIfMissing(connection, "sales_shift_refunds", "idx_sales_shift_refunds_shift", "CREATE INDEX idx_sales_shift_refunds_shift ON sales_shift_refunds(shift_id)");
        createIndexIfMissing(connection, "sales_shift_refunds", "idx_sales_shift_refunds_order", "CREATE INDEX idx_sales_shift_refunds_order ON sales_shift_refunds(order_id)");
        createIndexIfMissing(connection, "sales_shift_refunds", "idx_sales_shift_refunds_processed_by", "CREATE INDEX idx_sales_shift_refunds_processed_by ON sales_shift_refunds(processed_by_user_id)");
        createIndexIfMissing(connection, "sales_shift_refunds", "idx_sales_shift_refunds_created_at", "CREATE INDEX idx_sales_shift_refunds_created_at ON sales_shift_refunds(created_at)");

        upsertSchemaVersion(connection);
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        for (String candidate : candidates(tableName)) {
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
        for (String tableCandidate : candidates(tableName)) {
            for (String columnCandidate : candidates(columnName)) {
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
        for (String tableCandidate : candidates(tableName)) {
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

    private boolean foreignKeyExists(Connection connection, String tableName, String foreignKeyName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        for (String tableCandidate : candidates(tableName)) {
            try (ResultSet rs = metaData.getImportedKeys(catalog, null, tableCandidate)) {
                while (rs.next()) {
                    String existing = rs.getString("FK_NAME");
                    if (existing != null && existing.equalsIgnoreCase(foreignKeyName)) {
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

    private void createForeignKeyIfMissing(Connection connection, String tableName, String foreignKeyName, String sql) throws SQLException {
        if (!foreignKeyExists(connection, tableName, foreignKeyName)) {
            execute(connection, sql);
        }
    }

    private void upsertSchemaVersion(Connection connection) throws SQLException {
        if (!tableExists(connection, "system_settings")) {
            execute(connection, """
                CREATE TABLE system_settings (
                    setting_key VARCHAR(100) NOT NULL PRIMARY KEY,
                    setting_value VARCHAR(255) NOT NULL,
                    description VARCHAR(500) NOT NULL DEFAULT '',
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        }
        try (PreparedStatement statement = connection.prepareStatement("""
            MERGE INTO system_settings (setting_key, setting_value, description)
            KEY(setting_key)
            VALUES ('schema_version', '10', 'Application-managed database schema version marker.')
            """)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO system_settings (setting_key, setting_value, description)
                VALUES ('schema_version', '10', 'Application-managed database schema version marker.')
                ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), description = VALUES(description)
                """)) {
                statement.executeUpdate();
            }
        }
    }

    private String[] candidates(String value) {
        return new String[]{value, value.toUpperCase(Locale.ROOT), value.toLowerCase(Locale.ROOT)};
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
