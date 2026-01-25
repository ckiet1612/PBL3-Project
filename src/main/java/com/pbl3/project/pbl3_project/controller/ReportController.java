package com.pbl3.project.pbl3_project.controller;

import com.pbl3.project.pbl3_project.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> getDailyReport() {
        return ResponseEntity.ok(reportService.getDailyStats());
    }

    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyReport() {
        return ResponseEntity.ok(reportService.getMonthlyStats());
    }
}
