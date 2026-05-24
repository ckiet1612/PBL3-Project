package com.pbl3.project.pbl3_project.feature.reports;

import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.AuthorizationService;
import com.pbl3.project.pbl3_project.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ReportsFeature {
    private final ReportService reportService;
    private final AuthorizationService authorizationService;

    public ReportsFeature(ReportService reportService, AuthorizationService authorizationService) {
        this.reportService = reportService;
        this.authorizationService = authorizationService;
    }

    public Map<String, Object> getDailyStats(User actor) {
        authorizationService.requireReportsAccess(actor);
        return reportService.getDailyStats();
    }

    public Map<String, Object> getMonthlyStats(User actor) {
        authorizationService.requireReportsAccess(actor);
        return reportService.getMonthlyStats();
    }
}
