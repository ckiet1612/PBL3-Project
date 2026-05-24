package com.pbl3.project.pbl3_project.ui.component;

import com.pbl3.project.pbl3_project.entity.ImportOrderStatus;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

public final class StatusBadgeFactory {

    private StatusBadgeFactory() {
    }

    public static Label product(Product product) {
        Label badge = new Label(productStockStatusText(product));
        badge.getStyleClass().add("product-stock-status-badge");
        if (isProductOutOfStock(product)) {
            badge.getStyleClass().add("out");
        } else if (isProductLowStock(product)) {
            badge.getStyleClass().add("low");
        } else {
            badge.getStyleClass().add("ok");
        }
        return badge;
    }

    public static Label order(OrderStatus status) {
        Label badge = new Label(formatOrderStatus(status));
        badge.getStyleClass().add("order-detail-status-badge");
        badge.setStyle("-fx-text-fill: " + orderStatusColor(status) + "; -fx-border-color: " + orderStatusColor(status) + ";");
        keepCompact(badge);
        return badge;
    }

    public static Label importOrder(ImportOrderStatus status) {
        Label badge = new Label(formatImportOrderStatus(status));
        badge.getStyleClass().add("import-detail-status-badge");
        badge.setStyle("-fx-text-fill: " + importOrderStatusColor(status) + "; -fx-border-color: " + importOrderStatusColor(status) + ";");
        keepCompact(badge);
        return badge;
    }

    public static Label stocktake(StocktakeSessionStatus status) {
        Label badge = new Label(formatStocktakeStatus(status));
        badge.getStyleClass().add("stocktake-detail-status-badge");
        badge.setStyle("-fx-text-fill: " + stocktakeStatusColor(status) + "; -fx-border-color: " + stocktakeStatusColor(status) + ";");
        keepCompact(badge);
        return badge;
    }

    private static void keepCompact(Label badge) {
        badge.setMaxWidth(Region.USE_PREF_SIZE);
        badge.setMinWidth(Region.USE_PREF_SIZE);
    }

    private static boolean isProductOutOfStock(Product product) {
        return product != null && safeProductInt(product.getQuantity()) <= 0;
    }

    private static boolean isProductLowStock(Product product) {
        return product != null && safeProductInt(product.getQuantity()) <= safeProductInt(product.getMinStockLevel());
    }

    private static int safeProductInt(Integer value) {
        return value != null ? value : 0;
    }

    private static String productStockStatusText(Product product) {
        if (isProductOutOfStock(product)) {
            return "Out of Stock";
        }
        if (isProductLowStock(product)) {
            return "Low Stock";
        }
        return "In Stock";
    }

    private static String formatOrderStatus(OrderStatus status) {
        return humanize(status != null ? status.name() : OrderStatus.COMPLETED.name());
    }

    private static String orderStatusColor(OrderStatus status) {
        OrderStatus safeStatus = status != null ? status : OrderStatus.COMPLETED;
        return switch (safeStatus) {
            case COMPLETED -> "-app-success-hover";
            case PARTIALLY_RETURNED, RETURNED -> "-app-primary-hover";
            case CANCELED -> "-app-danger-hover";
        };
    }

    private static String formatImportOrderStatus(ImportOrderStatus status) {
        ImportOrderStatus safeStatus = status != null ? status : ImportOrderStatus.COMPLETED;
        return switch (safeStatus) {
            case COMPLETED -> "Completed";
            case CANCELED -> "Canceled";
        };
    }

    private static String importOrderStatusColor(ImportOrderStatus status) {
        ImportOrderStatus safeStatus = status != null ? status : ImportOrderStatus.COMPLETED;
        return switch (safeStatus) {
            case COMPLETED -> "-app-success-hover";
            case CANCELED -> "-app-danger-hover";
        };
    }

    private static String formatStocktakeStatus(StocktakeSessionStatus status) {
        return humanize(status != null ? status.name() : StocktakeSessionStatus.OPEN.name());
    }

    private static String stocktakeStatusColor(StocktakeSessionStatus status) {
        StocktakeSessionStatus safeStatus = status != null ? status : StocktakeSessionStatus.OPEN;
        return switch (safeStatus) {
            case OPEN -> "-app-primary-hover";
            case APPLIED -> "-app-success-hover";
            case CANCELED -> "-app-danger-hover";
        };
    }

    private static String humanize(String token) {
        if (token == null || token.isBlank()) {
            return "Unknown";
        }
        String normalized = token.toLowerCase().replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String part : normalized.split("\\s+")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? token : builder.toString();
    }
}
