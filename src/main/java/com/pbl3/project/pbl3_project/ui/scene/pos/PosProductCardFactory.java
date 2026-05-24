package com.pbl3.project.pbl3_project.ui.scene.pos;

import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.PromotionService;
import java.math.BigDecimal;
import java.util.function.Function;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class PosProductCardFactory {
    private PosProductCardFactory() {
    }

    public static PosProductCardView createProductCard(
        Product product,
        PromotionService.ProductPricingPreview pricingPreview,
        int inCartQuantity,
        DoubleProperty widthProp,
        Function<BigDecimal, String> moneyFormatter,
        Runnable onAddToCart
    ) {
        VBox card = new VBox(12);
        card.getStyleClass().add("pos-product-card");
        card.prefWidthProperty().bind(widthProp);
        card.minWidthProperty().bind(widthProp);
        card.maxWidthProperty().bind(widthProp);
        if (product.getQuantity() <= 0) {
            card.getStyleClass().add("pos-product-card-unavailable");
        }

        Label initial = new Label(product.getName() != null && !product.getName().isBlank()
            ? product.getName().substring(0, 1).toUpperCase()
            : "?");
        initial.getStyleClass().add("pos-product-initial");

        Label categoryLabel = new Label(product.getCategory() != null ? product.getCategory().getName() : "Uncategorized");
        categoryLabel.getStyleClass().add("pos-product-category");

        VBox heroBody = new VBox(4, initial, categoryLabel);
        heroBody.setAlignment(Pos.CENTER_LEFT);

        Label stockBadge = new Label(product.getQuantity() <= 0
            ? "Out of stock"
            : (product.getQuantity() <= (product.getMinStockLevel() != null ? product.getMinStockLevel() : 0)
                ? java.text.MessageFormat.format("Low stock: {0}", product.getQuantity())
                : java.text.MessageFormat.format("Stock: {0}", product.getQuantity())));
        stockBadge.getStyleClass().add("pos-stock-badge");
        if (product.getQuantity() <= 0) {
            stockBadge.getStyleClass().add("pos-stock-badge-empty");
        } else if (product.getMinStockLevel() != null && product.getQuantity() <= product.getMinStockLevel()) {
            stockBadge.getStyleClass().add("pos-stock-badge-low");
        }

        VBox badgeColumn = new VBox(8);
        badgeColumn.setAlignment(Pos.TOP_RIGHT);
        if (pricingPreview != null && pricingPreview.hasPromotion() && pricingPreview.promotion() != null) {
            Label promoBadge = new Label(pricingPreview.promotion().getName());
            promoBadge.getStyleClass().addAll("pos-product-badge", "pos-product-badge-promo");
            promoBadge.setMaxWidth(120);
            promoBadge.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            badgeColumn.getChildren().add(promoBadge);
        }
        Label inCartBadge = new Label();
        inCartBadge.getStyleClass().addAll("pos-product-badge", "pos-product-badge-cart");
        badgeColumn.getChildren().add(inCartBadge);

        Region heroSpacer = new Region();
        HBox.setHgrow(heroSpacer, Priority.ALWAYS);
        HBox hero = new HBox(10, heroBody, heroSpacer, badgeColumn);
        hero.getStyleClass().add("pos-product-hero");
        hero.setAlignment(Pos.TOP_LEFT);

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("pos-product-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        nameLabel.setPrefHeight(42);

        BigDecimal originalPrice = pricingPreview != null
            ? pricingPreview.originalUnitPrice()
            : MoneySupport.normalize(product.getPrice());
        BigDecimal currentPrice = pricingPreview != null ? pricingPreview.discountedUnitPrice() : originalPrice;

        Label currentPriceLabel = new Label(formatMoney(moneyFormatter, currentPrice));
        currentPriceLabel.getStyleClass().add("pos-product-price");
        if (pricingPreview != null && pricingPreview.hasPromotion()) {
            currentPriceLabel.getStyleClass().add("pos-product-price-promo");
        }

        VBox priceBox = new VBox(4, currentPriceLabel);
        if (pricingPreview != null && pricingPreview.hasPromotion()) {
            Label originalPriceLabel = new Label(formatMoney(moneyFormatter, originalPrice));
            originalPriceLabel.getStyleClass().add("pos-product-price-original");
            priceBox.getChildren().add(originalPriceLabel);
        }

        Button addButton = new Button(product.getQuantity() > 0 ? "Add to Cart" : "Unavailable");
        addButton.getStyleClass().add("pos-product-add-button");
        addButton.setDisable(product.getQuantity() <= 0);
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(event -> {
            if (onAddToCart != null) {
                onAddToCart.run();
            }
        });

        card.getChildren().addAll(hero, nameLabel, priceBox, stockBadge, addButton);
        PosProductCardView cardView = new PosProductCardView(card, inCartBadge);
        cardView.setInCartQuantity(inCartQuantity);
        return cardView;
    }

    private static String formatMoney(Function<BigDecimal, String> moneyFormatter, BigDecimal amount) {
        return moneyFormatter != null ? moneyFormatter.apply(amount) : MoneySupport.normalize(amount).toPlainString();
    }
}
