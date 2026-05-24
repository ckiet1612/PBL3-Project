package db.migration;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class V11UniqueProductBarcodesTest {

    @Test
    void migrateNormalizesBlankBarcodesAndCreatesUniqueIndex() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:v11_barcode_ok;DB_CLOSE_DELAY=-1")) {
            createTables(connection);
            connection.createStatement().executeUpdate("""
                INSERT INTO products (id, barcode)
                VALUES (1, ' 8938505974191 '), (2, ''), (3, '   '), (4, NULL)
                """);

            new V11__unique_product_barcodes().migrate(context(connection));

            assertThat(queryString(connection, "SELECT barcode FROM products WHERE id = 1")).isEqualTo("8938505974191");
            assertThat(queryString(connection, "SELECT barcode FROM products WHERE id = 2")).isNull();
            assertThat(queryString(connection, "SELECT barcode FROM products WHERE id = 3")).isNull();
            assertThat(queryString(connection, "SELECT setting_value FROM system_settings WHERE setting_key = 'schema_version'"))
                .isEqualTo("11");
            assertThat(uniqueIndexExists(connection)).isTrue();
        }
    }

    @Test
    void migrateRejectsDuplicateNonEmptyBarcodes() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:v11_barcode_duplicate;DB_CLOSE_DELAY=-1")) {
            createTables(connection);
            connection.createStatement().executeUpdate("""
                INSERT INTO products (id, barcode)
                VALUES (1, 'DUP-001'), (2, ' DUP-001 ')
                """);

            assertThatThrownBy(() -> new V11__unique_product_barcodes().migrate(context(connection)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("Duplicate product barcode 'DUP-001'")
                .hasMessageContaining("Fix duplicate barcodes before migrating schema v11");

            assertThat(queryString(connection, "SELECT setting_value FROM system_settings WHERE setting_key = 'schema_version'"))
                .isNull();
        }
    }

    private void createTables(Connection connection) throws SQLException {
        connection.createStatement().execute("""
            CREATE TABLE products (
                id BIGINT PRIMARY KEY,
                barcode VARCHAR(255)
            )
            """);
        connection.createStatement().execute("""
            CREATE TABLE system_settings (
                setting_key VARCHAR(100) NOT NULL PRIMARY KEY,
                setting_value VARCHAR(255) NOT NULL,
                description VARCHAR(500) NOT NULL DEFAULT '',
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }

    private Context context(Connection connection) {
        return new Context() {
            @Override
            public Configuration getConfiguration() {
                return null;
            }

            @Override
            public Connection getConnection() {
                return connection;
            }
        };
    }

    private String queryString(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getString(1) : null;
        }
    }

    private boolean uniqueIndexExists(Connection connection) throws SQLException {
        try (var resultSet = connection.getMetaData().getIndexInfo(null, null, "PRODUCTS", true, false)) {
            while (resultSet.next()) {
                String indexName = resultSet.getString("INDEX_NAME");
                if ("UK_PRODUCTS_BARCODE".equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
