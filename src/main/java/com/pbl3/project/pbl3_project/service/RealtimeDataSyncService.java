package com.pbl3.project.pbl3_project.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RealtimeDataSyncService {
    private final JdbcTemplate jdbcTemplate;

    public RealtimeDataSyncService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public String productInventoryToken() {
        String token = jdbcTemplate.queryForObject(
            """
                SELECT
                    COUNT(*),
                    COALESCE(SUM(quantity), 0),
                    COALESCE(SUM(version), 0),
                    COALESCE(MAX(id), 0)
                FROM products
                WHERE is_deleted = FALSE
                """,
            (rs, rowNum) -> rs.getString(1) + ":" + rs.getString(2) + ":" + rs.getString(3) + ":" + rs.getString(4)
        );
        return token == null ? "" : token;
    }
}
