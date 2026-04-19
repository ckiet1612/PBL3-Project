package com.pbl3.project.pbl3_project.controller;

import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }

    @org.springframework.web.bind.annotation.GetMapping
    public java.util.List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }
}
