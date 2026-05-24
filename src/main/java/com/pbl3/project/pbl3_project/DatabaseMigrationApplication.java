package com.pbl3.project.pbl3_project;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class DatabaseMigrationApplication {

    private DatabaseMigrationApplication() {
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(SalesManagementApplication.class)
            .web(WebApplicationType.NONE)
            .properties(
                "spring.flyway.enabled=true",
                "spring.flyway.ignore-migration-patterns=*:missing",
                "app.seed.enabled=false",
                "app.demo.seed=false",
                "app.version-gate.enabled=false"
            )
            .run(args);
        context.close();
    }
}
