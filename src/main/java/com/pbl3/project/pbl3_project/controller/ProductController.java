package com.pbl3.project.pbl3_project.controller;

import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.feature.products.ProductCatalogFeature;
import com.pbl3.project.pbl3_project.service.ApiSessionService;
import com.pbl3.project.pbl3_project.service.AuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductCatalogFeature productCatalogFeature;
    private final ApiSessionService apiSessionService;
    private final AuthorizationService authorizationService;

    public ProductController(
        ProductCatalogFeature productCatalogFeature,
        ApiSessionService apiSessionService,
        AuthorizationService authorizationService
    ) {
        this.productCatalogFeature = productCatalogFeature;
        this.apiSessionService = apiSessionService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<?> getAllProducts(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        return ResponseEntity.ok(productCatalogFeature.listProducts(actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        return productCatalogFeature.getProduct(actor, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createProduct(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody Object product
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        authorizationService.requireProductWrite(actor);
        return ResponseEntity.status(501).body("Product write API is not implemented yet");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateProduct(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id,
        @RequestBody Object productDetails
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        authorizationService.requireProductWrite(actor);
        return ResponseEntity.status(501).body("Product write API is not implemented yet");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        authorizationService.requireProductDelete(actor);
        return ResponseEntity.status(501).body("Product delete API is not implemented yet");
    }
}
