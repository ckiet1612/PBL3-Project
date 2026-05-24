package com.pbl3.project.pbl3_project;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import com.pbl3.project.pbl3_project.ui.bootstrap.TenantBootstrapSceneFactory;
import com.pbl3.project.pbl3_project.ui.bootstrap.TenantBootstrapStore;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SalesManagementDesktopApplication extends Application {

    private ConfigurableApplicationContext applicationContext;
    private boolean tenantBootstrapEnabled;

    @Override
    public void init() {
        validateDesktopProfiles();
        tenantBootstrapEnabled = desktopReleaseMode() || isProfileActive("tenant-client");
        if (tenantBootstrapEnabled) {
            return;
        }
        applicationContext = new SpringApplicationBuilder(SalesManagementApplication.class)
                .run();
    }

    @Override
    public void start(Stage stage) {
        if (tenantBootstrapEnabled) {
            startTenantAwareApplication(stage);
            return;
        }
        applicationContext.publishEvent(new PrimaryStageReadyEvent(stage));
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
        }
        Platform.exit();
    }

    private void startTenantAwareApplication(Stage stage) {
        TenantBootstrapStore store = new TenantBootstrapStore();
        TenantBootstrapSceneFactory factory = new TenantBootstrapSceneFactory();
        if (applicationContext != null) {
            applicationContext.publishEvent(new PrimaryStageReadyEvent(stage));
            return;
        }
        store.load().ifPresentOrElse(
            tenantConfig -> bootSpringForTenant(stage, factory, tenantConfig),
            () -> factory.show(new TenantBootstrapSceneFactory.Context(
                stage,
                store,
                provisioningApiBaseUrl(),
                propertyOrEnv("PROVISIONING_API_KEY", ""),
                tenantConfig -> bootSpringForTenant(stage, factory, tenantConfig)
            ))
        );
    }

    private void bootSpringForTenant(
        Stage stage,
        TenantBootstrapSceneFactory factory,
        TenantBootstrapStore.TenantConfig tenantConfig
    ) {
        factory.showStarting(stage);
        Thread worker = new Thread(() -> {
            try {
                applicationContext = new SpringApplicationBuilder(SalesManagementApplication.class)
                    .web(WebApplicationType.NONE)
                    .run(tenantStartupArgs(tenantConfig));
                Platform.runLater(() -> applicationContext.publishEvent(new PrimaryStageReadyEvent(stage)));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> factory.show(
                    new TenantBootstrapSceneFactory.Context(
                        stage,
                        new TenantBootstrapStore(),
                        provisioningApiBaseUrl(),
                        propertyOrEnv("PROVISIONING_API_KEY", ""),
                        nextTenantConfig -> bootSpringForTenant(stage, factory, nextTenantConfig)
                    ),
                    "Could not open the selected business workspace: " + userFriendlyStartupError(ex),
                    true
                ));
            }
        }, "tenant-spring-boot");
        worker.setDaemon(true);
        worker.start();
    }

    private String[] tenantStartupArgs(TenantBootstrapStore.TenantConfig tenantConfig) {
        List<String> args = new ArrayList<>(getParameters().getRaw());
        args.add("--spring.profiles.active=tenant-client");
        args.add("--spring.main.web-application-type=none");
        args.add("--spring.datasource.url=" + tenantConfig.datasourceUrl());
        args.add("--spring.datasource.username=" + tenantConfig.datasourceUsername());
        args.add("--spring.datasource.password=" + tenantConfig.datasourcePassword());
        args.add("--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver");
        args.add("--spring.flyway.enabled=false");
        args.add("--app.seed.enabled=false");
        args.add("--app.demo.seed=false");
        if (tenantConfig.businessName() != null && !tenantConfig.businessName().isBlank()) {
            args.add("--app.business.name=" + tenantConfig.businessName().trim());
        }
        return args.toArray(String[]::new);
    }

    private String userFriendlyStartupError(Exception ex) {
        String message = rootCauseMessage(ex);
        if (message == null || message.isBlank()) {
            return "Unknown startup error. Check the terminal log for details.";
        }
        if (message.contains("Access denied")) {
            return "saved workspace database credential is not valid. Use another workspace, then join again.";
        }
        if (message.contains("Communications link failure") || message.contains("Failed to obtain JDBC Connection")) {
            return "could not connect to the tenant database.";
        }
        if (message.contains("Schema-validation") || message.contains("missing table") || message.contains("Unknown column")) {
            return "tenant database schema is not up to date. Run the database migration and reopen the workspace.";
        }
        return message;
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        Throwable last = throwable;
        while (current != null) {
            last = current;
            current = current.getCause();
        }
        return last != null && last.getMessage() != null ? last.getMessage() : throwable != null ? throwable.getMessage() : null;
    }

    private boolean isProfileActive(String profileName) {
        String profiles = propertyOrEnv("SPRING_PROFILES_ACTIVE", "");
        return Arrays.stream(profiles.split(","))
            .map(value -> value.trim().toLowerCase(Locale.ROOT))
            .anyMatch(profileName::equals);
    }

    private void validateDesktopProfiles() {
        List<String> forbiddenProfiles = List.of("demo", "migration", "provisioning");
        for (String profile : forbiddenProfiles) {
            if (isProfileActive(profile)) {
                throw new IllegalStateException(
                    "Desktop app must not run the '" + profile + "' profile. "
                        + "Use the tenant-client profile for desktop releases."
                );
            }
        }
    }

    private boolean desktopReleaseMode() {
        return Boolean.parseBoolean(propertyOrEnv("APP_DESKTOP_RELEASE", "false"))
            || getClass().getResource("/META-INF/pbl3-desktop-release.marker") != null;
    }

    private String propertyOrEnv(String key, String fallback) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }
        return Objects.requireNonNullElse(System.getenv(key), fallback);
    }

    private String provisioningApiBaseUrl() {
        return propertyOrEnv("PROVISIONING_API_BASE_URL", "");
    }
}
