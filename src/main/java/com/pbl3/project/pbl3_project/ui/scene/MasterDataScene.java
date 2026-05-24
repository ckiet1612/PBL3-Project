package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.util.AsyncPageCache;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.SortCriterion;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.springframework.data.domain.Pageable;

public final class MasterDataScene {
    private static final Color PRIMARY_COLOR = Color.web("#1d7df2");

    public record Options() {
    }

    private record MasterPageResult<T>(org.springframework.data.domain.Page<T> page, int pageIndex) {
    }

    private record SupplierPageResult(
        org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Supplier> page,
        int pageIndex
    ) {
    }

    private final SceneRuntimeContext context;

    private MasterDataScene(SceneRuntimeContext context) {
        this.context = context;
    }

    public static Node create(SceneRuntimeContext context, com.pbl3.project.pbl3_project.entity.User user, Options options) {
        return new MasterDataScene(context).createAttributesView();
    }

    private javafx.scene.Node createAttributesView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12, 20, 8, 20));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: -app-surface-muted;");

        javafx.scene.layout.StackPane contentArea = new javafx.scene.layout.StackPane();
        VBox catView = createSimpleMasterDataView("Categories", "master-categories",
            (search, pageable) -> context.categoryService().searchCategories(search, pageable),
            name -> context.categoryService().saveCategory(new com.pbl3.project.pbl3_project.entity.Category(null, name)),
            id -> context.categoryService().deleteCategory(id)
        );
        VBox brandView = createSimpleMasterDataView("Brands", "master-brands",
            (search, pageable) -> context.brandService().searchBrands(search, pageable),
            name -> context.brandService().saveBrand(new com.pbl3.project.pbl3_project.entity.Brand(name)),
            id -> context.brandService().deleteBrand(id)
        );
        VBox supplierView = createSupplierMasterDataView();
        VBox originView = createSimpleMasterDataView("Origins", "master-origins",
            (search, pageable) -> context.originService().searchOrigins(search, pageable),
            name -> context.originService().saveOrigin(new com.pbl3.project.pbl3_project.entity.Origin(name)),
            id -> context.originService().deleteOrigin(id)
        );
        VBox unitView = createSimpleMasterDataView("Units", "master-units",
            (search, pageable) -> context.unitService().searchUnits(search, pageable),
            name -> context.unitService().saveUnit(new com.pbl3.project.pbl3_project.entity.Unit(name)),
            id -> context.unitService().deleteUnit(id)
        );
        javafx.scene.control.TableView<?> catTable = TableViewSupport.findFirstTableView(catView);
        javafx.scene.control.TableView<?> brandTable = TableViewSupport.findFirstTableView(brandView);
        javafx.scene.control.TableView<?> supplierTable = TableViewSupport.findFirstTableView(supplierView);
        javafx.scene.control.TableView<?> originTable = TableViewSupport.findFirstTableView(originView);
        javafx.scene.control.TableView<?> unitTable = TableViewSupport.findFirstTableView(unitView);

        VBox[] views = {catView, brandView, supplierView, originView, unitView};
        for (VBox v : views) {
            v.setVisible(false);
            v.setManaged(false);
            contentArea.getChildren().add(v);
        }
        views[0].setVisible(true);
        views[0].setManaged(true);

        String[] tabNames = {"Categories", "Brands", "Suppliers", "Origins", "Units"};
        javafx.scene.Node slidingMenu = createSlidingMenu(tabNames, index -> {
            for (int i = 0; i < views.length; i++) {
                views[i].setVisible(i == index);
                views[i].setManaged(i == index);
            }
        });

        root.getChildren().addAll(slidingMenu, contentArea);
        VBox.setVgrow(contentArea, javafx.scene.layout.Priority.ALWAYS);
        TableViewSupport.enableDeselectOnOutsideClick(root, catTable, brandTable, supplierTable, originTable, unitTable);

        return root;
    }

    private <T> VBox createSimpleMasterDataView(String title, String sortStateKey,
                                                java.util.function.BiFunction<String, org.springframework.data.domain.Pageable, org.springframework.data.domain.Page<T>> pageFetcher,
                                                java.util.function.Consumer<String> saver,
                                                java.util.function.Consumer<Long> deleter) {
        VBox root = new VBox();
        applyStandardTablePageLayout(root, new Insets(6, 20, 20, 20));
        TableSortState sortState = getOrCreateTableSortState(
            sortStateKey,
            new SortCriterion("name", javafx.scene.control.TableColumn.SortType.ASCENDING)
        );
        java.util.LinkedHashMap<String, String> sortProperties = new java.util.LinkedHashMap<>();
        sortProperties.put("name", "name");
        java.util.LinkedHashMap<String, String> sortLabels = new java.util.LinkedHashMap<>();
        sortLabels.put("name", "Name");
        javafx.scene.layout.HBox searchBox = new javafx.scene.layout.HBox(0);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getStyleClass().add("expandable-search-box");
        searchBox.setPrefSize(40, 40);
        searchBox.setMinSize(40, 40);
        searchBox.setMaxSize(40, 40);

        javafx.scene.shape.SVGPath sIcon = new javafx.scene.shape.SVGPath();
        sIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        sIcon.setFill(PRIMARY_COLOR);

        javafx.scene.layout.Region sSpacer = new javafx.scene.layout.Region();
        sSpacer.setMinWidth(0); sSpacer.setPrefWidth(0);

        TextField sField = new TextField();
        sField.setPromptText("Search");
        sField.getStyleClass().add("search-text-field");
        sField.setMinWidth(0); sField.setMaxWidth(0); sField.setPrefWidth(0); sField.setOpacity(0);

        searchBox.getChildren().addAll(sIcon, sSpacer, sField);

        javafx.animation.Timeline sExpand = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(searchBox.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchBox.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sSpacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        javafx.animation.Timeline sCollapse = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(searchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sSpacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        javafx.scene.control.TableView<T> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        final int pageSize = 15;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> searchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicLong pageLoadVersion = new java.util.concurrent.atomic.AtomicLong();
        AsyncPageCache<MasterPageResult<T>> pageCache = new AsyncPageCache<>(48);

        Label rowCountLabel = createStatusMetaLabel(java.text.MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label pageLabel = createStatusMetaLabel(java.text.MessageFormat.format("Page {0} / {1}", 0, 0));
        Button prevBtn = ButtonFactory.pageNav("Prev");
        Button nextBtn = ButtonFactory.pageNav("Next");

        Runnable updateStatusBar = () -> updatePagedStatus(
            table,
            rowCountLabel,
            pageLabel,
            prevBtn,
            nextBtn,
            totalElements[0],
            currentPage[0],
            totalPages[0],
            pageSize
        );
        Runnable refreshPage = () -> {
            int requestedPage = currentPage[0];
            String search = searchRef.get();
            java.util.List<SortCriterion> sortSnapshot = sortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(pageIndex, search, sortSnapshot);
            java.util.function.IntFunction<MasterPageResult<T>> fetchMasterPage = pageIndex -> {
                int resolvedPage = pageIndex;
                org.springframework.data.domain.Page<T> pageData = pageFetcher.apply(
                    search,
                    createPageable(sortForLoad, sortProperties, resolvedPage, pageSize)
                );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = pageFetcher.apply(
                        search,
                        createPageable(sortForLoad, sortProperties, resolvedPage, pageSize)
                    );
                }
                return new MasterPageResult<>(pageData, resolvedPage);
            };
            AsyncUiTask.runLatestCachedTableLoad(
                table,
                prevBtn,
                nextBtn,
                pageLoadVersion,
                pageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchMasterPage.apply(requestedPage),
                result -> {
                    org.springframework.data.domain.Page<T> pageData = result.page();
                    currentPage[0] = result.pageIndex();
                    totalElements[0] = pageData.getTotalElements();
                    totalPages[0] = pageData.getTotalPages();
                    table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
                    updateStatusBar.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < totalPages[0]) {
                        pageCache.prefetch(cacheKeyForPage.apply(nextPage), () -> fetchMasterPage.apply(nextPage), null, sortStateKey + "-next-page-prefetch");
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        pageCache.prefetch(cacheKeyForPage.apply(previousPage), () -> fetchMasterPage.apply(previousPage), null, sortStateKey + "-prev-page-prefetch");
                    }
                },
                context::showUserFacingError,
                "Loading " + title.toLowerCase(java.util.Locale.ROOT) + "...",
                "Could not load " + title.toLowerCase(java.util.Locale.ROOT),
                sortStateKey + "-page-loader"
            );
        };
        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                refreshPage.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage[0] + 1 < totalPages[0]) {
                currentPage[0]++;
                refreshPage.run();
            }
        });

        searchBox.setOnMouseClicked(ev -> {
            if (searchBox.getMaxWidth() == 40) { sExpand.play(); sField.requestFocus(); }
            else if (ev.getTarget() == sIcon || ev.getTarget() == searchBox) { sField.clear(); root.requestFocus(); sCollapse.play(); }
        });

        javafx.scene.layout.BorderPane topBar = new javafx.scene.layout.BorderPane();
        topBar.setRight(searchBox);

        javafx.animation.PauseTransition searchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            searchRef.set(sField.getText());
            refreshPage.run();
        });
        sField.textProperty().addListener((obs, oldV, newV) -> searchPause.playFromStart());

        javafx.scene.control.TableColumn<T, Integer> sttCol = new javafx.scene.control.TableColumn<>("ID");
        sttCol.setSortable(false);
        sttCol.setCellValueFactory(column -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
            currentPage[0] * pageSize + table.getItems().indexOf(column.getValue()) + 1));

        javafx.scene.control.TableColumn<T, String> nameCol = new javafx.scene.control.TableColumn<>("Title");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));

        Runnable deleteAction = () -> {
            java.util.List<T> selectedItems = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedItems.isEmpty()) return;

            boolean confirmed = selectedItems.size() >= 5
                ? DialogSupport.showTypedDangerConfirm(
                    context.owner(),
                    "Confirm Deletion",
                    MessageFormat.format("Delete {0} selected item(s)? This is a large delete operation.", selectedItems.size()),
                    "DELETE"
                )
                : showConfirmDialog(
                    "Confirm Deletion",
                    MessageFormat.format("Are you sure you want to delete {0} selected item(s)?", selectedItems.size())
                );
            if (!confirmed) {
                return;
            }

            int deletedCount = 0;
            for (T item : selectedItems) {
                try {
                    java.lang.reflect.Method getId = item.getClass().getMethod("getId");
                    Long id = (Long) getId.invoke(item);
                    deleter.accept(id);
                    deletedCount++;
                } catch (Exception ex) {
                     context.toastService().showError(java.text.MessageFormat.format("Error: {0}", ex.getMessage()));
                }
            }
            if (deletedCount > 0) {
                context.toastService().showSuccess(java.text.MessageFormat.format("Deleted {0}", deletedCount + " items"));
                pageCache.clear();
                refreshPage.run();
            }
        };

        table.setOnKeyPressed(event -> {
            javafx.scene.input.KeyCode code = event.getCode();
            if (code == javafx.scene.input.KeyCode.DELETE || code == javafx.scene.input.KeyCode.BACK_SPACE || code == javafx.scene.input.KeyCode.ENTER) {
                deleteAction.run();
            }
        });

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<T> row = new javafx.scene.control.TableRow<>();
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();

            javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete");
            deleteItem.setStyle("-fx-text-fill: -app-danger;");
            deleteItem.setOnAction(event -> {
                if (table.getSelectionModel().getSelectedItems().isEmpty() && row.getItem() != null) {
                    table.getSelectionModel().select(row.getItem());
                }
                deleteAction.run();
            });

            contextMenu.getItems().add(deleteItem);

            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((javafx.scene.control.ContextMenu)null)
                .otherwise(contextMenu)
            );
            return row;
        });

        table.getColumns().add(sttCol);
        table.getColumns().add(nameCol);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<T, ?>> sortColumns = new java.util.LinkedHashMap<>();
        sortColumns.put("name", nameCol);
        installSortHeaderIndicators(sortColumns);
        javafx.scene.layout.HBox addBox = new javafx.scene.layout.HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);
        TextField nameField = new TextField();
        nameField.setPromptText(java.text.MessageFormat.format("Enter {0} Name...", title));
        nameField.getStyleClass().add("master-data-entry-field");
        Button addBtn = ButtonFactory.expandableGreenAction("Add", 100);
        addBtn.setOnAction(e -> {
            String name = nameField.getText();
            if (name == null || name.isBlank()) return;
            try {
                AsyncUiTask.runButtonTask(
                    addBtn,
                    null,
                    "Adding...",
                    () -> {
                        saver.accept(name);
                        return null;
                    },
                    ignored -> {
                        context.toastService().showSuccess(java.text.MessageFormat.format("Added {0}", name));
                        nameField.clear();
                        currentPage[0] = 0;
                        pageCache.clear();
                        refreshPage.run();
                    },
                    ex -> context.toastService().showError(java.text.MessageFormat.format("Error: {0}", ex.getMessage())),
                    "master-data-add"
                );
            } catch (Exception ex) {
                context.toastService().showError(java.text.MessageFormat.format("Error: {0}", ex.getMessage()));
            }
        });

        addBox.getChildren().addAll(nameField, addBtn);
        topBar.setLeft(addBox);
        javafx.scene.layout.BorderPane.setAlignment(addBox, Pos.CENTER_LEFT);

        Label sortStatusLabel = createSortStatusLabel(sortState, sortLabels);
        Runnable applySortUi = () -> {
            applySortStateToTable(table, sortColumns, sortState);
            sortStatusLabel.setText(buildSortStatusText(sortState, sortLabels));
        };

        applySortUi.run();
        installManualServerSorting(
            table,
            sortColumns,
            sortState,
            () -> {
                applySortUi.run();
                currentPage[0] = 0;
                refreshPage.run();
            }
        );

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, sortStatusLabel, rowCountLabel, pageLabel, prevBtn, nextBtn);
        applyStandardTableStatusBar(statusBar);

        root.getChildren().addAll(topBar, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<T>) c -> updateStatusBar.run());
        javafx.application.Platform.runLater(refreshPage);
        TableViewSupport.enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private VBox createSupplierMasterDataView() {
        VBox root = new VBox();
        applyStandardTablePageLayout(root, new Insets(6, 20, 20, 20));
        final String supplierSortStateKey = "master-suppliers";
        TableSortState supplierSortState = getOrCreateTableSortState(
            supplierSortStateKey,
            new SortCriterion("name", javafx.scene.control.TableColumn.SortType.ASCENDING)
        );
        java.util.LinkedHashMap<String, String> supplierSortProperties = new java.util.LinkedHashMap<>();
        supplierSortProperties.put("name", "name");
        supplierSortProperties.put("phone", "phone");
        java.util.LinkedHashMap<String, String> supplierSortLabels = new java.util.LinkedHashMap<>();
        supplierSortLabels.put("name", "Name");
        supplierSortLabels.put("phone", "Phone");
        javafx.scene.layout.HBox searchBox2 = new javafx.scene.layout.HBox(0);
        searchBox2.setAlignment(Pos.CENTER);
        searchBox2.getStyleClass().add("expandable-search-box");
        searchBox2.setPrefSize(40, 40); searchBox2.setMinSize(40, 40); searchBox2.setMaxSize(40, 40);
        javafx.scene.shape.SVGPath sIcon2 = new javafx.scene.shape.SVGPath();
        sIcon2.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        sIcon2.setFill(PRIMARY_COLOR);
        javafx.scene.layout.Region sSpacer2 = new javafx.scene.layout.Region();
        sSpacer2.setMinWidth(0); sSpacer2.setPrefWidth(0);
        TextField sField2 = new TextField();
        sField2.setPromptText("Search"); sField2.getStyleClass().add("search-text-field");
        sField2.setMinWidth(0); sField2.setMaxWidth(0); sField2.setPrefWidth(0); sField2.setOpacity(0);
        searchBox2.getChildren().addAll(sIcon2, sSpacer2, sField2);
        javafx.animation.Timeline sExpand2 = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(searchBox2.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(searchBox2.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sSpacer2.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline sCollapse2 = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(searchBox2.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(searchBox2.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sSpacer2.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Supplier> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);
        final int supplierPageSize = 15;
        final int[] supplierCurrentPage = {0};
        final int[] supplierTotalPages = {0};
        final long[] supplierTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> supplierSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicLong supplierPageLoadVersion = new java.util.concurrent.atomic.AtomicLong();
        AsyncPageCache<SupplierPageResult> supplierPageCache = new AsyncPageCache<>(48);

        Label supplierRowCountLabel = createStatusMetaLabel(java.text.MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label supplierPageLabel = createStatusMetaLabel(java.text.MessageFormat.format("Page {0} / {1}", 0, 0));
        Button supplierPrevBtn = ButtonFactory.pageNav("Prev");
        Button supplierNextBtn = ButtonFactory.pageNav("Next");

        Runnable updateSupplierStatusBar = () -> updatePagedStatus(
            table,
            supplierRowCountLabel,
            supplierPageLabel,
            supplierPrevBtn,
            supplierNextBtn,
            supplierTotalElements[0],
            supplierCurrentPage[0],
            supplierTotalPages[0],
            supplierPageSize
        );
        Runnable refreshSupplierPage = () -> {
            int requestedPage = supplierCurrentPage[0];
            String search = supplierSearchRef.get();
            java.util.List<SortCriterion> sortSnapshot = supplierSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(pageIndex, search, sortSnapshot);
            java.util.function.IntFunction<SupplierPageResult> fetchSupplierPage = pageIndex -> {
                int resolvedPage = pageIndex;
                org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Supplier> pageData =
                    context.supplierService().searchSuppliers(
                        search,
                        createPageable(sortForLoad, supplierSortProperties, resolvedPage, supplierPageSize)
                    );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = context.supplierService().searchSuppliers(
                        search,
                        createPageable(sortForLoad, supplierSortProperties, resolvedPage, supplierPageSize)
                    );
                }
                return new SupplierPageResult(pageData, resolvedPage);
            };
            AsyncUiTask.runLatestCachedTableLoad(
                table,
                supplierPrevBtn,
                supplierNextBtn,
                supplierPageLoadVersion,
                supplierPageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchSupplierPage.apply(requestedPage),
                result -> {
                    org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Supplier> pageData = result.page();
                    supplierCurrentPage[0] = result.pageIndex();
                    supplierTotalElements[0] = pageData.getTotalElements();
                    supplierTotalPages[0] = pageData.getTotalPages();
                    table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
                    updateSupplierStatusBar.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < supplierTotalPages[0]) {
                        supplierPageCache.prefetch(cacheKeyForPage.apply(nextPage), () -> fetchSupplierPage.apply(nextPage), null, "master-suppliers-next-page-prefetch");
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        supplierPageCache.prefetch(cacheKeyForPage.apply(previousPage), () -> fetchSupplierPage.apply(previousPage), null, "master-suppliers-prev-page-prefetch");
                    }
                },
                context::showUserFacingError,
                "Loading suppliers...",
                "Could not load suppliers",
                "master-suppliers-page-loader"
            );
        };
        supplierPrevBtn.setOnAction(e -> {
            if (supplierCurrentPage[0] > 0) {
                supplierCurrentPage[0]--;
                refreshSupplierPage.run();
            }
        });
        supplierNextBtn.setOnAction(e -> {
            if (supplierCurrentPage[0] + 1 < supplierTotalPages[0]) {
                supplierCurrentPage[0]++;
                refreshSupplierPage.run();
            }
        });

        searchBox2.setOnMouseClicked(ev -> {
            if (searchBox2.getMaxWidth() == 40) { sExpand2.play(); sField2.requestFocus(); }
            else if (ev.getTarget() == sIcon2 || ev.getTarget() == searchBox2) { sField2.clear(); root.requestFocus(); sCollapse2.play(); }
        });
        javafx.scene.layout.BorderPane topBar2 = new javafx.scene.layout.BorderPane();
        topBar2.setRight(searchBox2);

        javafx.animation.PauseTransition supplierSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        supplierSearchPause.setOnFinished(e -> {
            supplierCurrentPage[0] = 0;
            supplierSearchRef.set(sField2.getText());
            refreshSupplierPage.run();
        });
        sField2.textProperty().addListener((obs, oldV, newV) -> supplierSearchPause.playFromStart());

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, Integer> sttCol = new javafx.scene.control.TableColumn<>("ID");
        sttCol.setSortable(false);
        sttCol.setCellValueFactory(column -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
            supplierCurrentPage[0] * supplierPageSize + table.getItems().indexOf(column.getValue()) + 1));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, String> nameCol = new javafx.scene.control.TableColumn<>("Title");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, String> phoneCol = new javafx.scene.control.TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("phone"));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, String> addrCol = new javafx.scene.control.TableColumn<>("Address");
        addrCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("address"));

        Runnable deleteAction = () -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Supplier> selectedItems = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedItems.isEmpty()) return;

            boolean confirmed = selectedItems.size() >= 5
                ? DialogSupport.showTypedDangerConfirm(
                    context.owner(),
                    "Confirm Deletion",
                    MessageFormat.format("Delete {0} selected supplier(s)? This is a large delete operation.", selectedItems.size()),
                    "DELETE"
                )
                : showConfirmDialog(
                    "Confirm Deletion",
                    MessageFormat.format("Are you sure you want to delete {0} selected supplier(s)?", selectedItems.size())
                );
            if (!confirmed) {
                return;
            }

            int deletedCount = 0;
            for (com.pbl3.project.pbl3_project.entity.Supplier item : selectedItems) {
                try {
                     context.supplierService().deleteSupplier(item.getId());
                     deletedCount++;
                } catch (Exception ex) {
                     context.toastService().showError(java.text.MessageFormat.format("Error: {0}", ex.getMessage()));
                }
            }

            if (deletedCount > 0) {
                 context.toastService().showSuccess(java.text.MessageFormat.format("Deleted {0}", deletedCount + " suppliers"));
                 supplierPageCache.clear();
                 refreshSupplierPage.run();
            }
        };

        table.setOnKeyPressed(event -> {
            javafx.scene.input.KeyCode code = event.getCode();
            if (code == javafx.scene.input.KeyCode.DELETE || code == javafx.scene.input.KeyCode.BACK_SPACE || code == javafx.scene.input.KeyCode.ENTER) {
                deleteAction.run();
            }
        });

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Supplier> row = new javafx.scene.control.TableRow<>();
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();

            javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete");
            deleteItem.setStyle("-fx-text-fill: -app-danger;");
            deleteItem.setOnAction(event -> {
                if (table.getSelectionModel().getSelectedItems().isEmpty() && row.getItem() != null) {
                    table.getSelectionModel().select(row.getItem());
                }
                deleteAction.run();
            });

            contextMenu.getItems().add(deleteItem);

            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((javafx.scene.control.ContextMenu)null)
                .otherwise(contextMenu)
            );
            return row;
        });

        table.getColumns().add(sttCol);
        table.getColumns().add(nameCol);
        table.getColumns().add(phoneCol);
        table.getColumns().add(addrCol);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, ?>> supplierSortColumns =
            new java.util.LinkedHashMap<>();
        supplierSortColumns.put("name", nameCol);
        supplierSortColumns.put("phone", phoneCol);
        installSortHeaderIndicators(supplierSortColumns);
        javafx.scene.layout.HBox addBox = new javafx.scene.layout.HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);
        TextField nameField = new TextField(); nameField.setPromptText("Name");
        TextField phoneField = new TextField(); phoneField.setPromptText("Phone");
        TextField addrField = new TextField(); addrField.setPromptText("Address");
        nameField.getStyleClass().add("master-data-entry-field");
        phoneField.getStyleClass().add("master-data-entry-field");
        addrField.getStyleClass().add("master-data-entry-field");
        Button addBtn = ButtonFactory.expandableGreenAction("Add", 100);
        addBtn.setOnAction(e -> {
            String name = nameField.getText();
            String phone = phoneField.getText();
            String address = addrField.getText();
            if (name == null || name.isBlank()) return;
            try {
                AsyncUiTask.runButtonTask(
                    addBtn,
                    null,
                    "Adding...",
                    () -> context.supplierService().saveSupplier(new com.pbl3.project.pbl3_project.entity.Supplier(name, phone, address)),
                    saved -> {
                        context.toastService().showSuccess(java.text.MessageFormat.format("Added {0}", name));
                        nameField.clear(); phoneField.clear(); addrField.clear();
                        supplierCurrentPage[0] = 0;
                        supplierPageCache.clear();
                        refreshSupplierPage.run();
                    },
                    ex -> context.toastService().showError(java.text.MessageFormat.format("Error: {0}", ex.getMessage())),
                    "supplier-add"
                );
            } catch (Exception ex) {
                context.toastService().showError(java.text.MessageFormat.format("Error: {0}", ex.getMessage()));
            }
        });

        addBox.getChildren().addAll(nameField, phoneField, addrField, addBtn);
        topBar2.setLeft(addBox);
        javafx.scene.layout.BorderPane.setAlignment(addBox, Pos.CENTER_LEFT);

        Label supplierSortStatusLabel = createSortStatusLabel(supplierSortState, supplierSortLabels);
        Runnable applySupplierSortUi = () -> {
            applySortStateToTable(table, supplierSortColumns, supplierSortState);
            supplierSortStatusLabel.setText(buildSortStatusText(supplierSortState, supplierSortLabels));
        };
        applySupplierSortUi.run();
        installManualServerSorting(
            table,
            supplierSortColumns,
            supplierSortState,
            () -> {
                applySupplierSortUi.run();
                supplierCurrentPage[0] = 0;
                refreshSupplierPage.run();
            }
        );

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, supplierSortStatusLabel, supplierRowCountLabel, supplierPageLabel, supplierPrevBtn, supplierNextBtn);
        applyStandardTableStatusBar(statusBar);

        root.getChildren().addAll(topBar2, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Supplier>) c -> updateSupplierStatusBar.run());
        javafx.application.Platform.runLater(refreshSupplierPage);
        TableViewSupport.enableDeselectOnOutsideClick(root, table);
        return root;
    }


















    private Node createSlidingMenu(String[] tabNames, Consumer<Integer> onSelect) {
        return context.support().createSlidingMenu(tabNames, onSelect);
    }

    private void applyStandardTablePageLayout(VBox root, Insets padding) {
        context.support().applyStandardTablePageLayout(root, padding);
    }

    private void applyStandardTableSizing(TableView<?> table) {
        context.support().applyStandardTableSizing(table);
    }

    private void applyStandardTableStatusBar(HBox statusBar) {
        context.support().applyStandardTableStatusBar(statusBar);
    }

    private Label createStatusMetaLabel(String text) {
        return context.support().createStatusMetaLabel(text);
    }

    private void updatePagedStatus(TableView<?> table, Label rowCountLabel, Label pageLabel, Button prevButton, Button nextButton, long totalElements, int currentPage, int totalPages, int pageSize) {
        context.support().updatePagedStatus(table, rowCountLabel, pageLabel, prevButton, nextButton, totalElements, currentPage, totalPages, pageSize);
    }

    private Pageable createPageable(TableSortState sortState, Map<String, String> propertyByUiKey, int page, int size) {
        return context.support().createPageable(sortState, propertyByUiKey, page, size);
    }

    private TableSortState getOrCreateTableSortState(String stateKey, SortCriterion... defaultCriteria) {
        return context.support().getOrCreateTableSortState(stateKey, defaultCriteria);
    }

    private <T> void installSortHeaderIndicators(LinkedHashMap<String, TableColumn<T, ?>> columnsByKey) {
        context.support().installSortHeaderIndicators(columnsByKey);
    }

    private <T> void applySortStateToTable(TableView<T> table, LinkedHashMap<String, TableColumn<T, ?>> columnsByKey, TableSortState sortState) {
        context.support().applySortStateToTable(table, columnsByKey, sortState);
    }

    private Label createSortStatusLabel(TableSortState sortState, Map<String, String> labelsByKey) {
        return context.support().createSortStatusLabel(sortState, labelsByKey);
    }

    private String buildSortStatusText(TableSortState sortState, Map<String, String> labelsByKey) {
        return context.support().buildSortStatusText(sortState, labelsByKey);
    }

    private <T> void installManualServerSorting(TableView<T> table, LinkedHashMap<String, TableColumn<T, ?>> columnsByKey, TableSortState sortState, Runnable onSortChanged) {
        context.support().installManualServerSorting(table, columnsByKey, sortState, onSortChanged);
    }

    private boolean showConfirmDialog(String title, String content) {
        return context.support().showConfirmDialog(title, content);
    }
}
