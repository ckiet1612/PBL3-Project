package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class V2__normalize_enum_columns_to_varchar extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        normalizeEnumColumn(connection, "orders", "payment_method", "VARCHAR(20) NOT NULL");
        normalizeEnumColumn(connection, "orders", "status", "VARCHAR(30) NOT NULL");
        normalizeEnumColumn(connection, "import_orders", "status", "VARCHAR(20) NOT NULL");
        normalizeEnumColumn(connection, "stocktake_sessions", "scope_type", "VARCHAR(30) NOT NULL");
        normalizeEnumColumn(connection, "stocktake_sessions", "status", "VARCHAR(30) NOT NULL");
    }

    private void normalizeEnumColumn(Connection connection, String tableName, String columnName, String definition) throws SQLException {
        if (!tableExists(connection, tableName) || !columnExists(connection, tableName, columnName)) {
            return;
        }
        String alterSql;
        if (isH2(connection)) {
            alterSql = "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " " + definition;
        } else {
            alterSql = "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + definition;
        }
        execute(connection, alterSql);
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

    private boolean isH2(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("h2");
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
