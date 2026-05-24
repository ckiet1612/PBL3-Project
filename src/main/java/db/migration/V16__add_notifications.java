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

public class V16__add_notifications extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        if (!tableExists(connection, "notifications")) {
            execute(connection, """
                CREATE TABLE notifications (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    category VARCHAR(20) NOT NULL,
                    notification_type VARCHAR(40) NOT NULL,
                    severity VARCHAR(20) NOT NULL,
                    title VARCHAR(180) NOT NULL,
                    message TEXT NOT NULL,
                    source_type VARCHAR(80) NULL,
                    source_id BIGINT NULL,
                    event_key VARCHAR(180) NULL,
                    action_target VARCHAR(40) NULL,
                    action_payload_json TEXT NULL,
                    created_by_user_id BIGINT NULL,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL,
                    resolved_at DATETIME NULL,
                    expires_at DATETIME NULL,
                    CONSTRAINT fk_notifications_created_by
                        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
                )
                """);
        }
        if (!tableExists(connection, "notification_user_states")) {
            execute(connection, """
                CREATE TABLE notification_user_states (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    notification_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    created_at DATETIME NOT NULL,
                    read_at DATETIME NULL,
                    dismissed_at DATETIME NULL,
                    completed_at DATETIME NULL,
                    CONSTRAINT fk_notification_user_states_notification
                        FOREIGN KEY (notification_id) REFERENCES notifications(id),
                    CONSTRAINT fk_notification_user_states_user
                        FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """);
        }

        createIndexIfMissing(connection, "notifications", "uk_notifications_event_key",
            "CREATE UNIQUE INDEX uk_notifications_event_key ON notifications(event_key)");
        createIndexIfMissing(connection, "notifications", "idx_notifications_category_created",
            "CREATE INDEX idx_notifications_category_created ON notifications(category, created_at)");
        createIndexIfMissing(connection, "notifications", "idx_notifications_type_resolved",
            "CREATE INDEX idx_notifications_type_resolved ON notifications(notification_type, resolved_at)");
        createIndexIfMissing(connection, "notifications", "idx_notifications_severity_created",
            "CREATE INDEX idx_notifications_severity_created ON notifications(severity, created_at)");
        createIndexIfMissing(connection, "notifications", "idx_notifications_source",
            "CREATE INDEX idx_notifications_source ON notifications(source_type, source_id)");
        createIndexIfMissing(connection, "notification_user_states", "uk_notification_user_states_user_notification",
            "CREATE UNIQUE INDEX uk_notification_user_states_user_notification ON notification_user_states(notification_id, user_id)");
        createIndexIfMissing(connection, "notification_user_states", "idx_notification_user_states_user_unread",
            "CREATE INDEX idx_notification_user_states_user_unread ON notification_user_states(user_id, read_at)");
        createIndexIfMissing(connection, "notification_user_states", "idx_notification_user_states_user_dismissed",
            "CREATE INDEX idx_notification_user_states_user_dismissed ON notification_user_states(user_id, dismissed_at)");
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
            VALUES ('schema_version', '16', 'Application-managed database schema version marker.')
            """)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO system_settings (setting_key, setting_value, description)
                VALUES ('schema_version', '16', 'Application-managed database schema version marker.')
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
