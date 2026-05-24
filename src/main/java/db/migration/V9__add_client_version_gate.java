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

public class V9__add_client_version_gate extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
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

        upsertSetting(
            connection,
            "minimum_client_version",
            "0.0.1-SNAPSHOT",
            "Minimum desktop client version allowed to sign in."
        );
        upsertSetting(
            connection,
            "schema_version",
            "9",
            "Application-managed database schema version marker."
        );
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

    private void upsertSetting(Connection connection, String key, String value, String description) throws SQLException {
        if (settingExists(connection, key)) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO system_settings (setting_key, setting_value, description)
            VALUES (?, ?, ?)
            """)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.setString(3, description);
            statement.executeUpdate();
        }
    }

    private boolean settingExists(Connection connection, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT 1 FROM system_settings WHERE setting_key = ?
            """)) {
            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
