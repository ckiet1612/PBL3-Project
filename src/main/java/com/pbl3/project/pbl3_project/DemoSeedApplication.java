package com.pbl3.project.pbl3_project;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class DemoSeedApplication {

    private DemoSeedApplication() {
    }

    public static void main(String[] args) {
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(Pbl3ProjectApplication.class)
            .profiles("demo")
            .headless(true)
            .properties(
                "spring.main.web-application-type=none",
                "spring.devtools.restart.enabled=false",
                "spring.devtools.livereload.enabled=false",
                "logging.level.org.hibernate.SQL=WARN",
                "logging.level.org.hibernate.orm.jdbc.bind=WARN"
            )
            .run(args)) {
            // CommandLineRunner beans seed the demo database during startup.
        }
    }
}
