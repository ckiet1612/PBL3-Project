package com.pbl3.project.pbl3_project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseWarmupService {

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;
    private final int connectionCount;

    public DatabaseWarmupService(
        JdbcTemplate jdbcTemplate,
        @Value("${app.database.warmup.enabled:true}") boolean enabled,
        @Value("${app.database.warmup.connection-count:2}") int connectionCount
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
        this.connectionCount = Math.max(1, connectionCount);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpConnections() {
        if (!enabled) {
            return;
        }
        Thread worker = new Thread(this::runWarmupQueries, "database-warmup");
        worker.setDaemon(true);
        worker.start();
    }

    private void runWarmupQueries() {
        for (int i = 0; i < connectionCount; i++) {
            try {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            } catch (RuntimeException ex) {
                System.err.println("Database warm-up failed: " + ex.getMessage());
                return;
            }
        }
    }
}
