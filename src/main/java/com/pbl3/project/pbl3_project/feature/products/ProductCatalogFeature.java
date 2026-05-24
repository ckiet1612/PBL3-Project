package com.pbl3.project.pbl3_project.feature.products;

import com.pbl3.project.pbl3_project.dto.ProductReadDto;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.AuthorizationService;
import com.pbl3.project.pbl3_project.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductCatalogFeature {
    private final ProductService productService;
    private final AuthorizationService authorizationService;

    public ProductCatalogFeature(ProductService productService, AuthorizationService authorizationService) {
        this.productService = productService;
        this.authorizationService = authorizationService;
    }

    public List<ProductReadDto> listProducts(User actor) {
        authorizationService.requireSalesAccess(actor);
        return productService.getAllProducts().stream()
            .map(ProductReadDto::from)
            .toList();
    }

    public Optional<ProductReadDto> getProduct(User actor, Long productId) {
        authorizationService.requireSalesAccess(actor);
        return productService.getProductById(productId)
            .map(ProductReadDto::from);
    }
}
