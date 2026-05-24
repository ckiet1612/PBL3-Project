package com.pbl3.project.pbl3_project.dto;

import com.pbl3.project.pbl3_project.entity.Brand;
import com.pbl3.project.pbl3_project.entity.Category;
import com.pbl3.project.pbl3_project.entity.Origin;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Unit;

import java.math.BigDecimal;

public record ProductReadDto(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Integer quantity,
    String imageUrl,
    String sku,
    String barcode,
    BigDecimal importPrice,
    Integer minStockLevel,
    boolean deleted,
    NamedRef category,
    NamedRef brand,
    NamedRef origin,
    NamedRef unit
) {
    public static ProductReadDto from(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductReadDto(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getQuantity(),
            product.getImageUrl(),
            product.getSku(),
            product.getBarcode(),
            product.getImportPrice(),
            product.getMinStockLevel(),
            product.isDeleted(),
            NamedRef.from(product.getCategory()),
            NamedRef.from(product.getBrand()),
            NamedRef.from(product.getOrigin()),
            NamedRef.from(product.getUnit())
        );
    }

    public record NamedRef(Long id, String name) {
        private static NamedRef from(Category category) {
            return category == null ? null : new NamedRef(category.getId(), category.getName());
        }

        private static NamedRef from(Brand brand) {
            return brand == null ? null : new NamedRef(brand.getId(), brand.getName());
        }

        private static NamedRef from(Origin origin) {
            return origin == null ? null : new NamedRef(origin.getId(), origin.getName());
        }

        private static NamedRef from(Unit unit) {
            return unit == null ? null : new NamedRef(unit.getId(), unit.getName());
        }
    }
}
