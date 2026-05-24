package com.pbl3.project.pbl3_project.ui.scene.pos;

import com.pbl3.project.pbl3_project.entity.Product;
import java.util.Optional;

public final class PosProductSearchSupport {
    private PosProductSearchSupport() {
    }

    public static Optional<Product> resolvePosScannedProduct(java.util.List<Product> products, String normalizedQuery) {
        if (products == null || normalizedQuery == null || normalizedQuery.isBlank()) {
            return Optional.empty();
        }
        Optional<Product> barcodeMatch = findSingleExactProduct(products, normalizedQuery, Product::getBarcode);
        if (barcodeMatch.isPresent()) {
            return barcodeMatch;
        }
        Optional<Product> skuMatch = findSingleExactProduct(products, normalizedQuery, Product::getSku);
        if (skuMatch.isPresent()) {
            return skuMatch;
        }
        return findSingleExactProduct(products, normalizedQuery, Product::getName);
    }

    public static boolean matchesPosProductSearch(Product product, String normalizedQuery) {
        if (product == null) {
            return false;
        }
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return true;
        }
        return containsNormalized(product.getName(), normalizedQuery)
            || containsNormalized(product.getSku(), normalizedQuery)
            || containsNormalized(product.getBarcode(), normalizedQuery);
    }

    public static String normalizeProductLookup(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static Optional<Product> findSingleExactProduct(
        java.util.List<Product> products,
        String normalizedQuery,
        java.util.function.Function<Product, String> valueExtractor
    ) {
        Product match = null;
        for (Product product : products) {
            if (product == null || !normalizedQuery.equals(normalizeProductLookup(valueExtractor.apply(product)))) {
                continue;
            }
            if (match != null) {
                return Optional.empty();
            }
            match = product;
        }
        return Optional.ofNullable(match);
    }

    private static boolean containsNormalized(String value, String normalizedQuery) {
        return normalizeProductLookup(value).contains(normalizedQuery);
    }
}
