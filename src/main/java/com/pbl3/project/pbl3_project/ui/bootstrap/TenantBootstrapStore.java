package com.pbl3.project.pbl3_project.ui.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public final class TenantBootstrapStore {

    private static final String BUSINESS_CODE = "businessCode";
    private static final String BUSINESS_NAME = "businessName";
    private static final String DATASOURCE_URL = "datasourceUrl";
    private static final String DATASOURCE_USERNAME = "datasourceUsername";
    private static final String DATASOURCE_PASSWORD = "datasourcePassword";

    private final Path configPath;

    public TenantBootstrapStore() {
        this(defaultConfigPath());
    }

    TenantBootstrapStore(Path configPath) {
        this.configPath = configPath;
    }

    public Optional<TenantConfig> load() {
        if (!Files.exists(configPath)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
        } catch (IOException ex) {
            return Optional.empty();
        }

        String businessCode = properties.getProperty(BUSINESS_CODE, "").trim();
        String businessName = properties.getProperty(BUSINESS_NAME, "").trim();
        String datasourceUrl = properties.getProperty(DATASOURCE_URL, "").trim();
        String datasourceUsername = properties.getProperty(DATASOURCE_USERNAME, "").trim();
        String datasourcePassword = properties.getProperty(DATASOURCE_PASSWORD, "");
        if (businessCode.isBlank() || datasourceUrl.isBlank() || datasourceUsername.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new TenantConfig(
            businessCode,
            businessName,
            datasourceUrl,
            datasourceUsername,
            datasourcePassword
        ));
    }

    public void save(TenantConfig config) throws IOException {
        Files.createDirectories(configPath.getParent());
        Properties properties = new Properties();
        properties.setProperty(BUSINESS_CODE, config.businessCode());
        properties.setProperty(BUSINESS_NAME, config.businessName() == null ? "" : config.businessName());
        properties.setProperty(DATASOURCE_URL, config.datasourceUrl());
        properties.setProperty(DATASOURCE_USERNAME, config.datasourceUsername() == null ? "" : config.datasourceUsername());
        properties.setProperty(DATASOURCE_PASSWORD, config.datasourcePassword() == null ? "" : config.datasourcePassword());
        try (OutputStream output = Files.newOutputStream(configPath)) {
            properties.store(output, "Sales Management tenant configuration");
        }
    }

    public void clear() throws IOException {
        Files.deleteIfExists(configPath);
    }

    public Path configPath() {
        return configPath;
    }

    private static Path defaultConfigPath() {
        return Path.of(
            System.getProperty("user.home"),
            ".sales-mgr",
            "tenant.properties"
        );
    }

    public record TenantConfig(
        String businessCode,
        String businessName,
        String datasourceUrl,
        String datasourceUsername,
        String datasourcePassword,
        String joinPin
    ) {
        public TenantConfig(
            String businessCode,
            String businessName,
            String datasourceUrl,
            String datasourceUsername,
            String datasourcePassword
        ) {
            this(businessCode, businessName, datasourceUrl, datasourceUsername, datasourcePassword, null);
        }

        public TenantConfig(String businessCode, String businessName, String datasourceUrl) {
            this(businessCode, businessName, datasourceUrl, "", "", null);
        }
    }
}
