package com.pbl3.project.pbl3_project.controller;

import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.feature.orders.OrderCheckoutFeature;
import com.pbl3.project.pbl3_project.service.ApiSessionService;
import com.pbl3.project.pbl3_project.service.AuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderCheckoutFeature orderCheckoutFeature;
    private final ApiSessionService apiSessionService;
    private final AuthorizationService authorizationService;

    public OrderController(
        OrderCheckoutFeature orderCheckoutFeature,
        ApiSessionService apiSessionService,
        AuthorizationService authorizationService
    ) {
        this.orderCheckoutFeature = orderCheckoutFeature;
        this.apiSessionService = apiSessionService;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody CreateOrderRequest request
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        return ResponseEntity.ok(orderCheckoutFeature.createOrder(actor, request));
    }

    @GetMapping
    public ResponseEntity<String> getAllOrders(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        authorizationService.requireOrderHistoryAccess(actor);
        return ResponseEntity.status(501).body("Orders list API is not implemented yet");
    }
}
