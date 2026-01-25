package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {
    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> getDailyStats() {
        LocalDateTime start = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);
        return getStats(start, end);
    }

    public Map<String, Object> getMonthlyStats() {
        LocalDateTime start = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().withDayOfMonth(LocalDateTime.now().toLocalDate().lengthOfMonth()).with(LocalTime.MAX);
        return getStats(start, end);
    }

    private Map<String, Object> getStats(LocalDateTime start, LocalDateTime end) {
        Double revenue = orderRepository.sumRevenueBetween(start, end);
        Long count = orderRepository.countOrdersBetween(start, end);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("revenue", revenue != null ? revenue : 0.0);
        stats.put("orders", count != null ? count : 0);
        return stats;
    }
}
