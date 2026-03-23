package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderItem;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryTransactionService transactionService;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, UserRepository userRepository, InventoryTransactionService transactionService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setCreatedAt(LocalDateTime.now());
        order.setOrderItems(new ArrayList<>());
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : com.pbl3.project.pbl3_project.entity.PaymentMethod.CASH);
        order.setTotalPrice(0.0);

        double total = 0;

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemRequest.getProductId()));

            if (product.getQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException("Not enough stock for product: " + product.getName());
            }

            // Deduct stock
            product.setQuantity(product.getQuantity() - itemRequest.getQuantity());
            productRepository.save(product);

            // Create OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());

            order.getOrderItems().add(orderItem);
            total += product.getPrice() * itemRequest.getQuantity();
        }

        order.setTotalPrice(total);
        Order savedOrder = orderRepository.save(order);
        
        // Record inventory transactions
        for (OrderItem item : savedOrder.getOrderItems()) {
            transactionService.recordTransaction(
                item.getProduct(), 
                -item.getQuantity(), 
                "SALE", 
                savedOrder.getId(), 
                user, 
                "Sale Order #" + savedOrder.getId()
            );
        }
        
        return savedOrder;
    }

    public java.util.List<Order> getAllOrders() {
        return orderRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public Order getOrderWithItems(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }
}
