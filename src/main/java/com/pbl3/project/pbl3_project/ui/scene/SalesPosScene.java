package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.dto.payment.QrPaymentCreateRequest;
import com.pbl3.project.pbl3_project.dto.payment.QrPaymentStatusDto;
import com.pbl3.project.pbl3_project.entity.Category;
import com.pbl3.project.pbl3_project.entity.Customer;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.feature.orders.ui.PosCartViewModel;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.PromotionService;
import com.pbl3.project.pbl3_project.service.ReceiptService;
import com.pbl3.project.pbl3_project.service.SalesShiftService;
import com.pbl3.project.pbl3_project.ui.dialog.QrPaymentDialog;
import com.pbl3.project.pbl3_project.ui.scene.pos.PosProductCardView;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.RealtimeDataSync;
import com.pbl3.project.pbl3_project.ui.util.UiTaskExecutor;
import java.math.BigDecimal;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import static com.pbl3.project.pbl3_project.ui.scene.pos.PosProductSearchSupport.matchesPosProductSearch;
import static com.pbl3.project.pbl3_project.ui.scene.pos.PosProductSearchSupport.normalizeProductLookup;
import static com.pbl3.project.pbl3_project.ui.scene.pos.PosProductSearchSupport.resolvePosScannedProduct;
import static com.pbl3.project.pbl3_project.ui.scene.pos.PosCustomerCardSupport.createCustomerContextIcon;
import static com.pbl3.project.pbl3_project.ui.scene.pos.PosCustomerCardSupport.updateCustomerCard;
import static com.pbl3.project.pbl3_project.ui.scene.pos.PosProductCardFactory.createProductCard;
import static com.pbl3.project.pbl3_project.ui.scene.pos.PosShiftCardSupport.createShiftCard;
import static com.pbl3.project.pbl3_project.ui.scene.pos.PosShiftCardSupport.showPendingShiftState;
import static com.pbl3.project.pbl3_project.ui.scene.pos.PosShiftCardSupport.updateShiftCard;

public final class SalesPosScene {
    private static final javafx.scene.paint.Color PRIMARY_COLOR = javafx.scene.paint.Color.web("#1d7df2");
    private static final javafx.scene.paint.Color TEXT_MUTED_COLOR = javafx.scene.paint.Color.web("#78909C");

    public record Options() {
    }

    private record CheckoutSelection(
        PaymentMethod paymentMethod,
        boolean printReceipt,
        Long selectedOrderPromotionId,
        BigDecimal amountDue
    ) {
    }

    private record ShiftCashInput(BigDecimal amount, String note) {
    }

    private record CheckoutTaskResult(
        Order order,
        String receiptError,
        java.util.List<Product> products,
        java.util.Map<Long, PromotionService.ProductPricingPreview> pricingByProductId,
        SalesShiftService.ShiftSummary shiftSummary
    ) {
    }

    private record ProductCatalogSnapshot(
        java.util.List<Product> products,
        java.util.List<Category> categories,
        java.util.Map<Long, PromotionService.ProductPricingPreview> pricingByProductId
    ) {
    }

    private final SceneRuntimeContext context;

    private SalesPosScene(SceneRuntimeContext context) {
        this.context = context;
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        return new SalesPosScene(context).createSalesView(context.owner(), user);
    }


    private BigDecimal calculatePosCartSubtotal(
        java.util.List<CreateOrderRequest.OrderItemRequest> cartItems,
        java.util.List<Product> allProducts,
        java.util.Map<Long, PromotionService.ProductPricingPreview> pricingByProductId
    ) {
        if (cartItems == null || cartItems.isEmpty()) {
            return MoneySupport.ZERO;
        }
        java.util.Map<Long, Product> productsById = new java.util.LinkedHashMap<>();
        if (allProducts != null) {
            for (Product product : allProducts) {
                if (product != null && product.getId() != null) {
                    productsById.put(product.getId(), product);
                }
            }
        }

        BigDecimal subtotal = MoneySupport.ZERO;
        for (CreateOrderRequest.OrderItemRequest item : cartItems) {
            if (item == null || item.getProductId() == null) {
                continue;
            }
            Product product = productsById.get(item.getProductId());
            if (product == null) {
                continue;
            }
            PromotionService.ProductPricingPreview preview = pricingByProductId != null
                ? pricingByProductId.get(item.getProductId())
                : null;
            BigDecimal unitPrice = preview != null ? preview.discountedUnitPrice() : product.getPrice();
            subtotal = MoneySupport.add(subtotal, MoneySupport.multiply(unitPrice, item.getQuantity()));
        }
        return subtotal;
    }

    private Node createSalesView(Stage stage, User user) {
        javafx.scene.control.SplitPane splitPane = new javafx.scene.control.SplitPane();
        splitPane.getStyleClass().add("pos-workspace");
        splitPane.setDividerPositions(0.68);

        java.util.List<Product> allProducts =
            new java.util.ArrayList<>(context.productService().getAllProducts());
        final java.util.Map<Long, PromotionService.ProductPricingPreview>[] pricingByProductIdRef =
            new java.util.Map[]{context.promotionService().previewBestProductPricing(allProducts, java.time.LocalDateTime.now())};
        java.util.List<Category> categories = new java.util.ArrayList<>(context.categoryRepository().findAll());
        java.util.Map<Long, PosProductCardView> productCardCache = new java.util.LinkedHashMap<>();
        final Category[] selectedCategory = {null};

        javafx.collections.ObservableList<PosCartViewModel> cartSessions =
            javafx.collections.FXCollections.observableArrayList(new PosCartViewModel(1));
        java.util.concurrent.atomic.AtomicReference<PosCartViewModel> activeCartSessionRef =
            new java.util.concurrent.atomic.AtomicReference<>(cartSessions.get(0));
        java.util.concurrent.atomic.AtomicReference<SalesShiftService.ShiftSummary> shiftSummaryRef =
            new java.util.concurrent.atomic.AtomicReference<>(context.salesShiftService().getCurrentShiftSummary(user));
        final int[] nextCartSessionNumber = {2};

        VBox browserPane = new VBox(16);
        browserPane.getStyleClass().add("pos-browser-pane");

        javafx.scene.shape.SVGPath searchIcon = new javafx.scene.shape.SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.setFill(PRIMARY_COLOR);

        TextField searchField = new TextField();
        searchField.setPromptText("Scan barcode or search by SKU/name");
        searchField.getStyleClass().add("pos-search-field");

        HBox searchShell = new HBox(10, searchIcon, searchField);
        searchShell.getStyleClass().add("pos-search-shell");
        searchShell.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        javafx.scene.layout.FlowPane categoryStrip = new javafx.scene.layout.FlowPane();
        categoryStrip.getStyleClass().add("pos-category-bar");
        categoryStrip.setAlignment(Pos.CENTER_LEFT);
        categoryStrip.setHgap(10);
        categoryStrip.setVgap(10);
        categoryStrip.setMaxWidth(Double.MAX_VALUE);
        categoryStrip.prefWrapLengthProperty().bind(browserPane.widthProperty().subtract(24));

        javafx.scene.layout.FlowPane productGrid = new javafx.scene.layout.FlowPane();
        productGrid.setHgap(16);
        productGrid.setVgap(16);
        productGrid.setPadding(new Insets(4, 0, 8, 0));
        productGrid.setAlignment(Pos.TOP_LEFT);
        productGrid.getStyleClass().add("pos-product-grid");

        javafx.scene.control.ScrollPane productScroll = new javafx.scene.control.ScrollPane(productGrid);
        productScroll.setFitToWidth(true);
        productScroll.getStyleClass().add("pos-product-scroll");
        VBox.setVgrow(productScroll, Priority.ALWAYS);

        javafx.beans.property.DoubleProperty cardWidthProp = new javafx.beans.property.SimpleDoubleProperty(220);
        productScroll.viewportBoundsProperty().addListener((obs, oldV, newV) -> {
            double width = newV.getWidth() - productGrid.getPadding().getLeft() - productGrid.getPadding().getRight() - 5;
            if (width > 0) {
                double minCardWidth = 220;
                double hgap = productGrid.getHgap();
                int cols = (int) Math.max(1, Math.floor((width + hgap) / (minCardWidth + hgap)));
                double computedWidth = Math.floor((width - (cols - 1) * hgap) / cols);
                cardWidthProp.set(computedWidth);
            }
        });

        browserPane.getChildren().addAll(searchShell, categoryStrip, productScroll);

        VBox cartPane = new VBox(10);
        cartPane.getStyleClass().add("pos-cart-pane");
        cartPane.setMinWidth(390);
        cartPane.setPrefWidth(430);
        cartPane.setMaxWidth(470);
        javafx.scene.control.SplitPane.setResizableWithParent(cartPane, false);

        Label cartTitle = new Label("Current Order");
        cartTitle.getStyleClass().add("header-label");
        Label cartMetaLabel = new Label(java.text.MessageFormat.format("{0} item", 0));
        cartMetaLabel.getStyleClass().add("pos-cart-meta");

        Region cartTitleSpacer = new Region();
        HBox.setHgrow(cartTitleSpacer, Priority.ALWAYS);
        HBox cartTitleRow = new HBox(10, cartTitle, cartTitleSpacer, cartMetaLabel);
        cartTitleRow.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.layout.FlowPane cartTabBar = new javafx.scene.layout.FlowPane();
        cartTabBar.getStyleClass().add("pos-cart-tab-bar");
        cartTabBar.setHgap(8);
        cartTabBar.setVgap(8);
        cartTabBar.setAlignment(Pos.CENTER_LEFT);

        Runnable[] renderCategoryStripRef = new Runnable[1];
        Runnable[] renderProductsRef = new Runnable[1];
        Runnable[] renderCartTabsRef = new Runnable[1];
        Runnable[] refreshCartRef = new Runnable[1];
        Runnable[] refreshProductCatalogRef = new Runnable[1];
        java.util.concurrent.atomic.AtomicBoolean productCatalogRefreshInFlight = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean productCatalogRefreshAgain = new java.util.concurrent.atomic.AtomicBoolean(false);

        Label selectedCustomerNameLabel = new Label("Guest");
        selectedCustomerNameLabel.getStyleClass().add("pos-customer-name");
        Label selectedCustomerPhoneLabel = new Label("");
        selectedCustomerPhoneLabel.getStyleClass().add("pos-customer-phone");
        selectedCustomerPhoneLabel.setManaged(false);
        selectedCustomerPhoneLabel.setVisible(false);
        Label customerStateBadge = new Label("Guest");
        customerStateBadge.getStyleClass().addAll("pos-customer-badge", "pos-customer-badge-guest");

        Button selectCustomerButton = new Button("Select");
        selectCustomerButton.getStyleClass().addAll("primary-button", "no-hover-button");
        Button clearCustomerButton = new Button("Clear");
        clearCustomerButton.getStyleClass().addAll("primary-text-button", "no-hover-button");
        clearCustomerButton.setDisable(true);

        Region customerHeaderSpacer = new Region();
        HBox.setHgrow(customerHeaderSpacer, Priority.ALWAYS);
        HBox customerActionRow = new HBox(10, selectCustomerButton, clearCustomerButton);
        customerActionRow.getStyleClass().add("pos-customer-action-row");
        customerActionRow.setAlignment(Pos.CENTER_RIGHT);
        HBox customerHeader = new HBox(
            10,
            createCustomerContextIcon(),
            customerHeaderSpacer,
            customerStateBadge
        );
        customerHeader.getStyleClass().add("pos-customer-header");
        customerHeader.setAlignment(Pos.CENTER_LEFT);

        VBox customerCard = new VBox(10, customerHeader, selectedCustomerNameLabel, selectedCustomerPhoneLabel, customerActionRow);
        customerCard.getStyleClass().add("pos-customer-card");
        updateCustomerCard(customerCard, selectedCustomerNameLabel, selectedCustomerPhoneLabel, customerStateBadge, clearCustomerButton, null);

        selectCustomerButton.setOnAction(e -> {
            try {
                Customer picked = showCustomerPickerDialog(stage, user);
                if (picked != null) {
                    activeCartSessionRef.get().setCustomer(picked);
                    updateCustomerCard(customerCard, selectedCustomerNameLabel, selectedCustomerPhoneLabel, customerStateBadge, clearCustomerButton, picked);
                    renderCartTabsRef[0].run();
                }
            } catch (Exception ex) {
                context.showUserFacingError(ex);
            }
        });
        clearCustomerButton.setOnAction(e -> {
            activeCartSessionRef.get().setCustomer(null);
            updateCustomerCard(customerCard, selectedCustomerNameLabel, selectedCustomerPhoneLabel, customerStateBadge, clearCustomerButton, null);
            renderCartTabsRef[0].run();
        });

        VBox cartItemsBox = new VBox(12);
        cartItemsBox.getStyleClass().add("pos-cart-items");

        javafx.scene.control.ScrollPane cartScroll = new javafx.scene.control.ScrollPane(cartItemsBox);
        cartScroll.setFitToWidth(true);
        cartScroll.getStyleClass().add("pos-cart-scroll");
        VBox.setVgrow(cartScroll, Priority.ALWAYS);

        Label subtotalCaption = new Label("Subtotal");
        subtotalCaption.getStyleClass().add("pos-summary-label");
        Label subtotalValueLabel = new Label(context.support().formatVnd(MoneySupport.ZERO));
        subtotalValueLabel.getStyleClass().add("pos-summary-value");

        Button checkoutButton = new Button("Proceed to Checkout");
        checkoutButton.getStyleClass().add("pos-checkout-button");
        checkoutButton.setDisable(true);
        checkoutButton.setMaxWidth(Double.MAX_VALUE);

        Label shiftStatusLabel = new Label();
        Label shiftOpenedLabel = new Label();
        Label shiftOpeningCashLabel = new Label();
        Label shiftSalesLabel = new Label();
        Label shiftRefundsLabel = new Label();
        Label shiftExpensesLabel = new Label();
        Label shiftExpectedCashLabel = new Label();
        Button openShiftButton = new Button("Open Shift");
        openShiftButton.getStyleClass().addAll("primary-button", "no-hover-button");
        Button closeShiftButton = new Button("Close Shift");
        closeShiftButton.getStyleClass().addAll("dashboard-report-secondary-button", "no-hover-button");

        VBox shiftCard = createShiftCard(
            shiftStatusLabel,
            shiftOpenedLabel,
            shiftOpeningCashLabel,
            shiftSalesLabel,
            shiftRefundsLabel,
            shiftExpensesLabel,
            shiftExpectedCashLabel,
            openShiftButton,
            closeShiftButton
        );

        HBox subtotalRow = new HBox(8, subtotalCaption, new Region(), subtotalValueLabel);
        subtotalRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(subtotalRow.getChildren().get(1), Priority.ALWAYS);

        VBox cartSummaryCard = new VBox(10, subtotalRow);
        cartSummaryCard.getStyleClass().add("pos-cart-summary");

        cartPane.getChildren().addAll(cartTitleRow, shiftCard, cartTabBar, customerCard, cartScroll, cartSummaryCard, checkoutButton);

        Runnable[] refreshShiftRef = new Runnable[1];
        refreshShiftRef[0] = () -> {
            try {
                shiftSummaryRef.set(context.salesShiftService().getCurrentShiftSummary(user));
                updateShiftCard(
                    context.support(),
                    shiftSummaryRef.get(),
                    shiftStatusLabel,
                    shiftOpenedLabel,
                    shiftOpeningCashLabel,
                    shiftSalesLabel,
                    shiftRefundsLabel,
                    shiftExpensesLabel,
                    shiftExpectedCashLabel,
                    openShiftButton,
                    closeShiftButton
                );
            } catch (Exception ex) {
                context.showUserFacingError(ex);
            }
        };

        openShiftButton.setOnAction(event -> {
            Optional<ShiftCashInput> input = showShiftCashDialog(
                stage,
                "Open Shift",
                "Opening cash",
                MoneySupport.ZERO,
                false,
                "Opening note"
            );
            if (input.isEmpty()) {
                return;
            }
            showPendingShiftState(shiftStatusLabel, openShiftButton, closeShiftButton, checkoutButton, "OPENING...", "Opening...");
            javafx.concurrent.Task<SalesShiftService.ShiftSummary> task = new javafx.concurrent.Task<>() {
                @Override
                protected SalesShiftService.ShiftSummary call() {
                    context.salesShiftService().openShift(user, input.get().amount(), input.get().note());
                    return context.salesShiftService().getCurrentShiftSummary(user);
                }
            };
            task.setOnSucceeded(openedEvent -> {
                shiftSummaryRef.set(task.getValue());
                context.toastService().showSuccess("Shift opened");
                updateShiftCard(
                    context.support(),
                    shiftSummaryRef.get(),
                    shiftStatusLabel,
                    shiftOpenedLabel,
                    shiftOpeningCashLabel,
                    shiftSalesLabel,
                    shiftRefundsLabel,
                    shiftExpensesLabel,
                    shiftExpectedCashLabel,
                    openShiftButton,
                    closeShiftButton
                );
                openShiftButton.setText("Open Shift");
                closeShiftButton.setText("Close Shift");
                openShiftButton.setDisable(false);
                closeShiftButton.setDisable(false);
                refreshCartRef[0].run();
            });
            task.setOnFailed(openedEvent -> {
                openShiftButton.setText("Open Shift");
                closeShiftButton.setText("Close Shift");
                openShiftButton.setDisable(false);
                closeShiftButton.setDisable(false);
                updateShiftCard(
                    context.support(),
                    shiftSummaryRef.get(),
                    shiftStatusLabel,
                    shiftOpenedLabel,
                    shiftOpeningCashLabel,
                    shiftSalesLabel,
                    shiftRefundsLabel,
                    shiftExpensesLabel,
                    shiftExpectedCashLabel,
                    openShiftButton,
                    closeShiftButton
                );
                refreshCartRef[0].run();
                context.showUserFacingError(task.getException());
            });
            UiTaskExecutor.execute(task, "pos-open-shift");
        });

        closeShiftButton.setOnAction(event -> {
            SalesShiftService.ShiftSummary currentSummary = shiftSummaryRef.get();
            Optional<ShiftCashInput> input = showShiftCashDialog(
                stage,
                "Close Shift",
                "Actual closing cash",
                currentSummary != null ? currentSummary.expectedCashAmount() : MoneySupport.ZERO,
                false,
                "Close note"
            );
            if (input.isEmpty()) {
                return;
            }
            showPendingShiftState(shiftStatusLabel, openShiftButton, closeShiftButton, checkoutButton, "CLOSING...", "Closing...");
            javafx.concurrent.Task<SalesShiftService.ShiftSummary> task = new javafx.concurrent.Task<>() {
                @Override
                protected SalesShiftService.ShiftSummary call() {
                    context.salesShiftService().closeOwnShift(user, input.get().amount(), input.get().note());
                    return context.salesShiftService().getCurrentShiftSummary(user);
                }
            };
            task.setOnSucceeded(closedEvent -> {
                shiftSummaryRef.set(task.getValue());
                context.toastService().showSuccess("Shift closed");
                updateShiftCard(
                    context.support(),
                    shiftSummaryRef.get(),
                    shiftStatusLabel,
                    shiftOpenedLabel,
                    shiftOpeningCashLabel,
                    shiftSalesLabel,
                    shiftRefundsLabel,
                    shiftExpensesLabel,
                    shiftExpectedCashLabel,
                    openShiftButton,
                    closeShiftButton
                );
                openShiftButton.setText("Open Shift");
                closeShiftButton.setText("Close Shift");
                openShiftButton.setDisable(false);
                closeShiftButton.setDisable(false);
                refreshCartRef[0].run();
            });
            task.setOnFailed(closedEvent -> {
                openShiftButton.setText("Open Shift");
                closeShiftButton.setText("Close Shift");
                openShiftButton.setDisable(false);
                closeShiftButton.setDisable(false);
                updateShiftCard(
                    context.support(),
                    shiftSummaryRef.get(),
                    shiftStatusLabel,
                    shiftOpenedLabel,
                    shiftOpeningCashLabel,
                    shiftSalesLabel,
                    shiftRefundsLabel,
                    shiftExpensesLabel,
                    shiftExpectedCashLabel,
                    openShiftButton,
                    closeShiftButton
                );
                refreshCartRef[0].run();
                context.showUserFacingError(task.getException());
            });
            UiTaskExecutor.execute(task, "pos-close-shift");
        });

        renderCartTabsRef[0] = () -> {
            cartTabBar.getChildren().clear();
            PosCartViewModel activeSession = activeCartSessionRef.get();
            for (PosCartViewModel session : cartSessions) {
                Button tabButton = new Button(session.title() + " · " + session.totalItems());
                tabButton.getStyleClass().add("pos-cart-tab");
                if (session == activeSession) {
                    tabButton.getStyleClass().add("pos-cart-tab-active");
                }
                tabButton.setOnAction(event -> {
                    activeCartSessionRef.set(session);
                    renderCartTabsRef[0].run();
                    refreshCartRef[0].run();
                    renderProductsRef[0].run();
                });

                HBox tabWrapper = new HBox(4, tabButton);
                tabWrapper.getStyleClass().add("pos-cart-tab-wrapper");
                tabWrapper.setAlignment(Pos.CENTER_LEFT);

                if (cartSessions.size() > 1) {
                    Button closeButton = new Button("×");
                    closeButton.getStyleClass().add("pos-cart-tab-close");
                    closeButton.setOnAction(event -> {
                        boolean hasUnsavedCart = session.hasUnsavedCart();
                        if (hasUnsavedCart && !DialogSupport.showConfirm(
                            stage,
                            "Close Order",
                            "This cart has unsaved items. Close it anyway?"
                        )) {
                            return;
                        }
                        cartSessions.remove(session);
                        if (activeCartSessionRef.get() == session) {
                            activeCartSessionRef.set(cartSessions.get(0));
                        }
                        renderCartTabsRef[0].run();
                        refreshCartRef[0].run();
                        renderProductsRef[0].run();
                    });
                    tabWrapper.getChildren().add(closeButton);
                }

                cartTabBar.getChildren().add(tabWrapper);
            }

            Button addTabButton = new Button();
            addTabButton.getStyleClass().add("pos-cart-tab-add");
            addTabButton.setGraphic(createPosCartTabAddIcon());
            addTabButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            addTabButton.setFocusTraversable(false);
            addTabButton.setOnAction(event -> {
                PosCartViewModel nextSession = new PosCartViewModel(nextCartSessionNumber[0]++);
                cartSessions.add(nextSession);
                activeCartSessionRef.set(nextSession);
                renderCartTabsRef[0].run();
                refreshCartRef[0].run();
                renderProductsRef[0].run();
            });
            cartTabBar.getChildren().add(addTabButton);
        };

        renderCategoryStripRef[0] = () -> {
            categoryStrip.getChildren().clear();

            Button allChip = createPosCategoryChipButton("All", selectedCategory[0] == null, () -> {
                if (selectedCategory[0] == null) {
                    return;
                }
                selectedCategory[0] = null;
                renderCategoryStripRef[0].run();
                renderProductsRef[0].run();
            });
            categoryStrip.getChildren().add(allChip);

            for (Category category : categories) {
                Button chip = createPosCategoryChipButton(
                    category.getName(),
                    selectedCategory[0] != null && java.util.Objects.equals(selectedCategory[0].getId(), category.getId()),
                    () -> {
                        if (selectedCategory[0] != null && java.util.Objects.equals(selectedCategory[0].getId(), category.getId())) {
                            return;
                        }
                        selectedCategory[0] = category;
                        renderCategoryStripRef[0].run();
                        renderProductsRef[0].run();
                    }
                );
                categoryStrip.getChildren().add(chip);
            }
        };

        java.util.function.Function<Product, Boolean> addProductToActiveCart = product -> {
            if (product == null) {
                return false;
            }
            if (product.getQuantity() == null || product.getQuantity() <= 0) {
                context.toastService().showWarning("Product out of stock");
                return false;
            }
            java.util.Optional<CreateOrderRequest.OrderItemRequest> existing = activeCartSessionRef.get().items().stream()
                .filter(item -> java.util.Objects.equals(item.getProductId(), product.getId()))
                .findFirst();
            if (existing.isPresent()) {
                existing.get().setQuantity(existing.get().getQuantity() + 1);
            } else {
                CreateOrderRequest.OrderItemRequest item =
                    new CreateOrderRequest.OrderItemRequest();
                item.setProductId(product.getId());
                item.setQuantity(1);
                activeCartSessionRef.get().items().add(item);
            }
            refreshCartRef[0].run();
            renderProductsRef[0].run();
            return true;
        };

        renderProductsRef[0] = () -> {
            productGrid.getChildren().clear();
            String query = normalizeProductLookup(searchField.getText());
            java.util.Map<Long, Integer> cartQuantityByProductId = new java.util.LinkedHashMap<>();
            for (CreateOrderRequest.OrderItemRequest cartItem : activeCartSessionRef.get().items()) {
                if (cartItem == null || cartItem.getProductId() == null) {
                    continue;
                }
                cartQuantityByProductId.merge(cartItem.getProductId(), cartItem.getQuantity(), Integer::sum);
            }

            int rendered = 0;
            for (Product product : allProducts) {
                boolean matchesQuery = query.isBlank() || matchesPosProductSearch(product, query);
                boolean matchesCategory = selectedCategory[0] == null
                    || (product.getCategory() != null && java.util.Objects.equals(product.getCategory().getId(), selectedCategory[0].getId()));
                if (!matchesQuery || !matchesCategory) {
                    continue;
                }

                PromotionService.ProductPricingPreview preview = pricingByProductIdRef[0].getOrDefault(
                    product.getId(),
                    new PromotionService.ProductPricingPreview(
                        product,
                        null,
                        MoneySupport.normalize(product.getPrice()),
                        MoneySupport.normalize(product.getPrice()),
                        MoneySupport.ZERO
                    )
                );
                int inCartQty = cartQuantityByProductId.getOrDefault(product.getId(), 0);
                PosProductCardView cardView = product.getId() != null
                    ? productCardCache.get(product.getId())
                    : null;
                if (cardView == null) {
                    cardView = createProductCard(product, preview, inCartQty, cardWidthProp, context.support()::formatVnd, () -> {
                        addProductToActiveCart.apply(product);
                    });
                    if (product.getId() != null) {
                        productCardCache.put(product.getId(), cardView);
                    }
                } else {
                    cardView.setInCartQuantity(inCartQty);
                }
                productGrid.getChildren().add(cardView.card());
                rendered++;
            }

            if (rendered == 0) {
                VBox emptyState = new VBox(4);
                emptyState.getStyleClass().add("pos-empty-state");
                emptyState.setAlignment(Pos.CENTER_LEFT);
                Label emptyTitle = new Label("No products match this view");
                emptyTitle.getStyleClass().add("pos-empty-title");
                emptyState.getChildren().add(emptyTitle);
                productGrid.getChildren().add(emptyState);
            }
        };

        refreshCartRef[0] = () -> {
            PosCartViewModel activeSession = activeCartSessionRef.get();
            cartItemsBox.getChildren().clear();
            updateCustomerCard(
                customerCard,
                selectedCustomerNameLabel,
                selectedCustomerPhoneLabel,
                customerStateBadge,
                clearCustomerButton,
                activeSession.customer()
            );

            int totalItems = 0;
            for (CreateOrderRequest.OrderItemRequest item : activeSession.items()) {
                if (item != null) {
                    totalItems += Math.max(0, item.getQuantity());
                }
            }
            cartMetaLabel.setText(totalItems == 1 ? java.text.MessageFormat.format("{0} item", totalItems) : java.text.MessageFormat.format("{0} items", totalItems));

            if (activeSession.items().isEmpty()) {
                VBox emptyState = new VBox(4);
                emptyState.getStyleClass().add("pos-cart-empty");
                emptyState.setAlignment(Pos.CENTER_LEFT);
                Label emptyTitle = new Label("Cart is empty");
                emptyTitle.getStyleClass().add("pos-empty-title");
                emptyState.getChildren().add(emptyTitle);
                cartItemsBox.getChildren().add(emptyState);
            } else {
                java.util.Map<Long, Product> productsById = new java.util.LinkedHashMap<>();
                for (Product product : allProducts) {
                    if (product != null && product.getId() != null) {
                        productsById.put(product.getId(), product);
                    }
                }
                for (CreateOrderRequest.OrderItemRequest item : activeSession.items()) {
                    if (item == null || item.getProductId() == null) {
                        continue;
                    }
                    Product product = productsById.get(item.getProductId());
                    if (product == null) {
                        continue;
                    }
                    PromotionService.ProductPricingPreview preview = pricingByProductIdRef[0].getOrDefault(
                        product.getId(),
                        new PromotionService.ProductPricingPreview(
                            product,
                            null,
                            MoneySupport.normalize(product.getPrice()),
                            MoneySupport.normalize(product.getPrice()),
                            MoneySupport.ZERO
                        )
                    );

                    VBox row = createPosCartRow(
                        product,
                        item,
                        preview,
                        () -> {
                            activeSession.items().remove(item);
                            refreshCartRef[0].run();
                            renderProductsRef[0].run();
                        },
                        nextQty -> {
                            item.setQuantity(nextQty);
                            refreshCartRef[0].run();
                            renderProductsRef[0].run();
                        }
                    );
                    cartItemsBox.getChildren().add(row);
                }
            }

            subtotalValueLabel.setText(context.support().formatVnd(calculatePosCartSubtotal(activeSession.items(), allProducts, pricingByProductIdRef[0])));
            checkoutButton.setDisable(activeSession.items().isEmpty() || shiftSummaryRef.get() == null);
            renderCartTabsRef[0].run();
        };

        refreshProductCatalogRef[0] = () -> {
            if (!productCatalogRefreshInFlight.compareAndSet(false, true)) {
                productCatalogRefreshAgain.set(true);
                return;
            }
            javafx.concurrent.Task<ProductCatalogSnapshot> task = new javafx.concurrent.Task<>() {
                @Override
                protected ProductCatalogSnapshot call() {
                    java.util.List<Product> refreshedProducts = new java.util.ArrayList<>(context.productService().getAllProducts());
                    java.util.Map<Long, PromotionService.ProductPricingPreview> refreshedPricing =
                        context.promotionService().previewBestProductPricing(refreshedProducts, java.time.LocalDateTime.now());
                    java.util.List<Category> refreshedCategories = new java.util.ArrayList<>(context.categoryRepository().findAll());
                    return new ProductCatalogSnapshot(refreshedProducts, refreshedCategories, refreshedPricing);
                }
            };
            Runnable finishRefresh = () -> {
                productCatalogRefreshInFlight.set(false);
                if (productCatalogRefreshAgain.getAndSet(false)) {
                    refreshProductCatalogRef[0].run();
                }
            };
            task.setOnSucceeded(done -> {
                try {
                    ProductCatalogSnapshot snapshot = task.getValue();
                    allProducts.clear();
                    allProducts.addAll(snapshot.products());
                    categories.clear();
                    categories.addAll(snapshot.categories());
                    if (selectedCategory[0] != null && selectedCategory[0].getId() != null) {
                        Long selectedCategoryId = selectedCategory[0].getId();
                        selectedCategory[0] = categories.stream()
                            .filter(category -> category != null && java.util.Objects.equals(category.getId(), selectedCategoryId))
                            .findFirst()
                            .orElse(null);
                    }
                    productCardCache.clear();
                    pricingByProductIdRef[0] = snapshot.pricingByProductId();
                    renderCategoryStripRef[0].run();
                    renderProductsRef[0].run();
                    refreshCartRef[0].run();
                } finally {
                    finishRefresh.run();
                }
            });
            task.setOnFailed(done -> finishRefresh.run());
            UiTaskExecutor.execute(task, "pos-product-catalog-sync");
        };

        searchField.textProperty().addListener((obs, oldValue, newValue) -> renderProductsRef[0].run());
        searchField.setOnAction(event -> {
            String query = normalizeProductLookup(searchField.getText());
            if (query.isBlank()) {
                return;
            }
            Optional<Product> scannedProduct = resolvePosScannedProduct(allProducts, query);
            if (scannedProduct.isPresent()) {
                if (addProductToActiveCart.apply(scannedProduct.get())) {
                    searchField.clear();
                }
                searchField.requestFocus();
                return;
            }
            long visibleMatches = allProducts.stream()
                .filter(product -> matchesPosProductSearch(product, query))
                .filter(product -> selectedCategory[0] == null
                    || (product.getCategory() != null && java.util.Objects.equals(product.getCategory().getId(), selectedCategory[0].getId())))
                .limit(2)
                .count();
            context.toastService().showWarning(visibleMatches > 0
                ? "Multiple products match; select one manually"
                : "No product found");
            searchField.requestFocus();
        });

        checkoutButton.setOnAction(e -> {
            PosCartViewModel checkoutSession = activeCartSessionRef.get();
            if (checkoutSession.items().isEmpty()) {
                return;
            }

            BigDecimal subtotal = calculatePosCartSubtotal(checkoutSession.items(), allProducts, pricingByProductIdRef[0]);
            java.util.List<PromotionService.OrderPromotionPreview> orderPromotions =
                context.promotionService().getEligibleOrderPromotionPreviews(subtotal, java.time.LocalDateTime.now());

            showCheckoutDialog(stage, subtotal, orderPromotions, selection -> {
                try {
                    CreateOrderRequest req = new CreateOrderRequest();
                    req.setUserId(user.getId());
                    req.setCustomerId(checkoutSession.customer() != null ? checkoutSession.customer().getId() : null);
                    req.setItems(new java.util.ArrayList<>(checkoutSession.items()));
                    req.setPaymentMethod(selection.paymentMethod());
                    req.setSelectedOrderPromotionId(selection.selectedOrderPromotionId());

                    java.util.function.Consumer<CheckoutTaskResult> applyCheckoutResult = result -> {
                        if (result.receiptError() != null && !result.receiptError().isBlank()) {
                            context.toastService().showWarning(java.text.MessageFormat.format(
                                "Order paid, but receipt PDF failed: {0}",
                                result.receiptError()
                            ));
                        }
                        context.toastService().showSuccess(java.text.MessageFormat.format(
                            "Order paid by {0}.",
                            context.support().formatPaymentMethodLabel(selection.paymentMethod())
                        ));
                        checkoutSession.items().clear();
                        checkoutSession.setCustomer(null);
                        updateCustomerCard(customerCard, selectedCustomerNameLabel, selectedCustomerPhoneLabel, customerStateBadge, clearCustomerButton, null);
                        allProducts.clear();
                        allProducts.addAll(result.products());
                        productCardCache.clear();
                        pricingByProductIdRef[0] = result.pricingByProductId();
                        shiftSummaryRef.set(result.shiftSummary());
                updateShiftCard(
                    context.support(),
                    shiftSummaryRef.get(),
                            shiftStatusLabel,
                            shiftOpenedLabel,
                            shiftOpeningCashLabel,
                            shiftSalesLabel,
                            shiftRefundsLabel,
                            shiftExpensesLabel,
                            shiftExpectedCashLabel,
                            openShiftButton,
                            closeShiftButton
                        );
                        checkoutButton.setText("Proceed to Checkout");
                        renderCategoryStripRef[0].run();
                        renderProductsRef[0].run();
                        refreshCartRef[0].run();
                    };

                    if (selection.paymentMethod() == PaymentMethod.QR) {
                        QrPaymentCreateRequest qrRequest = buildQrPaymentCreateRequest(req, selection.amountDue());
                        checkoutButton.setDisable(true);
                        checkoutButton.setText("Generating QR...");
                        javafx.concurrent.Task<QrPaymentStatusDto> task = new javafx.concurrent.Task<>() {
                            @Override
                            protected QrPaymentStatusDto call() {
                                return context.qrPaymentService().createPayment(qrRequest);
                            }
                        };
                        task.setOnSucceeded(done -> {
                            checkoutButton.setText("Proceed to Checkout");
                            refreshCartRef[0].run();
                            QrPaymentDialog.show(
                                stage,
                                task.getValue(),
                                context.qrPaymentService(),
                                paidStatus -> {
                                    checkoutButton.setDisable(true);
                                    checkoutButton.setText("Finalizing...");
                                    javafx.concurrent.Task<CheckoutTaskResult> finalizeTask = new javafx.concurrent.Task<>() {
                                        @Override
                                        protected CheckoutTaskResult call() {
                                            Order paidOrder = context.qrPaymentService().finalizePaidPayment(paidStatus.id());
                                            return buildCheckoutTaskResult(paidOrder, selection.printReceipt(), user);
                                        }
                                    };
                                    finalizeTask.setOnSucceeded(finalized -> applyCheckoutResult.accept(finalizeTask.getValue()));
                                    finalizeTask.setOnFailed(finalized -> {
                                        checkoutButton.setText("Proceed to Checkout");
                                        refreshCartRef[0].run();
                                        Throwable ex = finalizeTask.getException();
                                        context.toastService().showError(java.text.MessageFormat.format(
                                            "QR payment received, but order failed: {0}",
                                            ex != null && ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                                        ));
                                    });
                                    UiTaskExecutor.execute(finalizeTask, "pos-qr-payment-finalize");
                                },
                                error -> context.toastService().showError(java.text.MessageFormat.format(
                                    "QR payment status failed: {0}",
                                    error != null && error.getMessage() != null ? error.getMessage() : "Unknown error"
                                ))
                            );
                        });
                        task.setOnFailed(done -> {
                            checkoutButton.setText("Proceed to Checkout");
                            refreshCartRef[0].run();
                            Throwable ex = task.getException();
                            context.toastService().showError(java.text.MessageFormat.format(
                                "QR payment failed: {0}",
                                ex != null && ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                            ));
                        });
                        UiTaskExecutor.execute(task, "pos-qr-payment-create");
                        return;
                    }

                    checkoutButton.setDisable(true);
                    checkoutButton.setText("Processing...");
                    javafx.concurrent.Task<CheckoutTaskResult> task = new javafx.concurrent.Task<>() {
                        @Override
                        protected CheckoutTaskResult call() {
                            Order newOrder = context.orderService().createOrder(req);
                            return buildCheckoutTaskResult(newOrder, selection.printReceipt(), user);
                        }
                    };
                    task.setOnSucceeded(done -> applyCheckoutResult.accept(task.getValue()));
                    task.setOnFailed(done -> {
                        checkoutButton.setText("Proceed to Checkout");
                        refreshCartRef[0].run();
                        Throwable ex = task.getException();
                        context.toastService().showError(java.text.MessageFormat.format(
                            "Order failed: {0}",
                            ex != null && ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                        ));
                    });
                    UiTaskExecutor.execute(task, "pos-checkout");
                } catch (Exception ex) {
                    checkoutButton.setText("Proceed to Checkout");
                    refreshCartRef[0].run();
                    context.toastService().showError(java.text.MessageFormat.format("Order failed: {0}", ex.getMessage()));
                }
            });
        });

        renderCategoryStripRef[0].run();
        renderProductsRef[0].run();
        refreshShiftRef[0].run();
        refreshCartRef[0].run();

        RealtimeDataSync.installProductInventoryRefresh(
            splitPane,
            context.realtimeDataSyncService(),
            refreshProductCatalogRef[0]
        );
        splitPane.getItems().addAll(browserPane, cartPane);
        return splitPane;
    }

    private Optional<ShiftCashInput> showShiftCashDialog(
        Stage owner,
        String title,
        String amountLabel,
        BigDecimal defaultAmount,
        boolean requireNote,
        String notePrompt
    ) {
        Dialog<ShiftCashInput> dialog = new Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("pos-shift-dialog");

        ButtonType confirmType = new ButtonType(title, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);

        Label heading = new Label(title);
        heading.getStyleClass().add("pos-shift-dialog-title");

        Label amountCaption = new Label(amountLabel);
        amountCaption.getStyleClass().add("pos-shift-dialog-label");
        TextField amountField = new TextField(formatPlainMoney(defaultAmount));
        amountField.getStyleClass().add("pos-shift-dialog-money-field");
        amountField.setPromptText("0 VND");
        amountField.setMaxWidth(Double.MAX_VALUE);

        Label noteCaption = new Label(requireNote ? notePrompt + " *" : notePrompt);
        noteCaption.getStyleClass().add("pos-shift-dialog-label");
        TextField noteField = new TextField();
        noteField.getStyleClass().add("pos-shift-dialog-field");
        noteField.setPromptText(notePrompt);
        noteField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label("");
        errorLabel.getStyleClass().add("pos-shift-dialog-error");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        VBox amountGroup = new VBox(6, amountCaption, amountField);
        VBox noteGroup = new VBox(6, noteCaption, noteField);
        VBox content = new VBox(14, heading, amountGroup, noteGroup, errorLabel);
        content.getStyleClass().add("pos-shift-dialog-content");
        content.setPadding(new Insets(4, 2, 0, 2));
        content.setFillWidth(true);
        dialog.getDialogPane().setContent(content);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmType);
        confirmButton.getStyleClass().addAll("button", "primary-button", "product-dialog-primary-button", "no-hover-button");
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().addAll("button", "product-dialog-secondary-button", "dialog-cancel-button", "no-hover-button");
        Runnable validate = () -> {
            try {
                BigDecimal amount = parseMoneyInput(amountField.getText());
                boolean invalidNote = requireNote && (noteField.getText() == null || noteField.getText().trim().isEmpty());
                confirmButton.setDisable(amount.signum() < 0 || invalidNote);
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
            } catch (IllegalArgumentException ex) {
                confirmButton.setDisable(true);
                errorLabel.setText(ex.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        };
        amountField.textProperty().addListener((obs, oldValue, newValue) -> validate.run());
        noteField.textProperty().addListener((obs, oldValue, newValue) -> validate.run());
        validate.run();

        dialog.setOnShown(event -> {
            amountField.requestFocus();
            amountField.selectAll();
            javafx.application.Platform.runLater(amountField::requestFocus);
        });
        dialog.setResultConverter(buttonType -> {
            if (buttonType != confirmType) {
                return null;
            }
            return new ShiftCashInput(parseMoneyInput(amountField.getText()), noteField.getText());
        });
        return dialog.showAndWait();
    }

    private String formatPlainMoney(BigDecimal amount) {
        return MoneySupport.normalize(amount).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal parseMoneyInput(String input) {
        String normalized = input == null ? "" : input.trim().toUpperCase(java.util.Locale.ROOT).replace("VND", "").replace(" ", "");
        if (normalized.isBlank()) {
            return MoneySupport.ZERO;
        }
        boolean hasComma = normalized.contains(",");
        boolean hasDot = normalized.contains(".");
        if (hasComma && hasDot) {
            normalized = normalized.replace(",", "");
        } else if (hasComma) {
            normalized = normalized.replace(",", "");
        } else if (hasDot) {
            int dotIndex = normalized.lastIndexOf('.');
            if (normalized.length() - dotIndex - 1 == 3) {
                normalized = normalized.replace(".", "");
            }
        }
        try {
            return MoneySupport.normalize(new BigDecimal(normalized));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a valid cash amount");
        }
    }

    private Node createPosCartTabAddIcon() {
        javafx.scene.shape.SVGPath plusPath = new javafx.scene.shape.SVGPath();
        plusPath.setContent("M12 7v10M7 12h10");
        plusPath.setStroke(PRIMARY_COLOR);
        plusPath.setStrokeWidth(1.6);
        plusPath.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        plusPath.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        plusPath.setFill(javafx.scene.paint.Color.TRANSPARENT);
        plusPath.setSmooth(true);

        javafx.scene.Group iconGroup = new javafx.scene.Group(plusPath);
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(iconGroup);
        iconPane.setAlignment(Pos.CENTER);
        iconPane.setMinSize(18, 18);
        iconPane.setPrefSize(18, 18);
        iconPane.setMaxSize(18, 18);
        iconPane.setMouseTransparent(true);
        return iconPane;
    }


    private Button createPosCategoryChipButton(String labelText, boolean active, Runnable onClick) {
        Button chip = new Button(labelText);
        chip.getStyleClass().add("pos-category-chip");
        if (active) {
            chip.getStyleClass().add("pos-category-chip-active");
        }
        chip.setOnAction(e -> {
            if (onClick != null) {
                onClick.run();
            }
        });
        return chip;
    }


    private VBox createPosCartRow(
        Product product,
        CreateOrderRequest.OrderItemRequest item,
        PromotionService.ProductPricingPreview pricingPreview,
        Runnable onRemove,
        java.util.function.IntConsumer onQuantityChanged
    ) {
        VBox row = new VBox(10);
        row.getStyleClass().add("pos-cart-row");

        Label productName = new Label(product.getName());
        productName.getStyleClass().add("pos-cart-row-name");
        productName.setWrapText(true);

        BigDecimal unitPrice = pricingPreview != null ? pricingPreview.discountedUnitPrice() : MoneySupport.normalize(product.getPrice());
        Label unitPriceLabel = new Label(java.text.MessageFormat.format("Unit {0}", context.support().formatVnd(unitPrice)));
        unitPriceLabel.getStyleClass().add("pos-cart-row-price");

        VBox metaBox = new VBox(4, productName, unitPriceLabel);
        metaBox.setAlignment(Pos.TOP_LEFT);
        metaBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(metaBox, Priority.ALWAYS);

        Button removeButton = new Button();
        removeButton.getStyleClass().add("pos-remove-button");
        removeButton.setGraphic(createPosRemoveIcon());
        removeButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        removeButton.setOnAction(e -> {
            if (onRemove != null) {
                onRemove.run();
            }
        });

        HBox topRow = new HBox(10, metaBox, removeButton);
        topRow.setAlignment(Pos.TOP_LEFT);

        int maxStepperValue = Math.max(item.getQuantity(), Math.max(1, product.getQuantity()));
        com.pbl3.project.pbl3_project.ui.component.QuantityStepper stepper =
            new com.pbl3.project.pbl3_project.ui.component.QuantityStepper(1, maxStepperValue, null);
        stepper.getStyleClass().add("pos-cart-stepper");
        stepper.setValue(item.getQuantity(), false, null);
        stepper.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (onQuantityChanged != null && oldValue.intValue() != newValue.intValue()) {
                onQuantityChanged.accept(newValue.intValue());
            }
        });

        Label lineSubtotalLabel = new Label(context.support().formatVnd(MoneySupport.multiply(unitPrice, item.getQuantity())));
        lineSubtotalLabel.getStyleClass().add("pos-cart-line-total");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottomRow = new HBox(10, stepper, spacer, lineSubtotalLabel);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(topRow, bottomRow);
        return row;
    }


    private Node createPosRemoveIcon() {
        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
        icon.setContent("M6 6l12 12M18 6 6 18");
        icon.setStroke(TEXT_MUTED_COLOR);
        icon.setStrokeWidth(1.8);
        icon.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        icon.setFill(javafx.scene.paint.Color.TRANSPARENT);
        return new javafx.scene.layout.StackPane(icon);
    }


    private void showCheckoutDialog(
        Stage owner,
        BigDecimal subtotalAmount,
        java.util.List<PromotionService.OrderPromotionPreview> eligibleOrderPromotions,
        java.util.function.Consumer<CheckoutSelection> onConfirm
    ) {
        com.pbl3.project.pbl3_project.ui.dialog.CheckoutDialog.show(
            owner,
            subtotalAmount,
            eligibleOrderPromotions,
            () -> {
                com.pbl3.project.pbl3_project.service.SePaySettingsService.CheckoutAvailability availability =
                    context.sePaySettingsService().checkCheckoutAvailability();
                return new com.pbl3.project.pbl3_project.ui.dialog.CheckoutDialog.QrPaymentAvailability(
                    availability.available(),
                    availability.message()
                );
            },
            selection -> onConfirm.accept(new CheckoutSelection(
                selection.paymentMethod(),
                selection.printReceipt(),
                selection.selectedOrderPromotionId(),
                selection.amountDue()
            ))
        );
    }

    private QrPaymentCreateRequest buildQrPaymentCreateRequest(CreateOrderRequest orderRequest, BigDecimal amountDue) {
        QrPaymentCreateRequest qrRequest = new QrPaymentCreateRequest();
        qrRequest.setUserId(orderRequest.getUserId());
        qrRequest.setCustomerId(orderRequest.getCustomerId());
        qrRequest.setSelectedOrderPromotionId(orderRequest.getSelectedOrderPromotionId());
        qrRequest.setAmount(amountDue);
        qrRequest.setItems(new java.util.ArrayList<>(orderRequest.getItems()));
        return qrRequest;
    }

    private CheckoutTaskResult buildCheckoutTaskResult(Order order, boolean printReceipt, User user) {
        String receiptError = null;
        if (printReceipt) {
            try {
                context.receiptService().generateAndOpenReceipt(order);
            } catch (ReceiptService.ReceiptGenerationException receiptEx) {
                receiptError = receiptEx.getMessage();
            }
        }
        java.util.List<Product> refreshedProducts = new java.util.ArrayList<>(context.productService().getAllProducts());
        java.util.Map<Long, PromotionService.ProductPricingPreview> refreshedPricing =
            context.promotionService().previewBestProductPricing(refreshedProducts, java.time.LocalDateTime.now());
        SalesShiftService.ShiftSummary refreshedShift = context.salesShiftService().getCurrentShiftSummary(user);
        return new CheckoutTaskResult(order, receiptError, refreshedProducts, refreshedPricing, refreshedShift);
    }


    private Customer showCustomerPickerDialog(
        Stage owner,
        User actor
    ) {
        return com.pbl3.project.pbl3_project.ui.dialog.CustomerDialog.showPicker(
            owner,
            actor,
            customerDialogContext()
        );
    }


    private com.pbl3.project.pbl3_project.ui.dialog.CustomerDialog.Context customerDialogContext() {
        return new com.pbl3.project.pbl3_project.ui.dialog.CustomerDialog.Context(
            context.customerService(),
            context.orderService(),
            context.toastService(),
            context::showUserFacingError,
            context.support()::showOrderDetailsDialog
        );
    }


}
