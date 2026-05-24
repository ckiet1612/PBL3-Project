package com.pbl3.project.pbl3_project.provisioning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantProvisioningServiceTest {

    private JdbcTemplate jdbcTemplate;
    private TenantProvisioningService service;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:tenant_registry_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        service = new TenantProvisioningService(
            jdbcTemplate,
            "jdbc:h2:mem:tenant_admin;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "sa",
            "",
            "jdbc:h2:mem:%s;MODE=MySQL;DB_CLOSE_DELAY=-1"
        );
    }

    @Test
    void previewBusinessDoesNotExposeTenantJdbcUrl() {
        seedBusiness("BIZ-TEST01", "Demo Store", "12345678");

        TenantProvisioningService.TenantPreviewResponse preview = service.previewBusiness("biz-test01");

        assertEquals("BIZ-TEST01", preview.businessCode());
        assertEquals("Demo Store", preview.businessName());
        assertEquals("ACTIVE", preview.status());
    }

    @Test
    void connectBusinessReturnsTenantConfigOnlyWhenJoinPinMatches() {
        seedBusiness("BIZ-TEST02", "Secure Store", "87654321");

        ResponseStatusException rejected = assertThrows(
            ResponseStatusException.class,
            () -> service.connectBusiness("BIZ-TEST02", new TenantProvisioningService.JoinBusinessRequest("wrong-pin"))
        );
        assertEquals(401, rejected.getStatusCode().value());

        TenantProvisioningService.TenantConnectionResponse connected = service.connectBusiness(
            "BIZ-TEST02",
            new TenantProvisioningService.JoinBusinessRequest("87654321")
        );
        assertEquals("BIZ-TEST02", connected.businessCode());
        assertEquals("Secure Store", connected.businessName());
        assertEquals("jdbc:h2:mem:pbl3_tenant_biz_test02;MODE=MySQL;DB_CLOSE_DELAY=-1", connected.tenantJdbcUrl());
        assertEquals("sa", connected.tenantDbUsername());
        assertEquals("", connected.tenantDbPassword());
    }

    @Test
    void connectBusinessRejectsExistingTenantWithoutJoinPin() {
        initializeRegistry();
        jdbcTemplate.update("""
            INSERT INTO tenant_registry (
                business_code,
                business_name,
                database_name,
                tenant_jdbc_url,
                admin_username,
                status
            )
            VALUES (?, ?, ?, ?, ?, 'ACTIVE')
            """,
            "BIZ-NOPIN1",
            "Legacy Store",
            "pbl3_tenant_biz_nopin1",
            "jdbc:h2:mem:pbl3_tenant_biz_nopin1;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "admin"
        );

        ResponseStatusException rejected = assertThrows(
            ResponseStatusException.class,
            () -> service.connectBusiness("BIZ-NOPIN1", new TenantProvisioningService.JoinBusinessRequest("12345678"))
        );

        assertEquals(409, rejected.getStatusCode().value());
        assertEquals("Join PIN is not configured for this business", rejected.getReason());
    }

    @Test
    void resetJoinPinAllowsLegacyTenantToConnect() {
        initializeRegistry();
        jdbcTemplate.update("""
            INSERT INTO tenant_registry (
                business_code,
                business_name,
                database_name,
                tenant_jdbc_url,
                admin_username,
                status
            )
            VALUES (?, ?, ?, ?, ?, 'ACTIVE')
            """,
            "BIZ-LEGACY",
            "Legacy Store",
            "pbl3_tenant_biz_legacy",
            "jdbc:h2:mem:pbl3_tenant_biz_legacy;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "admin"
        );

        TenantProvisioningService.TenantJoinPinResetResponse reset = service.resetJoinPin("BIZ-LEGACY");

        TenantProvisioningService.TenantConnectionResponse connected = service.connectBusiness(
            "BIZ-LEGACY",
            new TenantProvisioningService.JoinBusinessRequest(reset.joinPin())
        );
        assertEquals("BIZ-LEGACY", reset.businessCode());
        assertEquals("Legacy Store", reset.businessName());
        assertEquals("BIZ-LEGACY", connected.businessCode());
    }

    @Test
    void missingBusinessCodeReturnsNotFound() {
        ResponseStatusException rejected = assertThrows(
            ResponseStatusException.class,
            () -> service.previewBusiness("BIZ-MISSING")
        );

        assertEquals(404, rejected.getStatusCode().value());
    }

    @Test
    void tidbRuntimeUsernameKeepsProviderPrefixAndFitsLimit() throws Exception {
        TenantProvisioningService tidbService = new TenantProvisioningService(
            jdbcTemplate,
            "jdbc:mysql://gateway.example.com:4000/tenant_registry",
            "mwHapiphHnJgg2x.root",
            "secret",
            "jdbc:mysql://gateway.example.com:4000/%s"
        );
        Method method = TenantProvisioningService.class.getDeclaredMethod("generateTenantRuntimeUsername", String.class);
        method.setAccessible(true);

        String username = (String) method.invoke(tidbService, "pbl3_tenant_biz_68p3lr");

        assertTrue(username.startsWith("mwHapiphHnJgg2x."));
        assertTrue(username.length() <= 32);
    }

    private void seedBusiness(String businessCode, String businessName, String joinPin) {
        initializeRegistry();
        String databaseName = "pbl3_tenant_" + businessCode.toLowerCase().replace("-", "_");
        jdbcTemplate.update("""
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
            businessName,
            databaseName,
            "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
            "admin",
            passwordEncoder.encode(joinPin)
        );
    }

    private void initializeRegistry() {
        try {
            service.previewBusiness("BIZ-MISSING");
        } catch (ResponseStatusException ignored) {
        }
    }
}
