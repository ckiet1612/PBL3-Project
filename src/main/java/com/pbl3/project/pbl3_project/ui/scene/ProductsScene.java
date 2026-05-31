package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.Category;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.ProductService;
import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.component.FilterControlFactory;
import com.pbl3.project.pbl3_project.ui.component.FilterDisclosureSection;
import com.pbl3.project.pbl3_project.ui.component.RangeSlider;
import com.pbl3.project.pbl3_project.ui.component.StatusBadgeFactory;
import com.pbl3.project.pbl3_project.ui.dialog.ProductDialog;
import com.pbl3.project.pbl3_project.ui.scene.model.ImportOrderPrefill;
import com.pbl3.project.pbl3_project.ui.scene.model.ProductViewPreset;
import com.pbl3.project.pbl3_project.ui.util.AsyncPageCache;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.RealtimeDataSync;
import com.pbl3.project.pbl3_project.ui.util.SortCriterion;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class ProductsScene {
    private static final Color PRIMARY_COLOR = Color.web("#1d7df2");
    private static final double PRODUCT_FILTER_BUTTON_SIZE = 42;
    private static final double PRODUCT_FILTER_POPUP_WIDTH = 360;
    private static final double PRODUCT_FILTER_POPUP_VIEWPORT_HEIGHT = 330;
    private static final double PRODUCT_FILTER_POPUP_X_OFFSET = PRODUCT_FILTER_BUTTON_SIZE - PRODUCT_FILTER_POPUP_WIDTH;
    private static final double PRODUCT_FILTER_POPUP_Y_OFFSET = 5;
    private static final double PRODUCT_FILTER_SLIDER_WIDTH = 290;

    private enum ProductCatalogScope {
        ALL,
        CATEGORY,
        LOW_STOCK
    }

    private record ProductFilterOptionsCache(Long categoryId, boolean lowStockOnly, ProductService.CatalogFilterOptions options) {
        private boolean matches(Long categoryId, boolean lowStockOnly) {
            return Objects.equals(this.categoryId, categoryId) && this.lowStockOnly == lowStockOnly;
        }
    }

    private record ProductPageResult(org.springframework.data.domain.Page<Product> page, int pageIndex) {
    }

    public record Options(ProductViewPreset preset) {
    }

    private ProductsScene() {
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        Stage stage = context.owner();
        ProductViewPreset initialProductViewPreset = options == null ? null : options.preset();
        var productService = context.productService();
        var categoryRepository = context.categoryRepository();
        var authorizationService = context.authorizationService();
        var toastService = context.toastService();
        java.util.List<com.pbl3.project.pbl3_project.entity.Category> categories =
            new java.util.ArrayList<>(categoryRepository.findAll());

        ProductCatalogScope[] activeScope = {
            initialProductViewPreset == ProductViewPreset.LOW_STOCK ? ProductCatalogScope.LOW_STOCK : ProductCatalogScope.ALL
        };
        final com.pbl3.project.pbl3_project.entity.Category[] selectedCategory = {null};
        final int productPageSize = 20;
        final int[] productCurrentPage = {0};
        final int[] productTotalPages = {0};
        final long[] productTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> productSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> productBrandsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<BigDecimal> productMinPriceRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<BigDecimal> productMaxPriceRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<Integer> productMinQtyRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<Integer> productMaxQtyRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<ProductFilterOptionsCache> productFilterOptionsCache = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicLong productPageLoadVersion = new java.util.concurrent.atomic.AtomicLong();
        AsyncPageCache<ProductPageResult> productPageCache = new AsyncPageCache<>(80);

        final String productSortStateKey = "products";
        TableSortState productSortState = context.support().getOrCreateTableSortState(
            productSortStateKey,
            new SortCriterion("name", javafx.scene.control.TableColumn.SortType.ASCENDING)
        );
        java.util.LinkedHashMap<String, String> productSortProperties = new java.util.LinkedHashMap<>();
        productSortProperties.put("sku", "sku");
        productSortProperties.put("name", "name");
        productSortProperties.put("category", "category.name");
        productSortProperties.put("price", "price");
        productSortProperties.put("quantity", "quantity");

        VBox root = new VBox(14);
        root.getStyleClass().add("products-workspace");
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        Button addButton = ButtonFactory.expandableGreenAction("Add Product", 150);
        addButton.setVisible(activeScope[0] != ProductCatalogScope.LOW_STOCK);
        addButton.setManaged(activeScope[0] != ProductCatalogScope.LOW_STOCK);

        javafx.scene.layout.BorderPane pageHeader = new javafx.scene.layout.BorderPane();
        pageHeader.getStyleClass().add("products-page-header");
        pageHeader.setRight(addButton);
        pageHeader.visibleProperty().bind(addButton.visibleProperty());
        pageHeader.managedProperty().bind(addButton.managedProperty());
        javafx.scene.layout.BorderPane.setAlignment(addButton, Pos.CENTER_RIGHT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search SKU, product, barcode, brand");
        searchField.getStyleClass().add("product-search-field");
        HBox searchBox = createProductSearchBox(searchField);

        Button filterButton = new Button();
        filterButton.getStyleClass().add("product-filter-button");
        filterButton.setCursor(javafx.scene.Cursor.HAND);
        filterButton.setTooltip(new javafx.scene.control.Tooltip("Filter"));
        filterButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        javafx.scene.shape.SVGPath filterIcon = new javafx.scene.shape.SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.setStyle("-fx-fill: -app-primary;");
        filterIcon.setMouseTransparent(true);
        filterButton.setGraphic(filterIcon);

        FlowPane categoryStrip = new FlowPane();
        categoryStrip.getStyleClass().add("product-category-strip");
        categoryStrip.setHgap(10);
        categoryStrip.setVgap(10);
        categoryStrip.setAlignment(Pos.CENTER_LEFT);

        FlowPane activeFilterStrip = new FlowPane();
        activeFilterStrip.getStyleClass().add("product-active-filter-strip");
        activeFilterStrip.setHgap(8);
        activeFilterStrip.setVgap(8);
        activeFilterStrip.setVisible(false);
        activeFilterStrip.setManaged(false);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Product> table = new javafx.scene.control.TableView<>();
        table.getStyleClass().add("product-catalog-table");
        context.support().applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> skuCol = new javafx.scene.control.TableColumn<>("SKU");
        skuCol.setPrefWidth(90);
        skuCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(emptyDash(cell.getValue().getSku())));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> nameCol = new javafx.scene.control.TableColumn<>("Product");
        nameCol.setPrefWidth(190);
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        nameCol.setCellFactory(col -> new javafx.scene.control.TableCell<com.pbl3.project.pbl3_project.entity.Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("product-name-cell");
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    getStyleClass().add("product-name-cell");
                }
            }
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> categoryCol = new javafx.scene.control.TableColumn<>("Category");
        categoryCol.setPrefWidth(120);
        categoryCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
            cell.getValue().getCategory() != null ? cell.getValue().getCategory().getName() : "-"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> priceCol = new javafx.scene.control.TableColumn<>("Price");
        priceCol.setPrefWidth(120);
        priceCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(context.support().formatVnd(cell.getValue().getPrice())));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Integer> qtyCol = new javafx.scene.control.TableColumn<>("Stock");
        qtyCol.setPrefWidth(100);
        qtyCol.setMinWidth(90);
        qtyCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(safeProductInt(cell.getValue().getQuantity())).asObject());

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, com.pbl3.project.pbl3_project.entity.Product> statusCol = new javafx.scene.control.TableColumn<>("Status");
        statusCol.setPrefWidth(120);
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue()));
        statusCol.setCellFactory(col -> new javafx.scene.control.TableCell<com.pbl3.project.pbl3_project.entity.Product, com.pbl3.project.pbl3_project.entity.Product>() {
            @Override
            protected void updateItem(com.pbl3.project.pbl3_project.entity.Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : StatusBadgeFactory.product(item));
            }
        });

        table.getColumns().addAll(skuCol, nameCol, categoryCol, priceCol, qtyCol, statusCol);
        table.getColumns().forEach(column -> column.setResizable(true));
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, ?>> productSortColumns =
            new java.util.LinkedHashMap<>();
        productSortColumns.put("sku", skuCol);
        productSortColumns.put("name", nameCol);
        productSortColumns.put("category", categoryCol);
        productSortColumns.put("price", priceCol);
        productSortColumns.put("quantity", qtyCol);
        context.support().installSortHeaderIndicators(productSortColumns);
        java.util.LinkedHashMap<String, String> productSortLabels = new java.util.LinkedHashMap<>();
        productSortLabels.put("sku", "SKU");
        productSortLabels.put("name", "Product");
        productSortLabels.put("category", "Category");
        productSortLabels.put("price", "Price");
        productSortLabels.put("quantity", "Stock");
        Label productSortStatusLabel = context.support().createSortStatusLabel(productSortState, productSortLabels);

        Label productRowCountLabel = context.support().createStatusMetaLabel(java.text.MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label productPageLabel = context.support().createStatusMetaLabel(java.text.MessageFormat.format("Page {0} / {1}", 0, 0));
        Button productPrevBtn = ButtonFactory.pageNav("Prev");
        Button productNextBtn = ButtonFactory.pageNav("Next");
        javafx.scene.layout.HBox productStatusBar = new javafx.scene.layout.HBox(15, productSortStatusLabel, productRowCountLabel, productPageLabel, productPrevBtn, productNextBtn);
        context.support().applyStandardTableStatusBar(productStatusBar);

        VBox inspectorContent = new VBox(14);
        inspectorContent.getStyleClass().add("product-inspector-card");
        refreshProductInspector(context, inspectorContent, null, stage, user, () -> {});
        javafx.scene.control.ScrollPane inspectorScroll = new javafx.scene.control.ScrollPane(inspectorContent);
        inspectorScroll.getStyleClass().add("product-inspector-scroll");
        inspectorScroll.setFitToWidth(true);
        inspectorScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        inspectorScroll.setMinWidth(320);
        inspectorScroll.setPrefWidth(360);

        Runnable[] refreshProductListRef = new Runnable[1];
        Runnable[] rebuildCategoryChipsRef = new Runnable[1];
        Runnable[] updateActiveFiltersRef = new Runnable[1];
        Runnable[] resetAdvancedFiltersRef = new Runnable[1];

        java.util.function.BooleanSupplier hasAdvancedFilters = () ->
            !productBrandsRef.get().isEmpty()
                || productMinPriceRef.get() != null
                || productMaxPriceRef.get() != null
                || productMinQtyRef.get() != null
                || productMaxQtyRef.get() != null;

        resetAdvancedFiltersRef[0] = () -> {
            productBrandsRef.set(new java.util.LinkedHashSet<>());
            productMinPriceRef.set(null);
            productMaxPriceRef.set(null);
            productMinQtyRef.set(null);
            productMaxQtyRef.set(null);
        };

        updateActiveFiltersRef[0] = () -> {
            activeFilterStrip.getChildren().clear();
            if (activeScope[0] == ProductCatalogScope.LOW_STOCK) {
                activeFilterStrip.getChildren().add(createProductActiveFilterChip("Low Stock View", true));
            } else if (activeScope[0] == ProductCatalogScope.CATEGORY && selectedCategory[0] != null) {
                activeFilterStrip.getChildren().add(createProductActiveFilterChip(java.text.MessageFormat.format("Category: {0}", selectedCategory[0].getName()), false));
            }
            String searchText = productSearchRef.get();
            if (searchText != null && !searchText.isBlank()) {
                activeFilterStrip.getChildren().add(createProductActiveFilterChip(java.text.MessageFormat.format("Search: {0}", searchText.trim()), false));
            }
            if (!productBrandsRef.get().isEmpty()) {
                activeFilterStrip.getChildren().add(createProductActiveFilterChip(java.text.MessageFormat.format("Brands: {0}", productBrandsRef.get().size()), false));
            }
            if (productMinPriceRef.get() != null || productMaxPriceRef.get() != null) {
                String min = productMinPriceRef.get() == null ? "0" : context.support().formatVnd(productMinPriceRef.get());
                String max = productMaxPriceRef.get() == null ? "Max" : context.support().formatVnd(productMaxPriceRef.get());
                activeFilterStrip.getChildren().add(createProductActiveFilterChip(java.text.MessageFormat.format("Price: {0} - {1}", min, max), false));
            }
            if (productMinQtyRef.get() != null || productMaxQtyRef.get() != null) {
                String min = productMinQtyRef.get() == null ? "0" : String.valueOf(productMinQtyRef.get());
                String max = productMaxQtyRef.get() == null ? "Max" : String.valueOf(productMaxQtyRef.get());
                activeFilterStrip.getChildren().add(createProductActiveFilterChip(java.text.MessageFormat.format("Stock: {0} - {1}", min, max), false));
            }
            boolean hasFilters = !activeFilterStrip.getChildren().isEmpty();
            activeFilterStrip.setVisible(hasFilters);
            activeFilterStrip.setManaged(hasFilters);
            filterButton.getStyleClass().remove("active");
            if (hasAdvancedFilters.getAsBoolean()) {
                filterButton.getStyleClass().add("active");
            }
        };

        Runnable updateHeaderState = () -> {
            boolean lowStock = activeScope[0] == ProductCatalogScope.LOW_STOCK;
            addButton.setVisible(!lowStock);
            addButton.setManaged(!lowStock);
        };

        refreshProductListRef[0] = () -> {
            Long categoryId = activeScope[0] == ProductCatalogScope.CATEGORY && selectedCategory[0] != null
                ? selectedCategory[0].getId()
                : null;
            boolean lowStockOnly = activeScope[0] == ProductCatalogScope.LOW_STOCK;
            int requestedPage = productCurrentPage[0];
            String search = productSearchRef.get();
            java.util.Set<String> brands = new java.util.LinkedHashSet<>(productBrandsRef.get());
            BigDecimal minPrice = productMinPriceRef.get();
            BigDecimal maxPrice = productMaxPriceRef.get();
            Integer minQty = productMinQtyRef.get();
            Integer maxQty = productMaxQtyRef.get();
            java.util.List<SortCriterion> sortSnapshot = productSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(
                pageIndex,
                categoryId,
                lowStockOnly,
                search,
                brands,
                minPrice,
                maxPrice,
                minQty,
                maxQty,
                sortSnapshot
            );
            java.util.function.IntFunction<ProductPageResult> fetchProductPage = pageIndex -> {
                int resolvedPage = pageIndex;
                org.springframework.data.domain.Page<Product> pageData =
                    productService.searchProductCatalog(
                        categoryId,
                        lowStockOnly,
                        search,
                        brands,
                        minPrice,
                        maxPrice,
                        minQty,
                        maxQty,
                        context.support().createPageable(sortForLoad, productSortProperties, resolvedPage, productPageSize)
                    );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = productService.searchProductCatalog(
                        categoryId,
                        lowStockOnly,
                        search,
                        brands,
                        minPrice,
                        maxPrice,
                        minQty,
                        maxQty,
                        context.support().createPageable(sortForLoad, productSortProperties, resolvedPage, productPageSize)
                    );
                }
                return new ProductPageResult(pageData, resolvedPage);
            };

            AsyncUiTask.runLatestCachedTableLoad(
                table,
                productPrevBtn,
                productNextBtn,
                productPageLoadVersion,
                productPageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchProductPage.apply(requestedPage),
                result -> {
                    org.springframework.data.domain.Page<Product> pageData = result.page();
                    productCurrentPage[0] = result.pageIndex();
                    productTotalElements[0] = pageData.getTotalElements();
                    productTotalPages[0] = pageData.getTotalPages();
                    table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
                    table.getSelectionModel().clearSelection();
                    context.support().updatePagedStatus(
                        table,
                        productRowCountLabel,
                        productPageLabel,
                        productPrevBtn,
                        productNextBtn,
                        productTotalElements[0],
                        productCurrentPage[0],
                        productTotalPages[0],
                        productPageSize
                    );
                    updateHeaderState.run();
                    updateActiveFiltersRef[0].run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < productTotalPages[0]) {
                        productPageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchProductPage.apply(nextPage),
                            null,
                            "products-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        productPageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchProductPage.apply(previousPage),
                            null,
                            "products-prev-page-prefetch"
                        );
                    }
                },
                context::showUserFacingError,
                "Loading products...",
                "Could not load products",
                "products-page-loader"
            );
        };
        Runnable refreshProductCatalogAfterChange = () -> {
            productFilterOptionsCache.set(null);
            productPageCache.clear();
            refreshProductListRef[0].run();
        };

        rebuildCategoryChipsRef[0] = () -> {
            categoryStrip.getChildren().clear();
            Button allChip = createProductCategoryChipButton("All", activeScope[0] == ProductCatalogScope.ALL, false, () -> {
                if (activeScope[0] == ProductCatalogScope.ALL && selectedCategory[0] == null) {
                    return;
                }
                activeScope[0] = ProductCatalogScope.ALL;
                selectedCategory[0] = null;
                resetAdvancedFiltersRef[0].run();
                productCurrentPage[0] = 0;
                rebuildCategoryChipsRef[0].run();
                refreshProductListRef[0].run();
            });
            categoryStrip.getChildren().add(allChip);

            Button lowStockChip = createProductCategoryChipButton("Low Stock", activeScope[0] == ProductCatalogScope.LOW_STOCK, true, () -> {
                if (activeScope[0] == ProductCatalogScope.LOW_STOCK) {
                    return;
                }
                activeScope[0] = ProductCatalogScope.LOW_STOCK;
                selectedCategory[0] = null;
                resetAdvancedFiltersRef[0].run();
                productCurrentPage[0] = 0;
                rebuildCategoryChipsRef[0].run();
                refreshProductListRef[0].run();
            });
            categoryStrip.getChildren().add(lowStockChip);

            for (com.pbl3.project.pbl3_project.entity.Category category : categories) {
                boolean active = activeScope[0] == ProductCatalogScope.CATEGORY
                    && selectedCategory[0] != null
                    && java.util.Objects.equals(selectedCategory[0].getId(), category.getId());
                Button categoryChip = createProductCategoryChipButton(category.getName(), active, false, () -> {
                    if (activeScope[0] == ProductCatalogScope.CATEGORY
                        && selectedCategory[0] != null
                        && java.util.Objects.equals(selectedCategory[0].getId(), category.getId())) {
                        return;
                    }
                    activeScope[0] = ProductCatalogScope.CATEGORY;
                    selectedCategory[0] = category;
                    resetAdvancedFiltersRef[0].run();
                    productCurrentPage[0] = 0;
                    rebuildCategoryChipsRef[0].run();
                    refreshProductListRef[0].run();
                });
                categoryStrip.getChildren().add(categoryChip);
            }
        };

        Runnable applyProductSortUi = () -> {
            context.support().applySortStateToTable(table, productSortColumns, productSortState);
            productSortStatusLabel.setText(context.support().buildSortStatusText(productSortState, productSortLabels));
        };
        applyProductSortUi.run();
        context.support().installManualServerSorting(
            table,
            productSortColumns,
            productSortState,
            () -> {
                applyProductSortUi.run();
                productCurrentPage[0] = 0;
                refreshProductListRef[0].run();
            }
        );

        productPrevBtn.setOnAction(e -> {
            if (productCurrentPage[0] > 0) {
                productCurrentPage[0]--;
                refreshProductListRef[0].run();
            }
        });
        productNextBtn.setOnAction(e -> {
            if (productCurrentPage[0] + 1 < productTotalPages[0]) {
                productCurrentPage[0]++;
                refreshProductListRef[0].run();
            }
        });

        Runnable deleteAction = () -> {
            if (!authorizationService.canDeleteProducts(user)) {
                toastService.showError("Only admins can delete products.");
                return;
            }
            java.util.List<com.pbl3.project.pbl3_project.entity.Product> selectedItems = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedItems.isEmpty()) return;
            boolean confirmed = selectedItems.size() >= 5
                ? DialogSupport.showTypedDangerConfirm(
                    context.owner(),
                    "Confirm Deletion",
                    MessageFormat.format("Delete {0} selected product(s)? This is a large delete operation.", selectedItems.size()),
                    "DELETE"
                )
                : context.support().showConfirmDialog(
                    "Confirm Deletion",
                    MessageFormat.format("Are you sure you want to delete {0} selected product(s)?", selectedItems.size())
                );
            if (!confirmed) {
                return;
            }
            int deletedCount = 0;
            for (com.pbl3.project.pbl3_project.entity.Product product : selectedItems) {
                try {
                    productService.deleteProduct(product.getId(), user);
                    deletedCount++;
                } catch (Exception ex) {
                    toastService.showError(java.text.MessageFormat.format("Delete failed: {0}", ex.getMessage()));
                }
            }
            if (deletedCount > 0) {
                toastService.showSuccess(java.text.MessageFormat.format("Deleted {0} product(s).", deletedCount));
                refreshProductCatalogAfterChange.run();
            }
        };

        Runnable editSelectedProduct = () -> {
            com.pbl3.project.pbl3_project.entity.Product selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            showProductDialog(context, selected, selected.getCategory(), user, refreshProductCatalogAfterChange);
        };

        table.setOnKeyPressed(event -> {
            javafx.scene.input.KeyCode code = event.getCode();
            if (code == javafx.scene.input.KeyCode.DELETE || code == javafx.scene.input.KeyCode.BACK_SPACE) {
                deleteAction.run();
            } else if (code == javafx.scene.input.KeyCode.ENTER) {
                editSelectedProduct.run();
            }
        });
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Product> row = new javafx.scene.control.TableRow<>() {
                @Override
                protected void updateItem(com.pbl3.project.pbl3_project.entity.Product item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("product-row-low-stock", "product-row-out-of-stock");
                    if (!empty && item != null) {
                        if (isProductOutOfStock(item)) {
                            getStyleClass().add("product-row-out-of-stock");
                        } else if (isProductLowStock(item)) {
                            getStyleClass().add("product-row-low-stock");
                        }
                    }
                }
            };
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
            javafx.scene.control.MenuItem editItem = new javafx.scene.control.MenuItem("Edit");
            editItem.setOnAction(event -> {
                if (row.getItem() != null) {
                    showProductDialog(context, row.getItem(), row.getItem().getCategory(), user, refreshProductCatalogAfterChange);
                }
            });
            contextMenu.getItems().add(editItem);
            if (authorizationService.canDeleteProducts(user)) {
                javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete");
                deleteItem.setOnAction(event -> {
                    if (table.getSelectionModel().getSelectedItems().isEmpty() && row.getItem() != null) {
                        table.getSelectionModel().select(row.getItem());
                    }
                    deleteAction.run();
                });
                contextMenu.getItems().addAll(new javafx.scene.control.SeparatorMenuItem(), deleteItem);
            }
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    showProductDialog(context, row.getItem(), row.getItem().getCategory(), user, refreshProductCatalogAfterChange);
                }
            });
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                    .then((javafx.scene.control.ContextMenu) null)
                    .otherwise(contextMenu)
            );
            return row;
        });

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Product>) c -> {
            context.support().updatePagedStatus(
                table,
                productRowCountLabel,
                productPageLabel,
                productPrevBtn,
                productNextBtn,
                productTotalElements[0],
                productCurrentPage[0],
                productTotalPages[0],
                productPageSize
            );
            javafx.application.Platform.runLater(() -> {
                com.pbl3.project.pbl3_project.entity.Product selected = table.getSelectionModel().getSelectedItem();
                refreshProductInspector(context, inspectorContent, selected, stage, user, refreshProductCatalogAfterChange);
            });
        });

        javafx.animation.PauseTransition productSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        productSearchPause.setOnFinished(e -> {
            productCurrentPage[0] = 0;
            productSearchRef.set(searchField.getText());
            refreshProductListRef[0].run();
        });
        searchField.textProperty().addListener((obs, old, val) -> productSearchPause.playFromStart());

        javafx.stage.Popup filterPopup = new javafx.stage.Popup();
        filterPopup.setAutoHide(true);
        filterButton.setOnAction(event -> {
            event.consume();
            if (filterPopup.isShowing()) {
                filterPopup.hide();
                return;
            }
            Long categoryId = activeScope[0] == ProductCatalogScope.CATEGORY && selectedCategory[0] != null
                ? selectedCategory[0].getId()
                : null;
            boolean lowStockOnly = activeScope[0] == ProductCatalogScope.LOW_STOCK;

            java.util.function.Consumer<ProductService.CatalogFilterOptions> showFilterPopupContent = filterOptions -> {
                BigDecimal maxPriceValue = filterOptions.maxPrice();
                double maxPrice = maxPriceValue == null ? 0.0 : maxPriceValue.doubleValue();
                int maxQty = filterOptions.maxQuantity();
                if (maxPrice <= 0) maxPrice = 1000;
                if (maxQty <= 0) maxQty = 100;

                FilterControlFactory.Shell shell = FilterControlFactory.shell(
                    PRODUCT_FILTER_POPUP_WIDTH,
                    PRODUCT_FILTER_POPUP_VIEWPORT_HEIGHT
                );
                Label brandTitle = FilterControlFactory.sectionTitle("Brands");
                javafx.scene.control.CheckBox allBrandsCb = new javafx.scene.control.CheckBox("All Brands");
                allBrandsCb.setSelected(productBrandsRef.get().isEmpty());
                allBrandsCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary; -fx-cursor: hand;");

                VBox brandCheckboxes = new VBox(6);
                brandCheckboxes.setPadding(new Insets(5, 5, 5, 10));
                java.util.List<javafx.scene.control.CheckBox> brandCbs = new java.util.ArrayList<>();
                boolean[] syncingBrandSelection = {false};
                java.util.Set<String> brands = filterOptions.brandNames();
                java.util.Set<String> selectedBrands = productBrandsRef.get();
                for (String brandName : brands) {
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(brandName);
                    cb.setSelected(selectedBrands.isEmpty() || selectedBrands.contains(brandName));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary; -fx-cursor: hand;");
                    cb.selectedProperty().addListener((obs, ov, nv) -> {
                        if (!syncingBrandSelection[0]) {
                            allBrandsCb.setSelected(brandCbs.stream().allMatch(javafx.scene.control.CheckBox::isSelected));
                        }
                    });
                    brandCbs.add(cb);
                    brandCheckboxes.getChildren().add(cb);
                }
                allBrandsCb.setOnAction(ae -> {
                    boolean selected = allBrandsCb.isSelected();
                    syncingBrandSelection[0] = true;
                    brandCbs.forEach(cb -> cb.setSelected(selected));
                    syncingBrandSelection[0] = false;
                    allBrandsCb.setSelected(selected);
                });

                javafx.scene.control.ScrollPane brandScroll = new javafx.scene.control.ScrollPane(brandCheckboxes);
                brandScroll.setFitToWidth(true);
                brandScroll.setMaxHeight(120);
                brandScroll.setStyle("-fx-background-color: transparent; -fx-background: -app-surface; -fx-border-color: -app-border; -fx-border-radius: 8;");
                FilterDisclosureSection brandSection = new FilterDisclosureSection(allBrandsCb, brandScroll);
                brandSection.setExpanded(!selectedBrands.isEmpty());

                double initialMinPrice = productMinPriceRef.get() == null
                    ? 0
                    : clampRangeValue(productMinPriceRef.get().doubleValue(), 0, maxPrice);
                double initialMaxPrice = productMaxPriceRef.get() == null
                    ? maxPrice
                    : clampRangeValue(productMaxPriceRef.get().doubleValue(), 0, maxPrice);
                initialMaxPrice = Math.max(initialMinPrice, initialMaxPrice);
                Label priceTitle = FilterControlFactory.sectionTitle("Price Range");
                Label priceLabel = new Label(String.format("%,.0f - %,.0f VND", initialMinPrice, initialMaxPrice));
                priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");
                RangeSlider priceSlider = new RangeSlider(0, maxPrice, initialMinPrice, initialMaxPrice, PRODUCT_FILTER_SLIDER_WIDTH);
                priceSlider.minVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%,.0f - %,.0f VND", nv.doubleValue(), priceSlider.maxVal.get())));
                priceSlider.maxVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%,.0f - %,.0f VND", priceSlider.minVal.get(), nv.doubleValue())));

                double initialMinQty = productMinQtyRef.get() == null ? 0 : clampRangeValue(productMinQtyRef.get(), 0, maxQty);
                double initialMaxQty = productMaxQtyRef.get() == null ? maxQty : clampRangeValue(productMaxQtyRef.get(), 0, maxQty);
                initialMaxQty = Math.max(initialMinQty, initialMaxQty);
                Label qtyTitle = FilterControlFactory.sectionTitle("Stock Range");
                Label qtyLabel = new Label(String.format("%d - %d", (int) initialMinQty, (int) initialMaxQty));
                qtyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");
                RangeSlider qtySlider = new RangeSlider(0, maxQty, initialMinQty, initialMaxQty, PRODUCT_FILTER_SLIDER_WIDTH);
                qtySlider.minVal.addListener((o, ov, nv) -> qtyLabel.setText(String.format("%d - %d", nv.intValue(), (int) qtySlider.maxVal.get())));
                qtySlider.maxVal.addListener((o, ov, nv) -> qtyLabel.setText(String.format("%d - %d", (int) qtySlider.minVal.get(), nv.intValue())));

                Button resetBtn = new Button("Reset");
                resetBtn.getStyleClass().add("filter-reset-btn");
                resetBtn.setOnAction(ae -> {
                    resetAdvancedFiltersRef[0].run();
                    productCurrentPage[0] = 0;
                    updateActiveFiltersRef[0].run();
                    refreshProductListRef[0].run();
                    filterPopup.hide();
                });

                final double fMaxPrice = maxPrice;
                final int fMaxQty = maxQty;
                Button applyBtn = new Button("Apply Filter");
                applyBtn.getStyleClass().add("filter-apply-btn");
                applyBtn.setOnAction(ae -> {
                    java.util.Set<String> nextBrands = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : brandCbs) {
                        if (cb.isSelected()) nextBrands.add(cb.getText());
                    }
                    if (!allBrandsCb.isSelected() && nextBrands.isEmpty() && !brandCbs.isEmpty()) {
                        toastService.showWarning("Select at least one brand or choose All Brands.");
                        return;
                    }
                    productBrandsRef.set(allBrandsCb.isSelected() ? new java.util.LinkedHashSet<>() : nextBrands);
                    double pMin = priceSlider.minVal.get();
                    double pMax = priceSlider.maxVal.get();
                    int qMin = (int) qtySlider.minVal.get();
                    int qMax = (int) qtySlider.maxVal.get();
                    productMinPriceRef.set(pMin <= 0 ? null : MoneySupport.normalize(BigDecimal.valueOf(pMin)));
                    productMaxPriceRef.set(pMax >= fMaxPrice ? null : MoneySupport.normalize(BigDecimal.valueOf(pMax)));
                    productMinQtyRef.set(qMin <= 0 ? null : qMin);
                    productMaxQtyRef.set(qMax >= fMaxQty ? null : qMax);
                    productCurrentPage[0] = 0;
                    updateActiveFiltersRef[0].run();
                    refreshProductListRef[0].run();
                    filterPopup.hide();
                });

                shell.content().getChildren().addAll(
                    brandTitle, brandSection.getNode(), new javafx.scene.control.Separator(),
                    priceTitle, priceLabel, priceSlider, new javafx.scene.control.Separator(),
                    qtyTitle, qtyLabel, qtySlider
                );
                shell.container().getChildren().add(FilterControlFactory.actionRow(resetBtn, applyBtn));
                filterPopup.getContent().setAll(shell.container());
                context.support().showPopupBelow(
                    filterPopup,
                    filterButton,
                    PRODUCT_FILTER_POPUP_X_OFFSET,
                    PRODUCT_FILTER_POPUP_Y_OFFSET
                );
            };

            ProductFilterOptionsCache cachedOptions = productFilterOptionsCache.get();
            if (cachedOptions != null && cachedOptions.matches(categoryId, lowStockOnly)) {
                showFilterPopupContent.accept(cachedOptions.options());
                return;
            }

            filterPopup.getContent().setAll(FilterControlFactory.loadingContainer(PRODUCT_FILTER_POPUP_WIDTH, "Loading filters..."));
            context.support().showPopupBelow(
                filterPopup,
                filterButton,
                PRODUCT_FILTER_POPUP_X_OFFSET,
                PRODUCT_FILTER_POPUP_Y_OFFSET
            );

            javafx.concurrent.Task<ProductService.CatalogFilterOptions> task = new javafx.concurrent.Task<>() {
                @Override
                protected ProductService.CatalogFilterOptions call() {
                    return productService.getCatalogFilterOptions(categoryId, lowStockOnly);
                }
            };
            task.setOnSucceeded(taskEvent -> {
                ProductService.CatalogFilterOptions loadedOptions = task.getValue();
                productFilterOptionsCache.set(new ProductFilterOptionsCache(categoryId, lowStockOnly, loadedOptions));
                if (filterPopup.isShowing()) {
                    showFilterPopupContent.accept(loadedOptions);
                }
            });
            task.setOnFailed(taskEvent -> {
                filterPopup.hide();
                context.showUserFacingError(task.getException());
            });
            Thread worker = new Thread(task, "product-filter-options-loader");
            worker.setDaemon(true);
            worker.start();
        });

        addButton.setOnAction(e -> showProductDialog(context, null, selectedCategory[0], user, refreshProductCatalogAfterChange));

        HBox toolbar = new HBox(12, searchBox, filterButton);
        toolbar.getStyleClass().add("product-toolbar-row");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        VBox leftPane = new VBox(12, toolbar, categoryStrip, activeFilterStrip, table, productStatusBar);
        leftPane.getStyleClass().add("product-catalog-pane");
        leftPane.setFillWidth(true);
        VBox.setVgrow(table, Priority.ALWAYS);

        javafx.scene.control.SplitPane splitPane = new javafx.scene.control.SplitPane(leftPane, inspectorScroll);
        splitPane.getStyleClass().add("product-workspace-split");
        splitPane.setDividerPositions(0.76);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        root.getChildren().addAll(pageHeader, splitPane);
        rebuildCategoryChipsRef[0].run();
        refreshProductListRef[0].run();
        RealtimeDataSync.installProductInventoryRefresh(
            root,
            context.realtimeDataSyncService(),
            refreshProductCatalogAfterChange
        );
        TableViewSupport.enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private static HBox createProductSearchBox(TextField searchField) {
        HBox box = new HBox(10);
        box.getStyleClass().add("product-search-box");
        box.setAlignment(Pos.CENTER_LEFT);
        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.setFill(PRIMARY_COLOR);
        searchIcon.setMouseTransparent(true);
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        box.getChildren().addAll(searchIcon, searchField);
        return box;
    }

    private static Button createProductCategoryChipButton(String text, boolean active, boolean warning, Runnable action) {
        Button chip = new Button(text);
        chip.getStyleClass().add("product-category-chip");
        if (warning) {
            chip.getStyleClass().add("warning");
        }
        if (active) {
            chip.getStyleClass().add("active");
        }
        chip.setCursor(Cursor.HAND);
        chip.setOnAction(e -> action.run());
        return chip;
    }

    private static Label createProductActiveFilterChip(String text, boolean warning) {
        Label chip = new Label(text);
        chip.getStyleClass().add("product-active-filter-chip");
        if (warning) {
            chip.getStyleClass().add("warning");
        }
        return chip;
    }

    private static int safeProductInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static String emptyDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String formatNullableVnd(SceneRuntimeContext context, BigDecimal amount) {
        return amount == null ? "-" : context.support().formatVnd(amount);
    }

    private static double clampRangeValue(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private static boolean isProductOutOfStock(Product product) {
        return product != null && safeProductInt(product.getQuantity()) <= 0;
    }

    private static boolean isProductLowStock(Product product) {
        return product != null && safeProductInt(product.getQuantity()) <= safeProductInt(product.getMinStockLevel());
    }

    private static void refreshProductInspector(
        SceneRuntimeContext context,
        VBox inspector,
        Product product,
        Stage stage,
        User user,
        Runnable refreshAction
    ) {
        inspector.getChildren().clear();
        if (product == null) {
            Label title = new Label("Select a product");
            title.getStyleClass().add("product-inspector-title");
            Label helper = new Label("Choose one row to view stock, pricing, and catalog details.");
            helper.getStyleClass().add("product-inspector-empty-text");
            helper.setWrapText(true);
            inspector.getChildren().addAll(title, helper);
            return;
        }

        Label title = new Label(product.getName() == null ? "Unnamed product" : product.getName());
        title.getStyleClass().add("product-inspector-title");
        title.setWrapText(true);
        Label category = new Label(product.getCategory() != null ? product.getCategory().getName() : "Uncategorized");
        category.getStyleClass().add("product-inspector-subtitle");
        VBox titleBlock = new VBox(3, title, category);
        HBox heading = new HBox(titleBlock);
        heading.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        HBox stockMetrics = new HBox(10,
            createProductMetricTile("Stock", String.valueOf(safeProductInt(product.getQuantity()))),
            createProductMetricTile("Min", String.valueOf(safeProductInt(product.getMinStockLevel())))
        );
        stockMetrics.setAlignment(Pos.CENTER_LEFT);
        VBox marginMetric = createProductMetricTile("Margin", calculateProductMarginText(product));
        VBox metrics = new VBox(10, stockMetrics, marginMetric);
        metrics.getStyleClass().add("product-inspector-metrics");

        VBox details = new VBox(8,
            createProductInspectorMetaRow("SKU", emptyDash(product.getSku())),
            createProductInspectorMetaRow("Barcode", emptyDash(product.getBarcode())),
            createProductInspectorMetaRow("Brand", product.getBrand() != null ? product.getBrand().getName() : "-"),
            createProductInspectorMetaRow("Origin", product.getOrigin() != null ? product.getOrigin().getName() : "-"),
            createProductInspectorMetaRow("Unit", product.getUnit() != null ? product.getUnit().getName() : "-"),
            createProductInspectorMetaRow("Selling", context.support().formatVnd(product.getPrice())),
            createProductInspectorMetaRow("Import", formatNullableVnd(context, product.getImportPrice()))
        );
        details.getStyleClass().add("product-inspector-detail-list");

        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            Label descTitle = new Label("Description");
            descTitle.getStyleClass().add("product-inspector-section-title");
            Label desc = new Label(product.getDescription());
            desc.getStyleClass().add("product-inspector-description");
            desc.setWrapText(true);
            details.getChildren().addAll(descTitle, desc);
        }

        Button editButton = new Button("Edit Product");
        editButton.getStyleClass().add("product-inspector-primary-button");
        editButton.setMaxWidth(Double.MAX_VALUE);
        editButton.setOnAction(e -> showProductDialog(context, product, product.getCategory(), user, refreshAction));

        VBox actions = new VBox(10, editButton);
        actions.getStyleClass().add("product-inspector-actions");
        if (context.authorizationService().canAccessImportGoods(user) && isProductLowStock(product)) {
            Button importButton = new Button("Open Import Goods");
            importButton.getStyleClass().add("product-inspector-secondary-button");
            importButton.setMaxWidth(Double.MAX_VALUE);
            importButton.setOnAction(e -> context.navigator().showImportGoods(new ImportOrderPrefill(product.getId(), suggestedProductReorderQuantity(product))));
            actions.getChildren().add(importButton);
        }

        inspector.getChildren().addAll(heading, metrics, details, actions);
    }

    private static VBox createProductMetricTile(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("product-inspector-metric-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("product-inspector-metric-value");
        value.setWrapText(true);
        VBox tile = new VBox(4, label, value);
        tile.getStyleClass().add("product-inspector-metric");
        tile.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tile, Priority.ALWAYS);
        return tile;
    }

    private static HBox createProductInspectorMetaRow(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("product-inspector-meta-label");
        label.setMinWidth(72);
        label.setPrefWidth(72);
        Label value = new Label(valueText);
        value.getStyleClass().add("product-inspector-meta-value");
        value.setWrapText(true);
        HBox row = new HBox(10, label, value);
        row.getStyleClass().add("product-inspector-meta-row");
        row.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(value, Priority.ALWAYS);
        return row;
    }

    private static String calculateProductMarginText(Product product) {
        if (product == null || product.getPrice() == null || product.getImportPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return "-";
        }
        BigDecimal price = MoneySupport.normalize(product.getPrice());
        BigDecimal cost = MoneySupport.normalize(product.getImportPrice());
        BigDecimal margin = price.subtract(cost);
        BigDecimal percent = margin.multiply(BigDecimal.valueOf(100)).divide(price, 1, RoundingMode.HALF_UP);
        return String.format("%,.0f / %s%%", margin.doubleValue(), percent.stripTrailingZeros().toPlainString());
    }

    private static int suggestedProductReorderQuantity(Product product) {
        int quantity = safeProductInt(product.getQuantity());
        int minStock = Math.max(1, safeProductInt(product.getMinStockLevel()));
        return Math.max(1, minStock - quantity);
    }

    private static void showProductDialog(
        SceneRuntimeContext context,
        Product product,
        Category contextCategory,
        User user,
        Runnable onSave
    ) {
        ProductDialog.show(
            context.owner(),
            product,
            contextCategory,
            user,
            onSave,
            new ProductDialog.Context(
                context.categoryRepository(),
                context.brandService(),
                context.originService(),
                context.unitService(),
                context.productService(),
                context.toastService()
            )
        );
    }
}
