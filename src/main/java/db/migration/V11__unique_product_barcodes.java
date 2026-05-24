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

public class V11__unique_product_barcodes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        normalizeProductBarcodes(connection);
        rejectDuplicateProductBarcodes(connection);
        createIndexIfMissing(
            connection,
            "products",
            "uk_products_barcode",
            "CREATE UNIQUE INDEX uk_products_barcode ON products(barcode)"
        );
        upsertSchemaVersion(connection);
    }

    private void normalizeProductBarcodes(Connection connection) throws SQLException {
        execute(connection, "UPDATE products SET barcode = NULLIF(TRIM(barcode), '') WHERE barcode IS NOT NULL");
    }

    private void rejectDuplicateProductBarcodes(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT barcode, COUNT(*) AS barcode_count
            FROM products
            WHERE barcode IS NOT NULL
            GROUP BY barcode
            HAVING COUNT(*) > 1
            """);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                String barcode = rs.getString("barcode");
                long count = rs.getLong("barcode_count");
                throw new SQLException("Duplicate product barcode '" + barcode + "' exists on " + count
                    + " products. Fix duplicate barcodes before migrating schema v11.");
            }
        }
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
            VALUES ('schema_version', '11', 'Application-managed database schema version marker.')
            """)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO system_settings (setting_key, setting_value, description)
                VALUES ('schema_version', '11', 'Application-managed database schema version marker.')
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
