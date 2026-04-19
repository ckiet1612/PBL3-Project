package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class V6__add_expenses extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (tableExists(connection, "expenses")) {
            return;
        }
        execute(connection, """
            CREATE TABLE expenses (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                spent_on DATE NOT NULL,
                category VARCHAR(30) NOT NULL,
                title VARCHAR(255) NOT NULL,
                amount DECIMAL(19, 2) NOT NULL,
                payment_method VARCHAR(20) NOT NULL,
                note TEXT NULL,
                created_by_user_id BIGINT NOT NULL,
                created_at DATETIME NOT NULL,
                updated_at DATETIME NOT NULL,
                CONSTRAINT fk_expenses_created_by_user
                    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
            )
            """);
        execute(connection, "CREATE INDEX idx_expenses_spent_on ON expenses(spent_on)");
        execute(connection, "CREATE INDEX idx_expenses_category ON expenses(category)");
        execute(connection, "CREATE INDEX idx_expenses_created_by_user ON expenses(created_by_user_id)");
        execute(connection, "CREATE INDEX idx_expenses_spent_on_category ON expenses(spent_on, category)");
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
