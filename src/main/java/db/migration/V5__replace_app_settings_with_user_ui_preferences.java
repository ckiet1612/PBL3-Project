package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class V5__replace_app_settings_with_user_ui_preferences extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "user_ui_preferences")) {
            execute(connection, """
                CREATE TABLE user_ui_preferences (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    accent_preset VARCHAR(20) NOT NULL DEFAULT 'BLUE',
                    density_mode VARCHAR(20) NOT NULL DEFAULT 'COMFORTABLE',
                    reduced_motion BOOLEAN NOT NULL DEFAULT FALSE,
                    sidebar_collapsed_by_default BOOLEAN NOT NULL DEFAULT FALSE,
                    dashboard_hidden_sections TEXT NOT NULL,
                    dashboard_section_order TEXT NOT NULL,
                    CONSTRAINT uk_user_ui_preferences_user UNIQUE (user_id),
                    CONSTRAINT fk_user_ui_preferences_user
                        FOREIGN KEY (user_id) REFERENCES users(id)
                        ON DELETE CASCADE
                )
                """);
        }
        if (tableExists(connection, "app_settings")) {
            execute(connection, "DROP TABLE IF EXISTS app_settings");
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

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
