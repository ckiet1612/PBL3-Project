package com.pbl3.project.pbl3_project.feature.orders;

import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.dto.OrderCreatedDto;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.AuthorizationService;
import com.pbl3.project.pbl3_project.service.OrderService;
import org.springframework.stereotype.Service;

@Service
public class OrderCheckoutFeature {
    private final OrderService orderService;
    private final AuthorizationService authorizationService;

    public OrderCheckoutFeature(OrderService orderService, AuthorizationService authorizationService) {
        this.orderService = orderService;
        this.authorizationService = authorizationService;
    }

    public OrderCreatedDto createOrder(User actor, CreateOrderRequest request) {
        authorizationService.requireSalesAccess(actor);
        if (request == null) {
            throw new IllegalArgumentException("Order request is required");
        }
        request.setUserId(actor.getId());
        Order order = orderService.createOrder(request);
        return OrderCreatedDto.from(order);
    }
}
