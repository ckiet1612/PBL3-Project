package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReportService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public ReportService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
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
    
    // Count products with quantity below threshold
    public long getLowStockCount(int threshold) {
        return productRepository.countByQuantityLessThanAndIsDeletedFalse(threshold);
    }
    
    // Get daily revenue for the last 7 days
    public Map<String, Double> getLast7DaysRevenue() {
        Map<String, Double> result = new LinkedHashMap<>(); // Preserve insertion order
        LocalDate today = LocalDate.now();
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            
            Double revenue = orderRepository.sumRevenueBetween(start, end);
            String label = date.getDayOfMonth() + "/" + date.getMonthValue();
            result.put(label, revenue != null ? revenue : 0.0);
        }
        
        return result;
    }
}
