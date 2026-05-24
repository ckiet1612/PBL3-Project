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

public class V12__add_qr_payments extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        if (!tableExists(connection, "qr_payments")) {
            execute(connection, """
                CREATE TABLE qr_payments (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    order_code BIGINT NULL,
                    payment_link_id VARCHAR(100) NULL,
                    status VARCHAR(30) NOT NULL,
                    amount DECIMAL(19, 2) NOT NULL,
                    qr_code TEXT NULL,
                    checkout_url VARCHAR(1000) NULL,
                    cart_snapshot_json TEXT NOT NULL,
                    user_id BIGINT NOT NULL,
                    customer_id BIGINT NULL,
                    promotion_id BIGINT NULL,
                    created_at DATETIME NOT NULL,
                    expires_at DATETIME NOT NULL,
                    paid_at DATETIME NULL,
                    updated_at DATETIME NOT NULL,
                    failure_reason TEXT NULL,
                    created_order_id BIGINT NULL,
                    CONSTRAINT fk_qr_payments_user
                        FOREIGN KEY (user_id) REFERENCES users(id),
                    CONSTRAINT fk_qr_payments_customer
                        FOREIGN KEY (customer_id) REFERENCES customers(id),
                    CONSTRAINT fk_qr_payments_created_order
                        FOREIGN KEY (created_order_id) REFERENCES orders(id)
                )
                """);
        }

        createIndexIfMissing(connection, "qr_payments", "idx_qr_payments_order_code", "CREATE UNIQUE INDEX idx_qr_payments_order_code ON qr_payments(order_code)");
        createIndexIfMissing(connection, "qr_payments", "idx_qr_payments_payment_link_id", "CREATE INDEX idx_qr_payments_payment_link_id ON qr_payments(payment_link_id)");
        createIndexIfMissing(connection, "qr_payments", "idx_qr_payments_status", "CREATE INDEX idx_qr_payments_status ON qr_payments(status)");
        createIndexIfMissing(connection, "qr_payments", "idx_qr_payments_user_created_at", "CREATE INDEX idx_qr_payments_user_created_at ON qr_payments(user_id, created_at)");
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

    private void createIndexIfMissing(Connection connection, String tableName, String indexName, String sql) throws SQLException {
        if (!indexExists(connection, tableName, indexName)) {
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
            VALUES ('schema_version', '12', 'Application-managed database schema version marker.')
            """)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO system_settings (setting_key, setting_value, description)
                VALUES ('schema_version', '12', 'Application-managed database schema version marker.')
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
