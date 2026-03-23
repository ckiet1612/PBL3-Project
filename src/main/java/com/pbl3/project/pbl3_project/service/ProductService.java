package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryTransactionService transactionService;

    public ProductService(ProductRepository productRepository, InventoryTransactionService transactionService) {
        this.productRepository = productRepository;
        this.transactionService = transactionService;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAllByIsDeletedFalse();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public Product saveProduct(Product product, com.pbl3.project.pbl3_project.entity.User user, String notes) {
        int oldQty = 0;
        if (product.getId() != null) {
            oldQty = productRepository.findById(product.getId())
                    .map(Product::getQuantity)
                    .orElse(0);
        }
        
        Product saved = productRepository.save(product);
        
        int diff = (saved.getQuantity() != null ? saved.getQuantity() : 0) - oldQty;
        if (diff != 0 && user != null) {
            transactionService.recordTransaction(saved, diff, "MANUAL_ADJUST", null, user, notes);
        }
        return saved;
    }

    public void deleteProduct(Long id) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            Product p = product.get();
            p.setDeleted(true);
            productRepository.save(p);
        }
    }

    public void deleteProduct(Long id, com.pbl3.project.pbl3_project.entity.User user) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            Product p = product.get();
            p.setDeleted(true);
            productRepository.save(p);
            
            if (p.getQuantity() != null && p.getQuantity() > 0 && user != null) {
                transactionService.recordTransaction(p, -p.getQuantity(), "DELETE", null, user, "Product deleted");
            }
        }
    }
}
