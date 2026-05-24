package com.pbl3.project.pbl3_project.provisioning;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

@Service
@Profile("provisioning")
public class TenantProvisioningService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int JOIN_PIN_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate registryJdbcTemplate;
    private final String adminJdbcUrl;
    private final String adminUsername;
    private final String adminPassword;
    private final String tenantJdbcUrlTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public TenantProvisioningService(
        JdbcTemplate registryJdbcTemplate,
        @Value("${provisioning.admin-jdbc-url}") String adminJdbcUrl,
        @Value("${provisioning.admin-username}") String adminUsername,
        @Value("${provisioning.admin-password}") String adminPassword,
        @Value("${provisioning.tenant-jdbc-url-template:}") String tenantJdbcUrlTemplate
    ) {
        this.registryJdbcTemplate = registryJdbcTemplate;
        this.adminJdbcUrl = adminJdbcUrl;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.tenantJdbcUrlTemplate = tenantJdbcUrlTemplate == null ? "" : tenantJdbcUrlTemplate.trim();
    }

    public TenantProvisioningResponse createBusiness(CreateBusinessRequest request) {
        validateCreateRequest(request);
        ensureRegistryTable();

        String businessCode = generateUniqueBusinessCode();
        String joinPin = generateJoinPin();
        String joinPinHash = passwordEncoder.encode(joinPin);
        String databaseName = "pbl3_tenant_" + businessCode.toLowerCase(Locale.ROOT).replace("-", "_");
        String tenantJdbcUrl = buildTenantJdbcUrl(databaseName);

        createTenantDatabase(databaseName);
        migrateTenantDatabase(tenantJdbcUrl);
        createTenantAdmin(tenantJdbcUrl, request);
        TenantRuntimeCredential runtimeCredential = createTenantRuntimeCredential(databaseName);

        registryJdbcTemplate.update("""
            INSERT INTO tenant_registry (
                business_code,
                business_name,
                database_name,
                tenant_jdbc_url,
                admin_username,
                join_pin_hash,
                join_pin_updated_at,
                status
            )
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'ACTIVE')
            """,
            businessCode,
            request.businessName().trim(),
            databaseName,
            tenantJdbcUrl,
            request.adminUsername().trim(),
            joinPinHash
        );

        return new TenantProvisioningResponse(
            businessCode,
            request.businessName().trim(),
            databaseName,
            tenantJdbcUrl,
            runtimeCredential.username(),
            runtimeCredential.password(),
            request.adminUsername().trim(),
            joinPin,
            "ACTIVE"
        );
    }

    public TenantPreviewResponse previewBusiness(String businessCode) {
        ensureRegistryTable();
        String normalizedCode = normalizeBusinessCode(businessCode);
        try {
            return registryJdbcTemplate.queryForObject("""
                SELECT business_code, business_name, status
                FROM tenant_registry
                WHERE business_code = ?
                """,
                (rs, rowNum) -> new TenantPreviewResponse(
                    rs.getString("business_code"),
                    rs.getString("business_name"),
                    rs.getString("status")
                ),
                normalizedCode
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business code not found");
        }
    }

    public TenantConnectionResponse connectBusiness(String businessCode, JoinBusinessRequest request) {
        ensureRegistryTable();
        String normalizedCode = normalizeBusinessCode(businessCode);
        String joinPin = request == null ? "" : request.joinPin();
        if (isBlank(joinPin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Join PIN is required");
        }
        try {
            TenantRegistryRecord record = registryJdbcTemplate.queryForObject("""
                SELECT business_code, business_name, database_name, tenant_jdbc_url, status, join_pin_hash
                FROM tenant_registry
                WHERE business_code = ?
                """,
                (rs, rowNum) -> new TenantRegistryRecord(
                    rs.getString("business_code"),
                    rs.getString("business_name"),
                    rs.getString("database_name"),
                    rs.getString("tenant_jdbc_url"),
                    rs.getString("status"),
                    rs.getString("join_pin_hash")
                ),
                normalizedCode
            );
            if (record == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business code not found");
            }
            if (isBlank(record.joinPinHash())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Join PIN is not configured for this business");
            }
            if (!passwordEncoder.matches(joinPin.trim(), record.joinPinHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Join PIN");
            }
            TenantRuntimeCredential runtimeCredential = createTenantRuntimeCredential(record.databaseName());
            return new TenantConnectionResponse(
                record.businessCode(),
                record.businessName(),
                record.databaseName(),
                record.tenantJdbcUrl(),
                runtimeCredential.username(),
                runtimeCredential.password(),
                record.status()
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business code not found");
        }
    }

    public TenantJoinPinResetResponse resetJoinPin(String businessCode) {
        ensureRegistryTable();
        String normalizedCode = normalizeBusinessCode(businessCode);
        String joinPin = generateJoinPin();
        String joinPinHash = passwordEncoder.encode(joinPin);
        int updated = registryJdbcTemplate.update("""
            UPDATE tenant_registry
            SET join_pin_hash = ?, join_pin_updated_at = CURRENT_TIMESTAMP
            WHERE business_code = ?
            """,
            joinPinHash,
            normalizedCode
        );
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business code not found");
        }
        TenantPreviewResponse preview = previewBusiness(normalizedCode);
        return new TenantJoinPinResetResponse(
            preview.businessCode(),
            preview.businessName(),
            joinPin,
            preview.status()
        );
    }

    private void validateCreateRequest(CreateBusinessRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business information is required");
        }
        if (isBlank(request.businessName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business name is required");
        }
        if (isBlank(request.adminUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin username is required");
        }
        if (isBlank(request.adminPassword()) || request.adminPassword().length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin password must contain at least 4 characters");
        }
    }

    private void ensureRegistryTable() {
        registryJdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS tenant_registry (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                business_code VARCHAR(32) NOT NULL UNIQUE,
                business_name VARCHAR(255) NOT NULL,
                database_name VARCHAR(80) NOT NULL UNIQUE,
                tenant_jdbc_url VARCHAR(1000) NOT NULL,
                admin_username VARCHAR(255) NOT NULL,
                join_pin_hash VARCHAR(255) NULL,
                join_pin_updated_at TIMESTAMP NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);
        addColumnIfMissing("join_pin_hash", "ALTER TABLE tenant_registry ADD COLUMN join_pin_hash VARCHAR(255) NULL");
        addColumnIfMissing("join_pin_updated_at", "ALTER TABLE tenant_registry ADD COLUMN join_pin_updated_at TIMESTAMP NULL");
    }

    private String generateUniqueBusinessCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = "BIZ-" + randomToken(6);
            Integer count = registryJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenant_registry WHERE business_code = ?",
                Integer.class,
                code
            );
            if (count == null || count == 0) {
                return code;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate a unique business code");
    }

    private String randomToken(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private String generateJoinPin() {
        return randomToken(JOIN_PIN_LENGTH);
    }

    private String generateRuntimePassword() {
        return randomToken(32);
    }

    private String generateTenantRuntimeUsername(String databaseName) {
        String compactName = databaseName.toLowerCase(Locale.ROOT)
            .replace("pbl3_tenant_", "")
            .replaceAll("[^a-z0-9]", "");
        if (compactName.isBlank()) {
            compactName = "tenant";
        }
        String providerPrefix = providerUsernamePrefix();
        int maxLocalLength = 32 - (providerPrefix.isBlank() ? 0 : providerPrefix.length() + 1);
        if (maxLocalLength < 8) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "TiDB username prefix is too long to create a tenant runtime credential"
            );
        }

        String suffix = randomToken(5).toLowerCase(Locale.ROOT);
        int compactLength = Math.max(1, maxLocalLength - 2 - suffix.length());
        if (compactName.length() > compactLength) {
            compactName = compactName.substring(0, compactLength);
        }
        String localUsername = "p3" + compactName + suffix;
        return providerPrefix.isBlank() ? localUsername : providerPrefix + "." + localUsername;
    }

    private String providerUsernamePrefix() {
        if (adminUsername == null) {
            return "";
        }
        int separator = adminUsername.indexOf('.');
        if (separator <= 0) {
            return "";
        }
        String prefix = adminUsername.substring(0, separator).trim();
        return prefix.matches("[A-Za-z0-9_]+") ? prefix : "";
    }

    private boolean isH2JdbcUrl(String jdbcUrl) {
        return jdbcUrl != null && jdbcUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:h2:");
    }

    private String quotedSqlString(String value) {
        return "'" + (value == null ? "" : value.replace("\\", "\\\\").replace("'", "''")) + "'";
    }

    private void addColumnIfMissing(String columnName, String ddl) {
        if (columnExists(columnName)) {
            return;
        }
        registryJdbcTemplate.execute(ddl);
    }

    private boolean columnExists(String columnName) {
        try (Connection connection = Objects.requireNonNull(registryJdbcTemplate.getDataSource()).getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            return metadataColumnExists(metadata, "tenant_registry", columnName)
                || metadataColumnExists(metadata, "TENANT_REGISTRY", columnName)
                || metadataColumnExists(metadata, "tenant_registry", columnName.toUpperCase(Locale.ROOT))
                || metadataColumnExists(metadata, "TENANT_REGISTRY", columnName.toUpperCase(Locale.ROOT));
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not inspect tenant registry schema: " + ex.getMessage(), ex);
        }
    }

    private boolean metadataColumnExists(DatabaseMetaData metadata, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metadata.getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }

    private void createTenantDatabase(String databaseName) {
        validateDatabaseName(databaseName);
        try (Connection connection = DriverManager.getConnection(adminJdbcUrl, adminUsername, adminPassword);
             PreparedStatement statement = connection.prepareStatement("CREATE DATABASE IF NOT EXISTS `" + databaseName + "`")) {
            statement.execute();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create tenant database: " + ex.getMessage(), ex);
        }
    }

    private void migrateTenantDatabase(String tenantJdbcUrl) {
        try {
            Flyway.configure()
                .dataSource(tenantJdbcUrl, adminUsername, adminPassword)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .ignoreMigrationPatterns("*:missing")
                .load()
                .migrate();
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not migrate tenant database: " + ex.getMessage(), ex);
        }
    }

    private void createTenantAdmin(String tenantJdbcUrl, CreateBusinessRequest request) {
        String fullName = isBlank(request.adminFullName()) ? "Business Administrator" : request.adminFullName().trim();
        String passwordHash = passwordEncoder.encode(request.adminPassword());
        try (Connection connection = DriverManager.getConnection(tenantJdbcUrl, adminUsername, adminPassword);
             PreparedStatement statement = connection.prepareStatement("""
                 INSERT INTO users (username, password, full_name, role, enabled)
                 VALUES (?, ?, ?, 'ADMIN', TRUE)
                 """)) {
            statement.setString(1, request.adminUsername().trim());
            statement.setString(2, passwordHash);
            statement.setString(3, fullName);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create tenant admin account: " + ex.getMessage(), ex);
        }
    }

    private TenantRuntimeCredential createTenantRuntimeCredential(String databaseName) {
        validateDatabaseName(databaseName);
        if (isH2JdbcUrl(adminJdbcUrl)) {
            return new TenantRuntimeCredential(adminUsername, adminPassword);
        }

        String username = generateTenantRuntimeUsername(databaseName);
        String password = generateRuntimePassword();
        String account = quotedSqlString(username) + "@'%'";
        try (Connection connection = DriverManager.getConnection(adminJdbcUrl, adminUsername, adminPassword)) {
            try (PreparedStatement createUser = connection.prepareStatement(
                "CREATE USER IF NOT EXISTS " + account + " IDENTIFIED BY " + quotedSqlString(password)
            )) {
                createUser.execute();
            }
            try (PreparedStatement grantAccess = connection.prepareStatement(
                "GRANT SELECT, INSERT, UPDATE, DELETE ON `" + databaseName + "`.* TO " + account
            )) {
                grantAccess.execute();
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Could not create tenant runtime credential: " + ex.getMessage(),
                ex
            );
        }
        return new TenantRuntimeCredential(username, password);
    }

    private String buildTenantJdbcUrl(String databaseName) {
        validateDatabaseName(databaseName);
        String template = tenantJdbcUrlTemplate.isBlank() ? deriveTenantJdbcTemplate(adminJdbcUrl) : tenantJdbcUrlTemplate;
        return template.formatted(databaseName);
    }

    private String deriveTenantJdbcTemplate(String jdbcUrl) {
        int queryStart = jdbcUrl.indexOf('?');
        String base = queryStart >= 0 ? jdbcUrl.substring(0, queryStart) : jdbcUrl;
        String query = queryStart >= 0 ? jdbcUrl.substring(queryStart) : "";

        int protocolEnd = base.indexOf("//");
        int databaseSlash = protocolEnd >= 0 ? base.indexOf('/', protocolEnd + 2) : base.lastIndexOf('/');
        if (databaseSlash < 0) {
            return base + "/%s" + query;
        }
        return base.substring(0, databaseSlash + 1) + "%s" + query;
    }

    private void validateDatabaseName(String databaseName) {
        if (databaseName == null || !databaseName.matches("[A-Za-z0-9_]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenant database name");
        }
    }

    private String normalizeBusinessCode(String code) {
        if (isBlank(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business code is required");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record CreateBusinessRequest(
        String businessName,
        String adminUsername,
        String adminPassword,
        String adminFullName
    ) {
    }

    public record JoinBusinessRequest(String joinPin) {
    }

    public record TenantProvisioningResponse(
        String businessCode,
        String businessName,
        String databaseName,
        String tenantJdbcUrl,
        String tenantDbUsername,
        String tenantDbPassword,
        String adminUsername,
        String joinPin,
        String status
    ) {
    }

    public record TenantPreviewResponse(
        String businessCode,
        String businessName,
        String status
    ) {
    }

    public record TenantJoinPinResetResponse(
        String businessCode,
        String businessName,
        String joinPin,
        String status
    ) {
    }

    public record TenantConnectionResponse(
        String businessCode,
        String businessName,
        String databaseName,
        String tenantJdbcUrl,
        String tenantDbUsername,
        String tenantDbPassword,
        String status
    ) {
    }

    private record TenantRegistryRecord(
        String businessCode,
        String businessName,
        String databaseName,
        String tenantJdbcUrl,
        String status,
        String joinPinHash
    ) {
    }

    private record TenantRuntimeCredential(String username, String password) {
    }
}
