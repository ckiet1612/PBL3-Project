package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class V3__add_customers_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        ensureCustomersTable(connection);
        ensureOrdersCustomerColumns(connection);
        ensureIndexes(connection);
        ensureForeignKey(connection);
    }

    private void ensureCustomersTable(Connection connection) throws SQLException {
        execute(connection, """
            CREATE TABLE IF NOT EXISTS customers (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                full_name VARCHAR(255) NOT NULL,
                phone VARCHAR(255) NOT NULL,
                enabled BOOLEAN NOT NULL DEFAULT TRUE,
                created_at DATETIME NOT NULL,
                updated_at DATETIME NOT NULL
            )
            """);
    }

    private void ensureOrdersCustomerColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "orders", "customer_id", "BIGINT NULL");
        addColumnIfMissing(connection, "orders", "customer_name_snapshot", "VARCHAR(255) NULL");
        addColumnIfMissing(connection, "orders", "customer_phone_snapshot", "VARCHAR(255) NULL");
    }

    private void ensureIndexes(Connection connection) throws SQLException {
        createIndexIfMissing(connection, "customers", "ux_customers_phone", "CREATE UNIQUE INDEX ux_customers_phone ON customers(phone)");
        createIndexIfMissing(connection, "customers", "idx_customers_enabled", "CREATE INDEX idx_customers_enabled ON customers(enabled)");
        createIndexIfMissing(connection, "customers", "idx_customers_created_at", "CREATE INDEX idx_customers_created_at ON customers(created_at)");
        createIndexIfMissing(connection, "orders", "idx_orders_customer_created_at", "CREATE INDEX idx_orders_customer_created_at ON orders(customer_id, created_at)");
    }

    private void ensureForeignKey(Connection connection) throws SQLException {
        if (!tableExists(connection, "orders") || !tableExists(connection, "customers") || foreignKeyExists(connection, "orders", "fk_orders_customer")) {
            return;
        }
        execute(connection, "ALTER TABLE orders ADD CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id)");
    }

    private void addColumnIfMissing(Connection connection, String tableName, String columnName, String definition) throws SQLException {
        if (!tableExists(connection, tableName) || columnExists(connection, tableName, columnName)) {
            return;
        }
        execute(connection, "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
    }

    private void createIndexIfMissing(Connection connection, String tableName, String indexName, String sql) throws SQLException {
        if (!tableExists(connection, tableName) || indexExists(connection, tableName, indexName)) {
            return;
        }
        execute(connection, sql);
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
                    String currentIndex = rs.getString("INDEX_NAME");
                    if (currentIndex != null && currentIndex.equalsIgnoreCase(indexName)) {
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
        for (String tableCandidate : new String[]{tableName, tableName.toUpperCase(Locale.ROOT), tableName.toLowerCase(Locale.ROOT)}) {
            try (ResultSet rs = metaData.getImportedKeys(catalog, null, tableCandidate)) {
                while (rs.next()) {
                    String currentKey = rs.getString("FK_NAME");
                    if (currentKey != null && currentKey.equalsIgnoreCase(foreignKeyName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
