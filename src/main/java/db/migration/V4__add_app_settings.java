package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class V4__add_app_settings extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (tableExists(connection, "app_settings")) {
            return;
        }
        execute(connection, """
            CREATE TABLE app_settings (
                id BIGINT NOT NULL PRIMARY KEY,
                app_display_name VARCHAR(255) NOT NULL,
                business_name VARCHAR(255) NOT NULL,
                business_phone VARCHAR(255) NOT NULL DEFAULT '',
                business_address VARCHAR(500) NOT NULL DEFAULT '',
                default_print_receipt BOOLEAN NOT NULL DEFAULT TRUE,
                receipt_footer_note TEXT NOT NULL
            )
            """);
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
