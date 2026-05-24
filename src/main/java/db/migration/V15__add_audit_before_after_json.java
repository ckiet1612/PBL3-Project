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

public class V15__add_audit_before_after_json extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        if (tableExists(connection, "operational_audit_logs")) {
            addColumnIfMissing(connection, "operational_audit_logs", "before_json",
                "ALTER TABLE operational_audit_logs ADD COLUMN before_json TEXT NULL");
            addColumnIfMissing(connection, "operational_audit_logs", "after_json",
                "ALTER TABLE operational_audit_logs ADD COLUMN after_json TEXT NULL");
        }
        if (tableExists(connection, "account_audit_logs")) {
            addColumnIfMissing(connection, "account_audit_logs", "before_json",
                "ALTER TABLE account_audit_logs ADD COLUMN before_json TEXT NULL");
            addColumnIfMissing(connection, "account_audit_logs", "after_json",
                "ALTER TABLE account_audit_logs ADD COLUMN after_json TEXT NULL");
        }
        upsertSchemaVersion(connection);
    }

    private void addColumnIfMissing(Connection connection, String tableName, String columnName, String sql) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, sql);
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
            VALUES ('schema_version', '15', 'Application-managed database schema version marker.')
            """)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO system_settings (setting_key, setting_value, description)
                VALUES ('schema_version', '15', 'Application-managed database schema version marker.')
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
