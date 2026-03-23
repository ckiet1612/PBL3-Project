package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest;
import com.pbl3.project.pbl3_project.entity.*;
import com.pbl3.project.pbl3_project.repository.ImportOrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.SupplierRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class ImportOrderService {
    private final ImportOrderRepository importOrderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryTransactionService transactionService;

    public ImportOrderService(ImportOrderRepository importOrderRepository, 
                              ProductRepository productRepository, 
                              UserRepository userRepository,
                              SupplierRepository supplierRepository,
                              InventoryTransactionService transactionService) {
        this.importOrderRepository = importOrderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public ImportOrder createImportOrder(CreateImportOrderRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found: " + request.getSupplierId()));

        ImportOrder order = new ImportOrder();
        order.setUser(user);
        order.setSupplier(supplier);
        order.setNotes(request.getNotes());
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        order.setStatus("COMPLETED");

        double total = 0;

        for (CreateImportOrderRequest.ImportOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

            // Update Product Stock and Import Price using MAC (Moving Average Cost)
        double currentQty = product.getQuantity();
        double currentImportPrice = product.getImportPrice() != null ? product.getImportPrice() : 0.0;
        
        int addedQty = itemReq.getQuantity();
        double addedPrice = itemReq.getImportPrice();
        
        double newQuantity = currentQty + addedQty;
        double newImportPrice = currentImportPrice;
        
        if (newQuantity > 0) {
            newImportPrice = ((currentQty * currentImportPrice) + (addedQty * addedPrice)) / newQuantity;
        }

        product.setQuantity((int) newQuantity);
        product.setImportPrice(newImportPrice); 
        productRepository.save(product);

            ImportOrderItem orderItem = new ImportOrderItem();
            orderItem.setImportOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setImportPrice(itemReq.getImportPrice());

            order.getItems().add(orderItem);
            total += itemReq.getImportPrice() * itemReq.getQuantity();
        }

        order.setTotalCost(total);
        ImportOrder savedOrder = importOrderRepository.save(order);
        
        // Record inventory transactions
        for (ImportOrderItem item : savedOrder.getItems()) {
            transactionService.recordTransaction(
                item.getProduct(), 
                item.getQuantity(), 
                "IMPORT", 
                savedOrder.getId(), 
                user, 
                "Import Order #" + savedOrder.getId()
            );
        }
        
        return savedOrder;
    }

    public java.util.List<ImportOrder> getAllImportOrders() {
        return importOrderRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public ImportOrder getImportOrderWithItems(Long id) {
        return importOrderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Import Order not found: " + id));
    }
}
