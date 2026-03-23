package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.InventoryTransaction;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.InventoryTransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryTransactionService {

    private final InventoryTransactionRepository transactionRepository;

    public InventoryTransactionService(InventoryTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public InventoryTransaction recordTransaction(Product product, Integer quantityChange, String type, Long referenceId, User user, String notes) {
        if (quantityChange == null || quantityChange == 0) {
            return null; // Skip if no change
        }
        
        InventoryTransaction tx = new InventoryTransaction(product, quantityChange, type, referenceId, user, notes);
        return transactionRepository.save(tx);
    }

    public List<InventoryTransaction> getTransactionsByProduct(Long productId) {
        return transactionRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public List<InventoryTransaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }
}
