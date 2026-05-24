package com.pbl3.project.pbl3_project.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl3.project.pbl3_project.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DataBackupService {
    public static final String BACKUP_EXTENSION = ".pbl3backup";

    private static final int FORMAT_VERSION = 1;
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final TypeReference<LinkedHashMap<String, Object>> ROW_TYPE = new TypeReference<>() {
    };

    private static final List<String> TABLE_ORDER = List.of(
        "categories",
        "brands",
        "origins",
        "units",
        "suppliers",
        "users",
        "customers",
        "user_ui_preferences",
        "sales_shifts",
        "products",
        "promotions",
        "qr_payments",
        "orders",
        "order_items",
        "sales_shift_refunds",
        "import_orders",
        "import_order_items",
        "expenses",
        "inventory_position_baselines",
        "inventory_transactions",
        "operational_audit_logs",
        "account_audit_logs",
        "stocktake_sessions",
        "stocktake_items",
        "notifications",
        "notification_user_states",
        "system_settings"
    );

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final AuthorizationService authorizationService;
    private final String clientVersion;
    private final String businessName;
    private final String defaultBackupDirectory;

    @PersistenceContext
    private EntityManager entityManager;

    public DataBackupService(
        JdbcTemplate jdbcTemplate,
        TransactionTemplate transactionTemplate,
        ObjectMapper objectMapper,
        AuthorizationService authorizationService,
        @Value("${app.client.version:0.0.1-SNAPSHOT}") String clientVersion,
        @Value("${app.business.name:PBL3 STORE}") String businessName,
        @Value("${app.backup.default-directory:}") String defaultBackupDirectory
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper.copy()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        this.authorizationService = authorizationService;
        this.clientVersion = normalizeText(clientVersion, "0.0.1-SNAPSHOT");
        this.businessName = normalizeText(businessName, "PBL3 STORE");
        this.defaultBackupDirectory = defaultBackupDirectory == null ? "" : defaultBackupDirectory.trim();
    }

    public File getDefaultBackupDirectory() {
        if (!defaultBackupDirectory.isBlank()) {
            return Path.of(defaultBackupDirectory).toFile();
        }
        return Path.of(System.getProperty("user.home"), "Desktop", "PBL3_Backups").toFile();
    }

    public String buildDefaultBackupFileName() {
        return "pbl3-backup-"
            + sanitizeFileName(businessName)
            + "-"
            + LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT)
            + BACKUP_EXTENSION;
    }

    public BackupPreview previewBackup(File backupFile) {
        Path path = normalizeBackupPath(backupFile, false);
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            BackupManifest manifest = readManifest(zipFile);
            return new BackupPreview(path.toFile(), manifest, Files.size(path));
        } catch (IOException ex) {
            throw new BackupException("Could not read backup file", ex);
        }
    }

    public BackupExportResult exportBackup(User actor, File targetFile) {
        return exportBackup(actor, targetFile, null);
    }

    public BackupExportResult exportBackup(User actor, File targetFile, Consumer<String> statusConsumer) {
        authorizationService.requireDataBackupAccess(actor);
        Path targetPath = normalizeBackupPath(targetFile, true);
        Path tempPath = null;
        try {
            Files.createDirectories(targetPath.getParent());
            tempPath = Files.createTempFile(targetPath.getParent(), targetPath.getFileName().toString(), ".tmp");
            List<TableSpec> tables = discoverTables();
            BackupManifest manifest = buildManifest(tables);

            publish(statusConsumer, "Preparing backup...");
            try (OutputStream outputStream = Files.newOutputStream(tempPath);
                 ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                writeManifest(zipOutputStream, manifest);
                for (TableSpec table : tables) {
                    publish(statusConsumer, "Exporting " + table.name() + "...");
                    writeTable(zipOutputStream, table);
                }
            }

            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            publish(statusConsumer, "Backup exported.");
            return new BackupExportResult(targetPath.toFile(), manifest, totalRows(manifest));
        } catch (IOException | RuntimeException ex) {
            deleteQuietly(tempPath);
            if (ex instanceof BackupException backupException) {
                throw backupException;
            }
            throw new BackupException("Could not export backup", ex);
        }
    }

    public BackupRestoreResult restoreBackup(User actor, File backupFile) {
        return restoreBackup(actor, backupFile, null);
    }

    public BackupRestoreResult restoreBackup(User actor, File backupFile, Consumer<String> statusConsumer) {
        authorizationService.requireDataBackupAccess(actor);
        Path backupPath = normalizeBackupPath(backupFile, false);
        BackupPreview preview = previewBackup(backupPath.toFile());
        validateManifestForCurrentDatabase(preview.manifest());

        File safetyBackup = new File(
            getDefaultBackupDirectory(),
            "pre-restore-safety-" + LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT) + BACKUP_EXTENSION
        );
        publish(statusConsumer, "Creating safety backup...");
        BackupExportResult safetyResult = exportBackup(actor, safetyBackup, statusConsumer);

        try (ZipFile zipFile = new ZipFile(backupPath.toFile())) {
            BackupManifest manifest = readManifest(zipFile);
            List<TableSpec> currentTables = discoverTables();
            validateManifestTables(manifest, currentTables);

            transactionTemplate.executeWithoutResult(status -> jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
                try {
                    restoreWithConnection(connection, zipFile, manifest, currentTables, statusConsumer);
                } catch (IOException ex) {
                    throw new BackupException("Could not read backup data", ex);
                }
                return null;
            }));
            clearPersistenceContext();
            publish(statusConsumer, "Restore completed.");
            return new BackupRestoreResult(backupPath.toFile(), safetyResult.file(), manifest, totalRows(manifest));
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof BackupException backupException) {
                throw backupException;
            }
            throw new BackupException("Could not restore backup. Safety backup was kept at " + safetyResult.file().getAbsolutePath(), ex);
        }
    }

    private BackupManifest buildManifest(List<TableSpec> tables) {
        String schemaVersion = readSystemSetting("schema_version").orElse("unknown");
        String databaseProduct = jdbcTemplate.execute((ConnectionCallback<String>) connection ->
            connection.getMetaData().getDatabaseProductName()
        );
        List<BackupTableSummary> summaries = tables.stream()
            .map(table -> new BackupTableSummary(table.name(), table.rowCount()))
            .toList();
        return new BackupManifest(
            FORMAT_VERSION,
            LocalDateTime.now().toString(),
            clientVersion,
            schemaVersion,
            businessName,
            databaseProduct == null || databaseProduct.isBlank() ? "Unknown" : databaseProduct,
            summaries
        );
    }

    private Optional<String> readSystemSetting(String key) {
        return jdbcTemplate.execute((ConnectionCallback<Optional<String>>) connection -> {
            if (!tableExists(connection, "system_settings")) {
                return Optional.empty();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT setting_value FROM system_settings WHERE setting_key = ?"
            )) {
                statement.setString(1, key);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String value = resultSet.getString(1);
                        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
                    }
                }
            }
            return Optional.empty();
        });
    }

    private List<TableSpec> discoverTables() {
        return jdbcTemplate.execute((ConnectionCallback<List<TableSpec>>) connection -> {
            List<TableSpec> tables = new ArrayList<>();
            for (String tableName : TABLE_ORDER) {
                if (!tableExists(connection, tableName)) {
                    continue;
                }
                List<ColumnSpec> columns = readColumns(connection, tableName);
                if (columns.isEmpty()) {
                    continue;
                }
                tables.add(new TableSpec(tableName, columns, countRows(connection, tableName)));
            }
            return tables;
        });
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        return resolveTableRef(connection, tableName).isPresent();
    }

    private Optional<TableRef> resolveTableRef(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        String currentSchema = safeCurrentSchema(connection);
        List<TableRef> matches = new ArrayList<>();
        for (String candidate : tableCandidates(tableName)) {
            for (String schemaCandidate : schemaCandidates(currentSchema)) {
                try (ResultSet resultSet = metaData.getTables(catalog, schemaCandidate, candidate, new String[]{"TABLE"})) {
                    while (resultSet.next()) {
                        String schema = resultSet.getString("TABLE_SCHEM");
                        String physicalTableName = resultSet.getString("TABLE_NAME");
                        if (physicalTableName != null && physicalTableName.equalsIgnoreCase(tableName)) {
                            matches.add(new TableRef(catalog, schema, physicalTableName));
                        }
                    }
                }
            }
        }
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        return matches.stream()
            .filter(ref -> ref.schema() != null && currentSchema != null && ref.schema().equalsIgnoreCase(currentSchema))
            .findFirst()
            .or(() -> matches.stream().filter(ref -> !isSystemSchema(ref.schema())).findFirst())
            .or(() -> Optional.of(matches.get(0)));
    }

    private List<ColumnSpec> readColumns(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        TableRef tableRef = resolveTableRef(connection, tableName).orElseThrow(() ->
            new BackupException("Table not found: " + tableName)
        );
        List<ColumnSpec> columns = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        try (ResultSet resultSet = metaData.getColumns(tableRef.catalog(), tableRef.schema(), tableRef.tableName(), null)) {
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                if (columnName == null || !isSafeIdentifier(columnName)) {
                    continue;
                }
                String normalizedColumn = columnName.toLowerCase(Locale.ROOT);
                if (!seen.add(normalizedColumn)) {
                    continue;
                }
                int jdbcType = resultSet.getInt("DATA_TYPE");
                int position = resultSet.getInt("ORDINAL_POSITION");
                columns.add(new ColumnSpec(normalizedColumn, jdbcType, position));
            }
        }
        columns.sort(Comparator.comparingInt(ColumnSpec::position));
        return columns;
    }

    private long countRows(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + safeIdentifier(tableName))) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private void writeManifest(ZipOutputStream zipOutputStream, BackupManifest manifest) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry("manifest.json"));
        objectMapper.writeValue(nonClosing(zipOutputStream), manifest);
        zipOutputStream.closeEntry();
    }

    private BackupManifest readManifest(ZipFile zipFile) throws IOException {
        ZipEntry entry = zipFile.getEntry("manifest.json");
        if (entry == null) {
            throw new BackupException("Backup file is missing manifest.json");
        }
        try (var inputStream = zipFile.getInputStream(entry)) {
            BackupManifest manifest = objectMapper.readValue(inputStream, BackupManifest.class);
            if (manifest == null) {
                throw new BackupException("Backup manifest is empty");
            }
            return manifest;
        }
    }

    private void writeTable(ZipOutputStream zipOutputStream, TableSpec table) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(tableEntryName(table.name())));
        JsonGenerator generator = objectMapper.getFactory().createGenerator(nonClosing(zipOutputStream));
        generator.writeStartArray();
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try {
                writeTableRows(connection, table, generator);
            } catch (IOException ex) {
                throw new BackupException("Could not export table " + table.name(), ex);
            }
            return null;
        });
        generator.writeEndArray();
        generator.flush();
        zipOutputStream.closeEntry();
    }

    private void writeTableRows(Connection connection, TableSpec table, JsonGenerator generator) throws SQLException, IOException {
        String sql = "SELECT " + columnList(table.columns()) + " FROM " + safeIdentifier(table.name())
            + (hasColumn(table, "id") ? " ORDER BY id" : "");
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                generator.writeStartObject();
                for (ColumnSpec column : table.columns()) {
                    generator.writeFieldName(column.name());
                    writeJsonValue(generator, resultSet.getObject(column.name()));
                }
                generator.writeEndObject();
            }
        }
    }

    private void writeJsonValue(JsonGenerator generator, Object value) throws IOException, SQLException {
        if (value == null) {
            generator.writeNull();
        } else if (value instanceof BigDecimal bigDecimal) {
            generator.writeNumber(bigDecimal);
        } else if (value instanceof Integer integer) {
            generator.writeNumber(integer);
        } else if (value instanceof Long longValue) {
            generator.writeNumber(longValue);
        } else if (value instanceof Number number) {
            generator.writeNumber(number.toString());
        } else if (value instanceof Boolean booleanValue) {
            generator.writeBoolean(booleanValue);
        } else if (value instanceof Timestamp timestamp) {
            generator.writeString(timestamp.toString());
        } else if (value instanceof Date date) {
            generator.writeString(date.toString());
        } else if (value instanceof Time time) {
            generator.writeString(time.toString());
        } else if (value instanceof LocalDateTime localDateTime) {
            generator.writeString(Timestamp.valueOf(localDateTime).toString());
        } else if (value instanceof LocalDate localDate) {
            generator.writeString(Date.valueOf(localDate).toString());
        } else if (value instanceof LocalTime localTime) {
            generator.writeString(Time.valueOf(localTime).toString());
        } else if (value instanceof byte[] bytes) {
            generator.writeString(java.util.Base64.getEncoder().encodeToString(bytes));
        } else if (value instanceof java.sql.Clob clob) {
            generator.writeString(clob.getSubString(1, Math.toIntExact(clob.length())));
        } else {
            generator.writeString(value.toString());
        }
    }

    private void restoreWithConnection(
        Connection connection,
        ZipFile zipFile,
        BackupManifest manifest,
        List<TableSpec> currentTables,
        Consumer<String> statusConsumer
    ) throws SQLException, IOException {
        boolean h2 = isH2(connection);
        disableReferentialIntegrity(connection, h2);
        try {
            for (int i = currentTables.size() - 1; i >= 0; i--) {
                TableSpec table = currentTables.get(i);
                publish(statusConsumer, "Clearing " + table.name() + "...");
                deleteTable(connection, table.name());
            }
            Map<String, TableSpec> currentByName = new LinkedHashMap<>();
            for (TableSpec table : currentTables) {
                currentByName.put(table.name(), table);
            }
            for (BackupTableSummary summary : manifest.tables()) {
                TableSpec table = currentByName.get(summary.tableName());
                if (table == null) {
                    throw new BackupException("Current database is missing table " + summary.tableName());
                }
                publish(statusConsumer, "Restoring " + table.name() + "...");
                restoreTable(connection, zipFile, table);
            }
        } finally {
            enableReferentialIntegrity(connection, h2);
        }
        resetIdentityColumnsBestEffort(connection, currentTables, h2);
    }

    private void deleteTable(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM " + safeIdentifier(tableName));
        }
    }

    private void restoreTable(Connection connection, ZipFile zipFile, TableSpec table) throws IOException, SQLException {
        ZipEntry entry = zipFile.getEntry(tableEntryName(table.name()));
        if (entry == null) {
            throw new BackupException("Backup file is missing data for table " + table.name());
        }
        String insertSql = "INSERT INTO " + safeIdentifier(table.name())
            + " (" + columnList(table.columns()) + ") VALUES (" + placeholders(table.columns().size()) + ")";
        try (JsonParser parser = objectMapper.getFactory().createParser(zipFile.getInputStream(entry));
             PreparedStatement statement = connection.prepareStatement(insertSql)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new BackupException("Invalid table data for " + table.name());
            }
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                Map<String, Object> row = objectMapper.readValue(parser, ROW_TYPE);
                bindRow(statement, table, row);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void bindRow(PreparedStatement statement, TableSpec table, Map<String, Object> row) throws SQLException {
        for (int i = 0; i < table.columns().size(); i++) {
            ColumnSpec column = table.columns().get(i);
            Object value = row.get(column.name());
            bindValue(statement, i + 1, column.jdbcType(), value);
        }
    }

    private void bindValue(PreparedStatement statement, int index, int jdbcType, Object value) throws SQLException {
        if (value == null) {
            statement.setNull(index, jdbcType);
            return;
        }
        switch (jdbcType) {
            case Types.BIGINT -> statement.setLong(index, asNumber(value).longValue());
            case Types.INTEGER, Types.SMALLINT, Types.TINYINT -> statement.setInt(index, asNumber(value).intValue());
            case Types.DECIMAL, Types.NUMERIC -> statement.setBigDecimal(index, asBigDecimal(value));
            case Types.BOOLEAN, Types.BIT -> statement.setBoolean(index, asBoolean(value));
            case Types.DATE -> statement.setDate(index, Date.valueOf(value.toString().substring(0, 10)));
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> statement.setTime(index, Time.valueOf(value.toString().substring(0, 8)));
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> statement.setTimestamp(index, Timestamp.valueOf(normalizeTimestamp(value.toString())));
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR, Types.CLOB -> statement.setString(index, value.toString());
            default -> statement.setObject(index, value);
        }
    }

    private Number asNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        return new BigDecimal(value.toString());
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(value.toString());
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString()) || "1".equals(value.toString());
    }

    private String normalizeTimestamp(String value) {
        String normalized = value.replace('T', ' ');
        if (normalized.endsWith("Z")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int offsetIndex = Math.max(normalized.lastIndexOf('+'), normalized.lastIndexOf('-'));
        if (offsetIndex > 10) {
            normalized = normalized.substring(0, offsetIndex);
        }
        if (normalized.length() == 10) {
            normalized += " 00:00:00";
        }
        return normalized;
    }

    private void validateManifestForCurrentDatabase(BackupManifest manifest) {
        if (manifest.formatVersion() != FORMAT_VERSION) {
            throw new BackupException("Unsupported backup format version " + manifest.formatVersion());
        }
        String currentSchemaVersion = readSystemSetting("schema_version").orElse("unknown");
        if (!Objects.equals(currentSchemaVersion, manifest.schemaVersion())) {
            throw new BackupException("Backup schema version does not match current database schema");
        }
        validateManifestTables(manifest, discoverTables());
    }

    private void validateManifestTables(BackupManifest manifest, List<TableSpec> currentTables) {
        Set<String> currentTableNames = new LinkedHashSet<>();
        for (TableSpec table : currentTables) {
            currentTableNames.add(table.name());
        }
        Set<String> backupTableNames = new LinkedHashSet<>();
        for (BackupTableSummary summary : manifest.tables()) {
            if (!TABLE_ORDER.contains(summary.tableName())) {
                throw new BackupException("Backup contains unsupported table " + summary.tableName());
            }
            backupTableNames.add(summary.tableName());
        }
        if (!currentTableNames.equals(backupTableNames)) {
            throw new BackupException("Backup table set does not match the current database");
        }
    }

    private void disableReferentialIntegrity(Connection connection, boolean h2) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(h2 ? "SET REFERENTIAL_INTEGRITY FALSE" : "SET FOREIGN_KEY_CHECKS=0");
        }
    }

    private void enableReferentialIntegrity(Connection connection, boolean h2) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(h2 ? "SET REFERENTIAL_INTEGRITY TRUE" : "SET FOREIGN_KEY_CHECKS=1");
        }
    }

    private boolean isH2(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null && productName.toLowerCase(Locale.ROOT).contains("h2");
    }

    private void resetIdentityColumnsBestEffort(Connection connection, List<TableSpec> tables, boolean h2) {
        for (TableSpec table : tables) {
            if (!hasColumn(table, "id")) {
                continue;
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM " + safeIdentifier(table.name()))) {
                resultSet.next();
                long nextValue = Math.max(1L, resultSet.getLong(1));
                try (Statement alterStatement = connection.createStatement()) {
                    if (h2) {
                        alterStatement.execute("ALTER TABLE " + safeIdentifier(table.name()) + " ALTER COLUMN id RESTART WITH " + nextValue);
                    } else {
                        alterStatement.execute("ALTER TABLE " + safeIdentifier(table.name()) + " AUTO_INCREMENT = " + nextValue);
                    }
                }
            } catch (RuntimeException | SQLException ignored) {
                // Identity reset is best-effort; explicit IDs were already restored.
            }
        }
    }

    private void clearPersistenceContext() {
        try {
            if (entityManager != null) {
                entityManager.clear();
                entityManager.getEntityManagerFactory().getCache().evictAll();
            }
        } catch (RuntimeException ignored) {
            // The restored database is authoritative; stale UI state is handled by the caller.
        }
    }

    private Path normalizeBackupPath(File file, boolean allowMissing) {
        if (file == null) {
            throw new BackupException("Backup file is required");
        }
        Path path = file.toPath().toAbsolutePath().normalize();
        if (allowMissing && !path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(BACKUP_EXTENSION)) {
            path = path.resolveSibling(path.getFileName() + BACKUP_EXTENSION);
        }
        if (!allowMissing && !Files.isRegularFile(path)) {
            throw new BackupException("Backup file does not exist");
        }
        if (path.getParent() == null) {
            throw new BackupException("Backup file must have a parent folder");
        }
        return path;
    }

    private String tableEntryName(String tableName) {
        return "tables/" + tableName + ".json";
    }

    private String columnList(List<ColumnSpec> columns) {
        return columns.stream()
            .map(ColumnSpec::name)
            .map(this::safeIdentifier)
            .reduce((left, right) -> left + ", " + right)
            .orElseThrow(() -> new BackupException("No columns available"));
    }

    private String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
    }

    private boolean hasColumn(TableSpec table, String columnName) {
        return table.columns().stream().anyMatch(column -> column.name().equalsIgnoreCase(columnName));
    }

    private String safeIdentifier(String identifier) {
        if (!isSafeIdentifier(identifier)) {
            throw new BackupException("Unsafe database identifier: " + identifier);
        }
        return identifier.toLowerCase(Locale.ROOT);
    }

    private boolean isSafeIdentifier(String identifier) {
        return identifier != null && identifier.matches("[A-Za-z][A-Za-z0-9_]*");
    }

    private List<String> tableCandidates(String tableName) {
        return List.of(tableName, tableName.toUpperCase(Locale.ROOT), tableName.toLowerCase(Locale.ROOT));
    }

    private List<String> schemaCandidates(String currentSchema) {
        List<String> candidates = new ArrayList<>();
        if (currentSchema != null && !currentSchema.isBlank()) {
            candidates.add(currentSchema);
        }
        candidates.add(null);
        return candidates;
    }

    private String safeCurrentSchema(Connection connection) {
        try {
            return connection.getSchema();
        } catch (SQLException | AbstractMethodError ex) {
            return null;
        }
    }

    private boolean isSystemSchema(String schema) {
        return schema != null && schema.equalsIgnoreCase("INFORMATION_SCHEMA");
    }

    private OutputStream nonClosing(OutputStream delegate) {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                delegate.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                delegate.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                delegate.flush();
            }
        };
    }

    private String sanitizeFileName(String value) {
        String sanitized = normalizeText(value, "pbl3-store")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");
        return sanitized.isBlank() ? "pbl3-store" : sanitized;
    }

    private String normalizeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private long totalRows(BackupManifest manifest) {
        return manifest.tables().stream().mapToLong(BackupTableSummary::rowCount).sum();
    }

    private void publish(Consumer<String> statusConsumer, String message) {
        if (statusConsumer != null) {
            statusConsumer.accept(message);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    public record BackupTableSummary(String tableName, long rowCount) {
    }

    public record BackupManifest(
        int formatVersion,
        String createdAt,
        String appVersion,
        String schemaVersion,
        String businessName,
        String databaseProduct,
        List<BackupTableSummary> tables
    ) {
    }

    public record BackupExportResult(File file, BackupManifest manifest, long totalRows) {
    }

    public record BackupRestoreResult(File sourceFile, File safetyBackupFile, BackupManifest manifest, long totalRows) {
    }

    public record BackupPreview(File file, BackupManifest manifest, long sizeBytes) {
    }

    private record TableSpec(String name, List<ColumnSpec> columns, long rowCount) {
    }

    private record ColumnSpec(String name, int jdbcType, int position) {
    }

    private record TableRef(String catalog, String schema, String tableName) {
    }
}
