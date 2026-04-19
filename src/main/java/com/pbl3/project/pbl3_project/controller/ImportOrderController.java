package com.pbl3.project.pbl3_project.controller;

import com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest;
import com.pbl3.project.pbl3_project.entity.ImportOrder;
import com.pbl3.project.pbl3_project.service.ImportOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/import-orders")
public class ImportOrderController {
    private final ImportOrderService importOrderService;

    public ImportOrderController(ImportOrderService importOrderService) {
        this.importOrderService = importOrderService;
    }

    @PostMapping
    public ResponseEntity<ImportOrder> createImportOrder(@RequestBody CreateImportOrderRequest request) {
        ImportOrder order = importOrderService.createImportOrder(request);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public List<ImportOrder> getAllImportOrders() {
        return importOrderService.getAllImportOrders();
    }
}
