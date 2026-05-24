package com.pbl3.project.pbl3_project.controller;

import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.feature.reports.ReportsFeature;
import com.pbl3.project.pbl3_project.service.ApiSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportsFeature reportsFeature;
    private final ApiSessionService apiSessionService;

    public ReportController(ReportsFeature reportsFeature, ApiSessionService apiSessionService) {
        this.reportsFeature = reportsFeature;
        this.apiSessionService = apiSessionService;
    }

    @GetMapping("/daily")
    public ResponseEntity<?> getDailyReport(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        return ResponseEntity.ok(reportsFeature.getDailyStats(actor));
    }

    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyReport(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        return ResponseEntity.ok(reportsFeature.getMonthlyStats(actor));
    }
}
