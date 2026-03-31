package com.pbl3.project.pbl3_project;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.TranslateTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import javafx.stage.Popup;
import com.pbl3.project.pbl3_project.service.*;
import com.pbl3.project.pbl3_project.StageReadyEvent;

@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final AuthService authService;
    private final ProductService productService;
    private final com.pbl3.project.pbl3_project.repository.CategoryRepository categoryRepository; // Keep for legacy if needed, or replace usage
    private final OrderService orderService;
    private final ReportService reportService;
    private final ToastService toastService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final SupplierService supplierService;
    private final OriginService originService;
    private final UnitService unitService;
    private final ImportOrderService importOrderService;
    private final ReceiptService receiptService;
    private final InventoryTransactionService transactionService;

    public StageInitializer(AuthService authService,
                            ProductService productService,
                            com.pbl3.project.pbl3_project.repository.CategoryRepository categoryRepository,
                            OrderService orderService,
                            ReportService reportService,
                            ToastService toastService,
                            CategoryService categoryService,
                            BrandService brandService,
                            SupplierService supplierService,
                            OriginService originService,
                            UnitService unitService,
                            ImportOrderService importOrderService,
                            ReceiptService receiptService,
                            InventoryTransactionService transactionService) {
        this.authService = authService;
        this.productService = productService;
        this.categoryRepository = categoryRepository;
        this.orderService = orderService;
        this.reportService = reportService;
        this.toastService = toastService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.supplierService = supplierService;
        this.originService = originService;
        this.unitService = unitService;
        this.importOrderService = importOrderService;
        this.receiptService = receiptService;
        this.transactionService = transactionService;
    }

    // --- Reusable UI Components ---
    public static class RangeSlider extends javafx.scene.layout.Pane {
        public final javafx.beans.property.DoubleProperty minVal = new javafx.beans.property.SimpleDoubleProperty(0);
        public final javafx.beans.property.DoubleProperty maxVal = new javafx.beans.property.SimpleDoubleProperty(1);
        
        public RangeSlider(double minBound, double maxBound, double initialMin, double initialMax, double sliderWidth) {
            setPrefHeight(24);
            setPrefWidth(sliderWidth); // explicitly set prefWidth
            javafx.scene.shape.Rectangle bgTrack = new javafx.scene.shape.Rectangle(0, 10, sliderWidth, 4);
            bgTrack.setFill(javafx.scene.paint.Color.web("#E0E0E0"));
            bgTrack.setArcWidth(4); bgTrack.setArcHeight(4);
            
            javafx.scene.shape.Rectangle activeTrack = new javafx.scene.shape.Rectangle(0, 10, sliderWidth, 4);
            activeTrack.setFill(javafx.scene.paint.Color.web("#1976D2"));
            activeTrack.setArcWidth(4); activeTrack.setArcHeight(4);
            
            javafx.scene.shape.Circle minThumb = new javafx.scene.shape.Circle(8, javafx.scene.paint.Color.WHITE);
            minThumb.setStroke(javafx.scene.paint.Color.web("#B0BEC5")); minThumb.setStrokeWidth(1);
            minThumb.setCenterY(12); minThumb.setCursor(javafx.scene.Cursor.HAND);
            minThumb.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);");
            
            javafx.scene.shape.Circle maxThumb = new javafx.scene.shape.Circle(8, javafx.scene.paint.Color.WHITE);
            maxThumb.setStroke(javafx.scene.paint.Color.web("#B0BEC5")); maxThumb.setStrokeWidth(1);
            maxThumb.setCenterY(12); maxThumb.setCursor(javafx.scene.Cursor.HAND);
            maxThumb.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);");

            getChildren().addAll(bgTrack, activeTrack, minThumb, maxThumb);

            Runnable updateLayout = () -> {
                double range = maxBound - minBound;
                if (range == 0) return;
                double minX = ((minVal.get() - minBound) / range) * sliderWidth;
                double maxX = ((maxVal.get() - minBound) / range) * sliderWidth;
                minThumb.setCenterX(minX);
                maxThumb.setCenterX(maxX);
                activeTrack.setX(minX);
                activeTrack.setWidth(maxX - minX);
            };

            minVal.addListener((obs, ov, nv) -> updateLayout.run());
            maxVal.addListener((obs, ov, nv) -> updateLayout.run());

            minVal.set(initialMin); maxVal.set(initialMax);

            javafx.event.EventHandler<javafx.scene.input.MouseEvent> dragMin = e -> {
                double newX = Math.max(0, Math.min(e.getX(), maxThumb.getCenterX() - 12));
                minVal.set(minBound + (newX / sliderWidth) * (maxBound - minBound));
            };
            minThumb.setOnMouseDragged(dragMin);
            minThumb.setOnMousePressed(dragMin);

            javafx.event.EventHandler<javafx.scene.input.MouseEvent> dragMax = e -> {
                double newX = Math.max(minThumb.getCenterX() + 12, Math.min(e.getX(), sliderWidth));
                maxVal.set(minBound + (newX / sliderWidth) * (maxBound - minBound));
            };
            maxThumb.setOnMouseDragged(dragMax);
            maxThumb.setOnMousePressed(dragMax);
        }
    }

    /** Helper to add scrollable month/year picker to a DatePicker */
    private static void customizeDatePicker(javafx.scene.control.DatePicker dp) {
        dp.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (!isShowing) return;
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.skin.DatePickerSkin skin = (javafx.scene.control.skin.DatePickerSkin) dp.getSkin();
                if (skin == null) return;
                javafx.scene.Node popup = skin.getPopupContent();
                if (popup == null) return;

                java.util.Set<javafx.scene.Node> spinners = popup.lookupAll(".spinner");
                int idx = 0;
                for (javafx.scene.Node spinner : spinners) {
                    if (!(spinner instanceof javafx.scene.layout.HBox hbox)) continue;
                    Label lbl = null;
                    for (javafx.scene.Node child : hbox.getChildren()) {
                        if (child instanceof Label l) { lbl = l; break; }
                    }
                    if (lbl == null) continue;

                    final boolean isMonth = (idx == 0);
                    idx++;
                    final Label clickLabel = lbl;

                    if (clickLabel.getUserData() != null && "customized".equals(clickLabel.getUserData())) continue;
                    clickLabel.setUserData("customized");

                    // Add up/down triangle arrows as graphic on the label
                    javafx.scene.shape.Polygon upArrow = new javafx.scene.shape.Polygon(0, 4, 3.5, 0, 7, 4);
                    upArrow.setFill(javafx.scene.paint.Color.web("#78909C"));
                    javafx.scene.shape.Polygon downArrow = new javafx.scene.shape.Polygon(0, 0, 3.5, 4, 7, 0);
                    downArrow.setFill(javafx.scene.paint.Color.web("#78909C"));
                    VBox arrowBox = new VBox(1, upArrow, downArrow);
                    arrowBox.setAlignment(Pos.CENTER);

                    clickLabel.setGraphic(arrowBox);
                    clickLabel.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
                    clickLabel.setCursor(javafx.scene.Cursor.HAND);

                    // Click handler for showing scrollable picker
                    javafx.event.EventHandler<javafx.scene.input.MouseEvent> showPicker = me -> {
                        javafx.stage.Popup selectorPopup = new javafx.stage.Popup();
                        selectorPopup.setAutoHide(true);

                        VBox listBox = new VBox(2);
                        listBox.setPadding(new Insets(6));
                        listBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #B0BEC5; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 8, 0, 0, 3);");

                        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(listBox);
                        sp.setFitToWidth(true);
                        sp.setStyle("-fx-background: white; -fx-background-color: transparent; -fx-border-color: transparent;");
                        sp.setPrefViewportHeight(200);
                        sp.setPrefWidth(120);

                        if (isMonth) {
                            String[] months = {"Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                                "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"};
                            java.time.LocalDate current = dp.getValue() != null ? dp.getValue() : java.time.LocalDate.now();
                            for (int i = 0; i < months.length; i++) {
                                final int monthIdx = i + 1;
                                Label item = new Label(months[i]);
                                item.setPrefWidth(100);
                                item.setPadding(new Insets(6, 10, 6, 10));
                                item.setCursor(javafx.scene.Cursor.HAND);
                                boolean selected = (current.getMonthValue() == monthIdx);
                                item.setStyle(selected
                                    ? "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 13px; -fx-font-weight: bold;"
                                    : "-fx-background-color: transparent; -fx-text-fill: #37474F; -fx-background-radius: 6; -fx-font-size: 13px;");
                                item.setOnMouseEntered(e -> {
                                    if (current.getMonthValue() != monthIdx) item.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1976D2; -fx-background-radius: 6; -fx-font-size: 13px;");
                                });
                                item.setOnMouseExited(e -> {
                                    boolean sel = (current.getMonthValue() == monthIdx);
                                    item.setStyle(sel
                                        ? "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 13px; -fx-font-weight: bold;"
                                        : "-fx-background-color: transparent; -fx-text-fill: #37474F; -fx-background-radius: 6; -fx-font-size: 13px;");
                                });
                                item.setOnMouseClicked(e -> {
                                    java.time.LocalDate cur = dp.getValue() != null ? dp.getValue() : java.time.LocalDate.now();
                                    int maxDay = java.time.YearMonth.of(cur.getYear(), monthIdx).lengthOfMonth();
                                    int day = Math.min(cur.getDayOfMonth(), maxDay);
                                    dp.setValue(java.time.LocalDate.of(cur.getYear(), monthIdx, day));
                                    selectorPopup.hide();
                                });
                                listBox.getChildren().add(item);
                            }
                        } else {
                            int currentYear = dp.getValue() != null ? dp.getValue().getYear() : java.time.LocalDate.now().getYear();
                            int startYear = currentYear - 50;
                            int endYear = currentYear + 10;
                            int scrollToIdx = 0;
                            java.util.List<Label> items = new java.util.ArrayList<>();
                            for (int y = startYear; y <= endYear; y++) {
                                final int yr = y;
                                Label item = new Label(String.valueOf(yr));
                                item.setPrefWidth(100);
                                item.setPadding(new Insets(6, 10, 6, 10));
                                item.setCursor(javafx.scene.Cursor.HAND);
                                boolean selected = (yr == currentYear);
                                item.setStyle(selected
                                    ? "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 13px; -fx-font-weight: bold;"
                                    : "-fx-background-color: transparent; -fx-text-fill: #37474F; -fx-background-radius: 6; -fx-font-size: 13px;");
                                item.setOnMouseEntered(e -> {
                                    if (yr != currentYear) item.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1976D2; -fx-background-radius: 6; -fx-font-size: 13px;");
                                });
                                item.setOnMouseExited(e -> {
                                    boolean sel = (yr == currentYear);
                                    item.setStyle(sel
                                        ? "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 13px; -fx-font-weight: bold;"
                                        : "-fx-background-color: transparent; -fx-text-fill: #37474F; -fx-background-radius: 6; -fx-font-size: 13px;");
                                });
                                item.setOnMouseClicked(e -> {
                                    java.time.LocalDate cur = dp.getValue() != null ? dp.getValue() : java.time.LocalDate.now();
                                    int maxDay = java.time.YearMonth.of(yr, cur.getMonthValue()).lengthOfMonth();
                                    int day = Math.min(cur.getDayOfMonth(), maxDay);
                                    dp.setValue(java.time.LocalDate.of(yr, cur.getMonthValue(), day));
                                    selectorPopup.hide();
                                });
                                if (selected) scrollToIdx = y - startYear;
                                items.add(item);
                                listBox.getChildren().add(item);
                            }
                            final int scrollIdx = scrollToIdx;
                            javafx.application.Platform.runLater(() -> {
                                double total = items.size();
                                if (total > 0) sp.setVvalue(Math.max(0, (scrollIdx - 3.0) / total));
                            });
                        }

                        VBox popupContent = new VBox(sp);
                        popupContent.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
                        selectorPopup.getContent().add(popupContent);

                        javafx.geometry.Bounds screenBounds = clickLabel.localToScreen(clickLabel.getBoundsInLocal());
                        selectorPopup.show(clickLabel, screenBounds.getMinX(), screenBounds.getMaxY() + 2);
                    };

                    clickLabel.setOnMouseClicked(showPicker);
                }
            });
        });
    }
    


    private void showDashboardScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        // ... (Table construction logic remains similar but compacted for brevity if needed, or kept same) ...
        // To avoid massive diffs, I will keep the content creation logic here but change the FINAL STEP to call switchScene
        
        // Product Table Wrapper
        javafx.scene.Node content = createProductView(stage, user);
        switchScene(stage, user, "Products", "nav-products", content);
    }

    private void showImportOrderScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.Node content = createImportOrderView(stage, user);
        switchScene(stage, user, "Import Goods", "nav-import", content);
    }

    private void showOrderHistoryScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox content = createOrderHistoryView(stage, user);
        switchScene(stage, user, "Order History", "nav-history", content);
    }
    
    private void showSalesScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.Node content = createSalesView(stage, user);
        switchScene(stage, user, "Sales (POS)", "nav-sales", content);
    }
    
    private void showAttributesScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.Node content = createAttributesView(stage, user);
        switchScene(stage, user, "Master Data", "nav-attributes", content);
    }

    private void showOverviewScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox content = createOverviewView(stage, user);
        switchScene(stage, user, "Dashboard", "nav-dashboard", content);
    }

    // --- View Creators (Extracted to keep code clean) ---

    private javafx.scene.Node createAttributesView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.control.TabPane tabPane = new javafx.scene.control.TabPane();
        tabPane.setTabClosingPolicy(javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-padding: 10; -fx-background-color: transparent;");

        // 1. Categories Tab
        javafx.scene.control.Tab catTab = new javafx.scene.control.Tab("Categories");
        catTab.setContent(createSimpleMasterDataView(stage, "Categories", 
            () -> {
                var list = categoryRepository.findAll();
                list.sort(java.util.Comparator.comparing(com.pbl3.project.pbl3_project.entity.Category::getId));
                return list;
            }, 
            name -> categoryRepository.save(new com.pbl3.project.pbl3_project.entity.Category(null, name)),
            id -> categoryRepository.deleteById(id)
        ));

        // 2. Brands Tab
        javafx.scene.control.Tab brandTab = new javafx.scene.control.Tab("Brands");
        brandTab.setContent(createSimpleMasterDataView(stage, "Brands", 
            () -> {
                var list = brandService.getAllBrands();
                list.sort(java.util.Comparator.comparing(com.pbl3.project.pbl3_project.entity.Brand::getId));
                return list;
            },
            name -> brandService.saveBrand(new com.pbl3.project.pbl3_project.entity.Brand(name)),
            brandService::deleteBrand
        ));

        // 3. Suppliers Tab
        javafx.scene.control.Tab supplierTab = new javafx.scene.control.Tab("Suppliers");
        supplierTab.setContent(createSupplierMasterDataView(stage));

        // 4. Origins Tab
        javafx.scene.control.Tab originTab = new javafx.scene.control.Tab("Origins");
        originTab.setContent(createSimpleMasterDataView(stage, "Origins", 
            () -> {
                 var list = originService.getAllOrigins();
                 list.sort(java.util.Comparator.comparing(com.pbl3.project.pbl3_project.entity.Origin::getId));
                 return list;
            },
            name -> originService.saveOrigin(new com.pbl3.project.pbl3_project.entity.Origin(name)),
            originService::deleteOrigin
        ));

        // 5. Units Tab
        javafx.scene.control.Tab unitTab = new javafx.scene.control.Tab("Units");
        unitTab.setContent(createSimpleMasterDataView(stage, "Units", 
            () -> {
                var list = unitService.getAllUnits();
                list.sort(java.util.Comparator.comparing(com.pbl3.project.pbl3_project.entity.Unit::getId));
                return list;
            },
            name -> unitService.saveUnit(new com.pbl3.project.pbl3_project.entity.Unit(name)),
            unitService::deleteUnit
        ));

        tabPane.getTabs().addAll(catTab, brandTab, supplierTab, originTab, unitTab);
        return tabPane;
    }

    // Helper for simple ID/Name entities (Brand, Origin, Unit, Category)
    private <T> VBox createSimpleMasterDataView(Stage stage, String title, 
                                                java.util.function.Supplier<java.util.List<T>> dataFetcher,
                                                java.util.function.Consumer<String> saver,
                                                java.util.function.Consumer<Long> deleter) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(35));
        
        Label header = new Label(title + " Management");
        header.getStyleClass().add("header-label");

        // Expandable Search Bar
        javafx.scene.layout.HBox searchBox = new javafx.scene.layout.HBox(0);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getStyleClass().add("expandable-search-box");
        searchBox.setPrefSize(40, 40);
        searchBox.setMinSize(40, 40);
        searchBox.setMaxSize(40, 40);

        javafx.scene.shape.SVGPath sIcon = new javafx.scene.shape.SVGPath();
        sIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        sIcon.setFill(javafx.scene.paint.Color.web("#90A4AE"));

        javafx.scene.layout.Region sSpacer = new javafx.scene.layout.Region();
        sSpacer.setMinWidth(0); sSpacer.setPrefWidth(0);

        TextField sField = new TextField();
        sField.setPromptText("Search...");
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
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        javafx.collections.ObservableList<T> masterData = javafx.collections.FXCollections.observableArrayList(dataFetcher.get());
        
        searchBox.setOnMouseClicked(ev -> {
            if (searchBox.getMaxWidth() == 40) { sExpand.play(); sField.requestFocus(); }
            else if (ev.getTarget() == sIcon || ev.getTarget() == searchBox) { sField.clear(); root.requestFocus(); sCollapse.play(); }
        });

        javafx.scene.layout.BorderPane topBar = new javafx.scene.layout.BorderPane();
        topBar.setLeft(header);
        topBar.setRight(searchBox);
        javafx.scene.layout.BorderPane.setAlignment(header, Pos.CENTER_LEFT);

        // Search filter
        sField.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV.toLowerCase().trim();
            if (q.isEmpty()) {
                table.setItems(masterData);
            } else {
                javafx.collections.ObservableList<T> filtered = javafx.collections.FXCollections.observableArrayList();
                for (T item : masterData) {
                    try {
                        java.lang.reflect.Method getName = item.getClass().getMethod("getName");
                        String name = (String) getName.invoke(item);
                        if (name != null && name.toLowerCase().contains(q)) filtered.add(item);
                    } catch (Exception ex) { filtered.add(item); }
                }
                table.setItems(filtered);
            }
        });
        
        javafx.scene.control.TableColumn<T, Integer> sttCol = new javafx.scene.control.TableColumn<>("STT");
        sttCol.setSortable(false);
        sttCol.setCellValueFactory(column -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
            table.getItems().indexOf(column.getValue()) + 1));
        
        javafx.scene.control.TableColumn<T, String> nameCol = new javafx.scene.control.TableColumn<>("Name");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        
        Runnable deleteAction = () -> {
            java.util.List<T> selectedItems = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedItems.isEmpty()) return;
            
            if (!showConfirmDialog("Confirm Deletion", "Are you sure you want to delete " + selectedItems.size() + " selected item(s)?")) {
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
                     toastService.showError("Error deleting item: " + ex.getMessage());
                }
            }
            if (deletedCount > 0) {
                toastService.showSuccess("Deleted " + deletedCount + " items!");
                masterData.setAll(dataFetcher.get());
                sField.clear();
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
            deleteItem.setStyle("-fx-text-fill: #F44336;");
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
        
        table.getColumns().addAll(sttCol, nameCol);
        table.setItems(masterData);
        
        // Add Form
        javafx.scene.layout.HBox addBox = new javafx.scene.layout.HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);
        TextField nameField = new TextField();
        nameField.setPromptText("Enter " + title + " Name...");
        Button addBtn = new Button("Add " + title);
        addBtn.getStyleClass().add("success-button");
        addBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) return;
            try {
                saver.accept(nameField.getText());
                toastService.showSuccess("Added " + nameField.getText());
                nameField.clear();
                masterData.setAll(dataFetcher.get());
                sField.clear();
            } catch (Exception ex) {
                toastService.showError("Error: " + ex.getMessage());
            }
        });
        
        addBox.getChildren().addAll(nameField, addBtn);

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, createSortStatusLabel(table), createRowCountBox(table));
        statusBar.setAlignment(Pos.CENTER_RIGHT);
        statusBar.setPadding(new Insets(5, 5, 0, 0));

        root.getChildren().addAll(topBar, addBox, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private VBox createSupplierMasterDataView(Stage stage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(35));
        
        Label header = new Label("Supplier Management");
        header.getStyleClass().add("header-label");

        // Expandable Search Bar
        javafx.scene.layout.HBox searchBox2 = new javafx.scene.layout.HBox(0);
        searchBox2.setAlignment(Pos.CENTER);
        searchBox2.getStyleClass().add("expandable-search-box");
        searchBox2.setPrefSize(40, 40); searchBox2.setMinSize(40, 40); searchBox2.setMaxSize(40, 40);
        javafx.scene.shape.SVGPath sIcon2 = new javafx.scene.shape.SVGPath();
        sIcon2.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        sIcon2.setFill(javafx.scene.paint.Color.web("#90A4AE"));
        javafx.scene.layout.Region sSpacer2 = new javafx.scene.layout.Region();
        sSpacer2.setMinWidth(0); sSpacer2.setPrefWidth(0);
        TextField sField2 = new TextField();
        sField2.setPromptText("Search..."); sField2.getStyleClass().add("search-text-field");
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
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        javafx.collections.ObservableList<com.pbl3.project.pbl3_project.entity.Supplier> supplierMaster = javafx.collections.FXCollections.observableArrayList(
            supplierService.getAllSuppliers().stream()
                .sorted(java.util.Comparator.comparing(com.pbl3.project.pbl3_project.entity.Supplier::getId))
                .toList()
        );

        searchBox2.setOnMouseClicked(ev -> {
            if (searchBox2.getMaxWidth() == 40) { sExpand2.play(); sField2.requestFocus(); }
            else if (ev.getTarget() == sIcon2 || ev.getTarget() == searchBox2) { sField2.clear(); root.requestFocus(); sCollapse2.play(); }
        });
        javafx.scene.layout.BorderPane topBar2 = new javafx.scene.layout.BorderPane();
        topBar2.setLeft(header); topBar2.setRight(searchBox2);
        javafx.scene.layout.BorderPane.setAlignment(header, Pos.CENTER_LEFT);
        
        sField2.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV.toLowerCase().trim();
            if (q.isEmpty()) { table.setItems(supplierMaster); } else {
                javafx.collections.ObservableList<com.pbl3.project.pbl3_project.entity.Supplier> filtered = javafx.collections.FXCollections.observableArrayList();
                for (com.pbl3.project.pbl3_project.entity.Supplier s : supplierMaster) {
                    if ((s.getName() != null && s.getName().toLowerCase().contains(q)) ||
                        (s.getPhone() != null && s.getPhone().toLowerCase().contains(q)) ||
                        (s.getAddress() != null && s.getAddress().toLowerCase().contains(q))) filtered.add(s);
                }
                table.setItems(filtered);
            }
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, Integer> sttCol = new javafx.scene.control.TableColumn<>("STT");
        sttCol.setSortable(false);
        sttCol.setCellValueFactory(column -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
            table.getItems().indexOf(column.getValue()) + 1));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, String> nameCol = new javafx.scene.control.TableColumn<>("Name");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, String> phoneCol = new javafx.scene.control.TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("phone"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, String> addrCol = new javafx.scene.control.TableColumn<>("Address");
        addrCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("address"));
        
        Runnable deleteAction = () -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Supplier> selectedItems = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedItems.isEmpty()) return;

            if (!showConfirmDialog("Confirm Deletion", "Are you sure you want to delete " + selectedItems.size() + " selected supplier(s)?")) {
                return;
            }

            int deletedCount = 0;
            for (com.pbl3.project.pbl3_project.entity.Supplier item : selectedItems) {
                try {
                     supplierService.deleteSupplier(item.getId());
                     deletedCount++;
                } catch (Exception ex) {
                     toastService.showError("Error deleting supplier: " + ex.getMessage());
                }
            }
            
            if (deletedCount > 0) {
                 toastService.showSuccess("Deleted " + deletedCount + " suppliers!");
                 supplierMaster.setAll(supplierService.getAllSuppliers().stream()
                     .sorted(java.util.Comparator.comparing(com.pbl3.project.pbl3_project.entity.Supplier::getId))
                     .toList());
                 sField2.clear();
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
            deleteItem.setStyle("-fx-text-fill: #F44336;");
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
        
        table.getColumns().addAll(sttCol, nameCol, phoneCol, addrCol);
        table.setItems(supplierMaster);
        
        // Add Form
        javafx.scene.layout.HBox addBox = new javafx.scene.layout.HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);
        TextField nameField = new TextField(); nameField.setPromptText("Name");
        TextField phoneField = new TextField(); phoneField.setPromptText("Phone");
        TextField addrField = new TextField(); addrField.setPromptText("Address");
        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("success-button");
        addBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) return;
            try {
                supplierService.saveSupplier(new com.pbl3.project.pbl3_project.entity.Supplier(nameField.getText(), phoneField.getText(), addrField.getText()));
                toastService.showSuccess("Added!");
                nameField.clear(); phoneField.clear(); addrField.clear();
                supplierMaster.setAll(supplierService.getAllSuppliers().stream()
                    .sorted(java.util.Comparator.comparing(com.pbl3.project.pbl3_project.entity.Supplier::getId))
                    .toList());
                sField2.clear();
            } catch (Exception ex) {
                toastService.showError("Error: " + ex.getMessage());
            }
        });
        
        addBox.getChildren().addAll(nameField, phoneField, addrField, addBtn);

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, createSortStatusLabel(table), createRowCountBox(table));
        statusBar.setAlignment(Pos.CENTER_RIGHT);
        statusBar.setPadding(new Insets(5, 5, 0, 0));

        root.getChildren().addAll(topBar2, addBox, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private javafx.scene.Node createProductView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane();
        
        // --- 1. Available Products (Table View) ---
        VBox productListView = new VBox(10);
        productListView.setPadding(new Insets(35));
        productListView.setVisible(false); // Hidden initially
        
        javafx.scene.layout.BorderPane toolbar = new javafx.scene.layout.BorderPane();
        toolbar.setPadding(new Insets(0, 0, 10, 0));
        
        javafx.scene.layout.HBox leftBox = new javafx.scene.layout.HBox(15);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        
        Button backBtn = new Button("⬅");
        backBtn.getStyleClass().add("back-nav-button");
        
        Label categoryTitle = new Label("All Products");
        categoryTitle.getStyleClass().add("product-header-title");
        
        leftBox.getChildren().addAll(backBtn, categoryTitle);
        toolbar.setLeft(leftBox);
        
        // Expandable Search Bar for filtering within category
        javafx.scene.layout.HBox searchBox = new javafx.scene.layout.HBox(0);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getStyleClass().add("expandable-search-box");
        searchBox.setPrefSize(40, 40);
        searchBox.setMinSize(40, 40);
        searchBox.setMaxSize(40, 40); // Initial collapsed size
        
        javafx.scene.shape.SVGPath searchIcon = new javafx.scene.shape.SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.setFill(javafx.scene.paint.Color.web("#90A4AE"));
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        spacer.setMinWidth(0);
        spacer.setPrefWidth(0);

        TextField categorySearchField = new TextField();
        categorySearchField.setPromptText("Search...");
        categorySearchField.getStyleClass().add("search-text-field");
        categorySearchField.setMinWidth(0);
        categorySearchField.setMaxWidth(0);
        categorySearchField.setPrefWidth(0); // Initially hidden width
        categorySearchField.setOpacity(0);   // Initially invisible
        
        searchBox.getChildren().addAll(searchIcon, spacer, categorySearchField);
        
        // Expansion Animation
        javafx.animation.Timeline expandAnim = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(searchBox.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchBox.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(spacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        
        // Collapse Animation
        javafx.animation.Timeline collapseAnim = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(searchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(spacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        
        searchBox.setOnMouseClicked(e -> {
            if (searchBox.getMaxWidth() == 40) {
                expandAnim.play();
                categorySearchField.requestFocus();
            } else if (e.getTarget() == searchIcon || e.getTarget() == searchBox) {
                categorySearchField.clear();
                if (searchBox.getParent() != null) {
                    searchBox.getParent().requestFocus();
                }
                collapseAnim.play();
            }
        });

        // Filter Button (Brand)
        javafx.scene.layout.HBox filterBox = new javafx.scene.layout.HBox();
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("expandable-search-box");
        filterBox.setPrefSize(40, 40); filterBox.setMinSize(40, 40); filterBox.setMaxSize(40, 40);
        filterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath filterIcon = new javafx.scene.shape.SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.setFill(javafx.scene.paint.Color.web("#90A4AE"));
        filterBox.getChildren().add(filterIcon);

        Button addButton = new Button();
        addButton.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(34,197,94,0.3), 12, 0, 0, 4); -fx-padding: 0;");
        addButton.setPrefSize(40, 40);
        addButton.setMinSize(40, 40);
        addButton.setMaxSize(40, 40);
        addButton.setCursor(javafx.scene.Cursor.HAND);
        
        javafx.scene.shape.SVGPath addPlusIcon = new javafx.scene.shape.SVGPath();
        addPlusIcon.setContent("M12 5v14M5 12h14");
        addPlusIcon.setStroke(javafx.scene.paint.Color.WHITE);
        addPlusIcon.setStrokeWidth(2.5);
        addPlusIcon.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        
        javafx.scene.layout.StackPane addIconWrapper = new javafx.scene.layout.StackPane(addPlusIcon);
        addIconWrapper.setPrefSize(40, 40);
        addIconWrapper.setMinSize(40, 40);
        addIconWrapper.setMaxSize(40, 40);
        
        Label addLabelText = new Label("Add Product");
        addLabelText.setStyle("-fx-font-family: 'Be Vietnam Pro', 'Inter', sans-serif; -fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: white; -fx-padding: 0;");
        addLabelText.setOpacity(0);
        addLabelText.setTranslateX(-10);
        
        javafx.scene.layout.StackPane addBtnContent = new javafx.scene.layout.StackPane(addIconWrapper, addLabelText);
        javafx.scene.layout.StackPane.setAlignment(addIconWrapper, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setAlignment(addLabelText, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setMargin(addLabelText, new Insets(0, 0, 0, 36));
        
        javafx.scene.shape.Rectangle btnClip = new javafx.scene.shape.Rectangle();
        btnClip.setArcWidth(40);
        btnClip.setArcHeight(40);
        btnClip.widthProperty().bind(addButton.widthProperty());
        btnClip.heightProperty().bind(addButton.heightProperty());
        addBtnContent.setClip(btnClip);
        
        addButton.setGraphic(addBtnContent);
        addButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        addButton.setAlignment(Pos.CENTER_LEFT);
        
        javafx.animation.Timeline hoverInAddBtn = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(210),
                new javafx.animation.KeyValue(addButton.minWidthProperty(), 150, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(addButton.prefWidthProperty(), 150, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(addButton.maxWidthProperty(), 150, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(addPlusIcon.rotateProperty(), 90, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(addLabelText.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(addIconWrapper.translateXProperty(), 6, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(addLabelText.translateXProperty(), 6, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        
        javafx.animation.Timeline hoverOutAddBtn = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(210),
                new javafx.animation.KeyValue(addButton.minWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(addButton.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(addButton.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(addPlusIcon.rotateProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(addLabelText.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(addIconWrapper.translateXProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(addLabelText.translateXProperty(), -10, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        
        addButton.setOnMouseEntered(e -> {
            addButton.setStyle("-fx-background-color: #16a34a; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(34,197,94,0.4), 20, 0, 0, 8); -fx-padding: 0;");
            hoverOutAddBtn.stop();
            hoverInAddBtn.play();
        });
        
        addButton.setOnMouseExited(e -> {
            addButton.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(34,197,94,0.3), 12, 0, 0, 4); -fx-padding: 0;");
            hoverInAddBtn.stop();
            hoverOutAddBtn.play();
        });
        
        addButton.setOnMousePressed(e -> {
            addButton.setScaleX(0.95);
            addButton.setScaleY(0.95);
        });
        
        addButton.setOnMouseReleased(e -> {
            addButton.setScaleX(1.0);
            addButton.setScaleY(1.0);
        });
        
        javafx.scene.layout.HBox rightContainer = new javafx.scene.layout.HBox(15, filterBox, searchBox, addButton);
        rightContainer.setAlignment(Pos.CENTER_RIGHT);
        toolbar.setRight(rightContainer);
        
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Product> table = new javafx.scene.control.TableView<>();
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Long> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> nameCol = new javafx.scene.control.TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        nameCol.setCellFactory(col -> new javafx.scene.control.TableCell<com.pbl3.project.pbl3_project.entity.Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #1976D2; -fx-font-weight: bold;");
                }
            }
        });
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Double> priceCol = new javafx.scene.control.TableColumn<>("Price");
        priceCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Integer> qtyCol = new javafx.scene.control.TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));

        Runnable deleteAction = () -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Product> selectedItems = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedItems.isEmpty()) return;

            if (!showConfirmDialog("Confirm Deletion", "Are you sure you want to delete " + selectedItems.size() + " selected product(s)?")) {
                return;
            }

            int deletedCount = 0;
            for (com.pbl3.project.pbl3_project.entity.Product product : selectedItems) {
                try {
                    productService.deleteProduct(product.getId(), user);
                    table.getItems().remove(product);
                    deletedCount++;
                } catch (Exception ex) {
                     toastService.showError("Could not delete product: " + ex.getMessage());
                }
            }
            if (deletedCount > 0) {
                 toastService.showSuccess("Deleted " + deletedCount + " products!");
                 if (productListView.getUserData() instanceof Runnable) ((Runnable)productListView.getUserData()).run();
            }
        };

        table.setOnKeyPressed(event -> {
            javafx.scene.input.KeyCode code = event.getCode();
            if (code == javafx.scene.input.KeyCode.DELETE || code == javafx.scene.input.KeyCode.BACK_SPACE || code == javafx.scene.input.KeyCode.ENTER) {
                deleteAction.run();
            }
        });

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Product> row = new javafx.scene.control.TableRow<>();
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
            
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    com.pbl3.project.pbl3_project.entity.Product product = row.getItem();
                    showProductDialog(stage, product, product.getCategory(), user, () -> {
                         if (productListView.getUserData() instanceof Runnable) ((Runnable)productListView.getUserData()).run();
                    });
                }
            });
            
            javafx.scene.control.MenuItem editItem = new javafx.scene.control.MenuItem("Edit");
            editItem.setStyle("-fx-text-fill: #1976D2;");
            editItem.setOnAction(event -> {
                com.pbl3.project.pbl3_project.entity.Product product = row.getItem();
                showProductDialog(stage, product, product.getCategory(), user, () -> {
                     if (productListView.getUserData() instanceof Runnable) ((Runnable)productListView.getUserData()).run();
                });
            });

            javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete");
            deleteItem.setStyle("-fx-text-fill: #F44336;");
            deleteItem.setOnAction(event -> {
                if (table.getSelectionModel().getSelectedItems().isEmpty() && row.getItem() != null) {
                    table.getSelectionModel().select(row.getItem());
                }
                deleteAction.run();
            });

            contextMenu.getItems().addAll(editItem, new javafx.scene.control.SeparatorMenuItem(), deleteItem);
            
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((javafx.scene.control.ContextMenu)null)
                .otherwise(contextMenu)
            );
            return row;
        });
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> skuCol = new javafx.scene.control.TableColumn<>("SKU");
        skuCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("sku"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> brandCol = new javafx.scene.control.TableColumn<>("Brand");
        brandCol.setCellValueFactory(cell -> {
             var brand = cell.getValue().getBrand();
             return new javafx.beans.property.SimpleStringProperty(brand != null ? brand.getName() : "-");
        });

        table.getColumns().addAll(idCol, skuCol, nameCol, brandCol, priceCol, qtyCol);

        javafx.scene.layout.HBox productStatusBar = new javafx.scene.layout.HBox(15, createSortStatusLabel(table), createRowCountBox(table));
        productStatusBar.setAlignment(Pos.CENTER_RIGHT);
        productStatusBar.setPadding(new Insets(5, 5, 0, 0));

        productListView.getChildren().addAll(toolbar, table, productStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);


        // --- 2. Category Overview (Grid View) ---
        VBox categoryView = new VBox(20);
        categoryView.setPadding(new Insets(35));
        categoryView.setAlignment(Pos.TOP_CENTER);
        
        Label title = new Label("Product Categories");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        javafx.scene.layout.FlowPane categoryGrid = new javafx.scene.layout.FlowPane();
        categoryGrid.setHgap(20);
        categoryGrid.setVgap(20);
        categoryGrid.setAlignment(Pos.CENTER);
        
        Label categoryCountLabel = new Label("Total: 0 Categories");
        categoryCountLabel.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 12px;");
        javafx.scene.layout.HBox categoryStatusBar = new javafx.scene.layout.HBox(categoryCountLabel);
        categoryStatusBar.setAlignment(Pos.CENTER_RIGHT);
        categoryStatusBar.setPadding(new Insets(5, 5, 0, 0));

        categoryView.getChildren().addAll(title, categoryGrid, categoryStatusBar);
        
        // --- Logic: Navigation & Refresh ---
        final com.pbl3.project.pbl3_project.entity.Category[] selectedCategory = {null};

        // Wire up filter button (after table & selectedCategory are declared)
        javafx.stage.Popup filterPopup = new javafx.stage.Popup();
        filterPopup.setAutoHide(true);

        filterBox.setOnMouseClicked(ev -> {
            if (filterPopup.isShowing()) { filterPopup.hide(); return; }

            // Get current products in table for range calculations
            javafx.collections.ObservableList<com.pbl3.project.pbl3_project.entity.Product> currentItems = table.getItems();
            double maxPrice = 0; int maxQty = 0;
            for (com.pbl3.project.pbl3_project.entity.Product p : currentItems) {
                if (p.getPrice() != null && p.getPrice() > maxPrice) maxPrice = p.getPrice();
                if (p.getQuantity() != null && p.getQuantity() > maxQty) maxQty = p.getQuantity();
            }
            // Also check master data from category
            if (selectedCategory[0] != null) {
                for (com.pbl3.project.pbl3_project.entity.Product p : productService.getAllProducts()) {
                    if (p.getCategory() != null && p.getCategory().getId().equals(selectedCategory[0].getId())) {
                        if (p.getPrice() != null && p.getPrice() > maxPrice) maxPrice = p.getPrice();
                        if (p.getQuantity() != null && p.getQuantity() > maxQty) maxQty = p.getQuantity();
                    }
                }
            }
            if (maxPrice == 0) maxPrice = 1000;
            if (maxQty == 0) maxQty = 100;

            VBox popupContent = new VBox(12);
            popupContent.setPadding(new Insets(16));
            popupContent.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 4); " +
                "-fx-border-color: #E0E0E0; -fx-border-radius: 12;");
            popupContent.setPrefWidth(280);

            // --- Brand Section ---
            Label brandTitle = new Label("Brands");
            brandTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");

            javafx.scene.control.CheckBox allBrandsCb = new javafx.scene.control.CheckBox("All Brands");
            allBrandsCb.setSelected(true);
            allBrandsCb.setStyle("-fx-font-size: 13px; -fx-text-fill: #455A64; -fx-cursor: hand;");

            VBox brandCheckboxes = new VBox(6);
            brandCheckboxes.setPadding(new Insets(5, 5, 5, 10));
            java.util.List<javafx.scene.control.CheckBox> brandCbs = new java.util.ArrayList<>();
            java.util.List<com.pbl3.project.pbl3_project.entity.Brand> brands = brandService.getAllBrands();
            for (com.pbl3.project.pbl3_project.entity.Brand b : brands) {
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(b.getName());
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: #455A64; -fx-cursor: hand;");
                cb.selectedProperty().addListener((obs, ov, nv) -> {
                    boolean allChecked = brandCbs.stream().allMatch(javafx.scene.control.CheckBox::isSelected);
                    allBrandsCb.setSelected(allChecked);
                });
                brandCbs.add(cb);
                brandCheckboxes.getChildren().add(cb);
            }
            allBrandsCb.setOnAction(ae -> {
                for (javafx.scene.control.CheckBox cb : brandCbs) cb.setSelected(allBrandsCb.isSelected());
            });

            javafx.scene.control.ScrollPane brandScroll = new javafx.scene.control.ScrollPane(brandCheckboxes);
            brandScroll.setFitToWidth(true);
            brandScroll.setMaxHeight(140);
            brandScroll.setStyle("-fx-background-color: transparent; -fx-background: white; -fx-border-color: #ECEFF1; -fx-border-radius: 4;");

        // ...
            // --- Range Slider Custom Helper ---
            // (Removed inline RangeSlider class)

            // --- Price Section ---
            Label priceTitle = new Label("Price Range");
            priceTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            Label priceLabel = new Label(String.format("0 - %,.0f VND", maxPrice));
            priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1976D2; -fx-font-weight: bold;");

            RangeSlider priceSlider = new RangeSlider(0, maxPrice, 0, maxPrice, 240);
            priceSlider.minVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%,.0f - %,.0f VND", nv.doubleValue(), priceSlider.maxVal.get())));
            priceSlider.maxVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%,.0f - %,.0f VND", priceSlider.minVal.get(), nv.doubleValue())));

            // --- Quantity Section ---
            Label qtyTitle = new Label("Quantity Range");
            qtyTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            Label qtyLabel = new Label("0 - " + maxQty);
            qtyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1976D2; -fx-font-weight: bold;");

            RangeSlider qtySlider = new RangeSlider(0, maxQty, 0, maxQty, 240);
            qtySlider.minVal.addListener((o, ov, nv) -> qtyLabel.setText(String.format("%d - %d", nv.intValue(), (int)qtySlider.maxVal.get())));
            qtySlider.maxVal.addListener((o, ov, nv) -> qtyLabel.setText(String.format("%d - %d", (int)qtySlider.minVal.get(), nv.intValue())));

            // --- Buttons ---
            javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
            btnRow.setAlignment(Pos.CENTER_RIGHT);

            final double fMaxPrice = maxPrice;
            final int fMaxQty = maxQty;

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(ae -> {
                filterBox.setStyle("");
                allBrandsCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : brandCbs) cb.setSelected(true);
                priceSlider.minVal.set(0); priceSlider.maxVal.set(fMaxPrice);
                qtySlider.minVal.set(0); qtySlider.maxVal.set(fMaxQty);
                // Reset to category data
                if (selectedCategory[0] != null) {
                    java.util.List<com.pbl3.project.pbl3_project.entity.Product> filtered = productService.getAllProducts().stream()
                        .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(selectedCategory[0].getId()))
                        .collect(java.util.stream.Collectors.toList());
                    table.setItems(javafx.collections.FXCollections.observableArrayList(filtered));
                }
                filterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(ae -> {
                java.util.Set<String> selectedBrands = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : brandCbs) {
                    if (cb.isSelected()) selectedBrands.add(cb.getText());
                }
                double pMin = priceSlider.minVal.get();
                double pMax = priceSlider.maxVal.get();
                int qMin = (int) qtySlider.minVal.get();
                int qMax = (int) qtySlider.maxVal.get();

                java.util.List<com.pbl3.project.pbl3_project.entity.Product> source = productService.getAllProducts().stream()
                    .filter(p -> p.getCategory() != null && selectedCategory[0] != null && p.getCategory().getId().equals(selectedCategory[0].getId()))
                    .collect(java.util.stream.Collectors.toList());

                java.util.List<com.pbl3.project.pbl3_project.entity.Product> result = source.stream()
                    .filter(p -> {
                        String bName = p.getBrand() != null ? p.getBrand().getName() : null;
                        return bName == null || selectedBrands.contains(bName);
                    })
                    .filter(p -> {
                        double price = p.getPrice() != null ? p.getPrice() : 0;
                        return price >= pMin && price <= pMax;
                    })
                    .filter(p -> {
                        int qty = p.getQuantity() != null ? p.getQuantity() : 0;
                        return qty >= qMin && qty <= qMax;
                    })
                    .collect(java.util.stream.Collectors.toList());

                table.setItems(javafx.collections.FXCollections.observableArrayList(result));

                boolean hasFilter = !allBrandsCb.isSelected() || pMin > 0 || pMax < fMaxPrice || qMin > 0 || qMax < fMaxQty;
                filterBox.setStyle(hasFilter ? "-fx-border-color: #1976D2;" : "");
                filterPopup.hide();
            });

            btnRow.getChildren().addAll(resetBtn, applyBtn);

            // Separators
            javafx.scene.control.Separator sep1 = new javafx.scene.control.Separator();
            javafx.scene.control.Separator sep2 = new javafx.scene.control.Separator();

            popupContent.getChildren().addAll(
                brandTitle, allBrandsCb, brandScroll, sep1,
                priceTitle, priceLabel, priceSlider, sep2,
                qtyTitle, qtyLabel, qtySlider,
                btnRow
            );

            filterPopup.getContent().clear();
            filterPopup.getContent().add(popupContent);

            javafx.geometry.Bounds bounds = filterBox.localToScreen(filterBox.getBoundsInLocal());
            filterPopup.show(filterBox, bounds.getMinX() - 240 + 40, bounds.getMaxY() + 5);
        });
        
        Runnable loadCategories = () -> {
            categoryGrid.getChildren().clear();
            java.util.List<com.pbl3.project.pbl3_project.entity.Category> categories = categoryRepository.findAll();
            java.util.List<com.pbl3.project.pbl3_project.entity.Product> allProducts = productService.getAllProducts(); // Cache for counts
            
            for (com.pbl3.project.pbl3_project.entity.Category cat : categories) {
                long count = allProducts.stream().filter(p -> p.getCategory() != null && p.getCategory().getId().equals(cat.getId())).count();
                
                VBox card = new VBox(10);
                card.getStyleClass().add("category-card");
                card.setPrefSize(200, 120);
                
                Label nameLbl = new Label(cat.getName());
                nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
                
                Label countLbl = new Label(count + " Products");
                countLbl.getStyleClass().add("category-card-count");
                
                card.getChildren().addAll(nameLbl, countLbl);
                
                card.setOnMouseClicked(e -> {
                    // Navigate to Product List
                    selectedCategory[0] = cat;
                    categoryTitle.setText(cat.getName());
                    categorySearchField.clear();
                    categoryView.setVisible(false);
                    productListView.setVisible(true);
                    
                    // Filter Table
                    java.util.List<com.pbl3.project.pbl3_project.entity.Product> filtered = productService.getAllProducts().stream()
                        .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(cat.getId()))
                        .collect(java.util.stream.Collectors.toList());
                    table.setItems(javafx.collections.FXCollections.observableArrayList(filtered));
                });
                
                categoryGrid.getChildren().add(card);
            }
            categoryCountLabel.setText("Total: " + categories.size() + " Categories");
        };
        
        // Refresh product list logic (store as userData)
        Runnable refreshProductList = () -> {
            if (selectedCategory[0] != null) {
                String searchText = categorySearchField.getText() == null ? "" : categorySearchField.getText().trim().toLowerCase();
                final String query = searchText;
                java.util.List<com.pbl3.project.pbl3_project.entity.Product> filtered = productService.getAllProducts().stream()
                        .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(selectedCategory[0].getId()))
                        .filter(p -> {
                            if (query.isEmpty()) return true;
                            boolean matchName = p.getName() != null && p.getName().toLowerCase().contains(query);
                            boolean matchSku = p.getSku() != null && p.getSku().toLowerCase().contains(query);
                            boolean matchBrand = p.getBrand() != null && p.getBrand().getName() != null && p.getBrand().getName().toLowerCase().contains(query);
                            return matchName || matchSku || matchBrand;
                        })
                        .collect(java.util.stream.Collectors.toList());
                table.setItems(javafx.collections.FXCollections.observableArrayList(filtered));
            }
        };
        productListView.setUserData(refreshProductList);
        
        // Live search: filter as user types
        categorySearchField.textProperty().addListener((obs, old, val) -> refreshProductList.run());
        
        backBtn.setOnAction(e -> {
            productListView.setVisible(false);
            categoryView.setVisible(true);
            selectedCategory[0] = null;
            categorySearchField.clear();
            loadCategories.run(); // Refresh counts
        });
        
        addButton.setOnAction(e -> showProductDialog(stage, null, selectedCategory[0], user, () -> {
            // After add, refresh current view
            if (productListView.isVisible()) refreshProductList.run();
            else loadCategories.run();
        }));

        loadCategories.run();
        root.getChildren().addAll(categoryView, productListView);
        enableDeselectOnOutsideClick(root, table);
        return root;
    }
    
    private VBox createOverviewView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        // --- Stat Cards Row ---
        javafx.scene.layout.HBox statsRow = new javafx.scene.layout.HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        
        // Get stats data
        java.util.Map<String, Object> dailyStats = reportService.getDailyStats();
        double todayRevenue = (Double) dailyStats.get("revenue");
        long todayOrders = (Long) dailyStats.get("orders");
        long lowStockCount = reportService.countLowStockProducts();
        
        // Create 3 stat cards
        VBox revenueCard = createDashboardCard("Today's Revenue", 
            String.format("%,.0f VND", todayRevenue), "#4CAF50");
        VBox ordersCard = createDashboardCard("Orders Today", 
            String.valueOf(todayOrders), "#2196F3");
        VBox lowStockCard = createDashboardCard("Low Stock Items", 
            String.valueOf(lowStockCount), lowStockCount > 0 ? "#F44336" : "#9E9E9E");
        
        javafx.scene.layout.HBox.setHgrow(revenueCard, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(ordersCard, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(lowStockCard, javafx.scene.layout.Priority.ALWAYS);
        
        statsRow.getChildren().addAll(revenueCard, ordersCard, lowStockCard);
        
        // --- 7-Day Revenue Chart ---
        Label chartTitle = new Label("Revenue - Last 7 Days");
        chartTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
        
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        xAxis.setLabel("Date");
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        yAxis.setLabel("Revenue (VND)");
        
        javafx.scene.chart.BarChart<String, Number> barChart = new javafx.scene.chart.BarChart<>(xAxis, yAxis);
        barChart.setTitle(null);
        barChart.setLegendVisible(false);
        barChart.setAnimated(true);
        barChart.setCategoryGap(20);
        barChart.setBarGap(5);
        barChart.setMaxHeight(250);
        
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Revenue");
        
        java.util.Map<String, Double> last7Days = reportService.getLast7DaysRevenue();
        for (java.util.Map.Entry<String, Double> entry : last7Days.entrySet()) {
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        barChart.getData().add(series);
        
        // Style chart bars with green color
        barChart.lookupAll(".chart-bar").forEach(node -> 
            node.setStyle("-fx-bar-fill: #4CAF50;"));

        // --- LOW STOCK ALERT PANEL ---
        java.util.List<com.pbl3.project.pbl3_project.entity.Product> lowStockProducts = reportService.getLowStockProducts();

        VBox lowStockPanel = new VBox(10);
        lowStockPanel.setPadding(new Insets(15));
        lowStockPanel.setStyle("-fx-background-color: #FFF3F3; -fx-background-radius: 12; " +
            "-fx-border-color: #F44336; -fx-border-radius: 12; -fx-border-width: 2; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(244,67,54,0.15), 8, 0, 0, 3);");

        // Create SVG check icon (green, minimalist - matching Figma)
        javafx.scene.shape.SVGPath checkIcon = new javafx.scene.shape.SVGPath();
        checkIcon.setContent("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z");
        checkIcon.setFill(javafx.scene.paint.Color.web("#00C853"));
        checkIcon.setScaleX(0.8); checkIcon.setScaleY(0.8);

        // Create SVG cross icon (red/pink, minimalist - matching Figma)
        javafx.scene.shape.SVGPath crossIcon = new javafx.scene.shape.SVGPath();
        crossIcon.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
        crossIcon.setFill(javafx.scene.paint.Color.web("#FF1744"));
        crossIcon.setScaleX(0.8); crossIcon.setScaleY(0.8);

        Label lowStockTitle = new Label();

        if (lowStockProducts.isEmpty()) {
            javafx.scene.layout.HBox titleRow = new javafx.scene.layout.HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            lowStockTitle.setText("Stock Status");
            lowStockTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #388E3C;");
            titleRow.getChildren().addAll(checkIcon, lowStockTitle);

            Label noAlert = new Label("All products are well-stocked!");
            noAlert.setStyle("-fx-font-size: 13px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            lowStockPanel.setStyle("-fx-background-color: #F1F8E9; -fx-background-radius: 12; " +
                "-fx-border-color: #4CAF50; -fx-border-radius: 12; -fx-border-width: 2; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(76,175,80,0.15), 8, 0, 0, 3);");
            lowStockPanel.getChildren().addAll(titleRow, noAlert);
        } else {
            javafx.scene.layout.HBox titleRow = new javafx.scene.layout.HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            lowStockTitle.setText("Low Stock Alert (" + lowStockProducts.size() + " items)");
            lowStockTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #D32F2F;");
            titleRow.getChildren().addAll(crossIcon, lowStockTitle);

            javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Product> lowStockTable = new javafx.scene.control.TableView<>();
            lowStockTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
            lowStockTable.setMaxHeight(200);
            lowStockTable.setStyle("-fx-background-color: transparent;");

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> nameCol = new javafx.scene.control.TableColumn<>("Product Name");
            nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> catCol = new javafx.scene.control.TableColumn<>("Category");
            catCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getCategory() != null ? data.getValue().getCategory().getName() : "-"));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Integer> qtyCol = new javafx.scene.control.TableColumn<>("Current Qty");
            qtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
            qtyCol.setStyle("-fx-alignment: CENTER;");
            // Red text for quantity
            qtyCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                @Override protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); }
                    else { setText(String.valueOf(item)); setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold; -fx-alignment: CENTER;"); }
                }
            });

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Integer> minCol = new javafx.scene.control.TableColumn<>("Min Level");
            minCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("minStockLevel"));
            minCol.setStyle("-fx-alignment: CENTER;");

            lowStockTable.getColumns().addAll(nameCol, catCol, qtyCol, minCol);
            lowStockTable.setItems(javafx.collections.FXCollections.observableArrayList(lowStockProducts));
            
            lowStockPanel.getChildren().addAll(titleRow, lowStockTable);
        }
        VBox.setVgrow(lowStockPanel, javafx.scene.layout.Priority.NEVER);

        content.getChildren().addAll(statsRow, chartTitle, barChart, lowStockPanel);
        return content;
    }
    
    private VBox createDashboardCard(String title, String value, String colorHex) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 3);");
        card.setMinHeight(100);
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #78909C;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private VBox createOrderHistoryView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Order> table = new javafx.scene.control.TableView<>();
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        javafx.collections.ObservableList<com.pbl3.project.pbl3_project.entity.Order> orderMaster = javafx.collections.FXCollections.observableArrayList(orderService.getAllOrders());
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, Long> idCol = new javafx.scene.control.TableColumn<>("Order ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> dateCol = new javafx.scene.control.TableColumn<>("Created At");
        dateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCreatedAt().toString()));
        dateCol.setPrefWidth(200);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, Double> totalCol = new javafx.scene.control.TableColumn<>("Total Price");
        totalCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("totalPrice"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> userCol = new javafx.scene.control.TableColumn<>("Created By");
        userCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getUser().getFullName()));

        table.getColumns().addAll(idCol, dateCol, totalCol, userCol);
        table.setItems(orderMaster);

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Order> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    try {
                        showOrderDetailsDialog(stage, orderService.getOrderWithItems(row.getItem().getId()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not load order details: " + ex.getMessage());
                    }
                }
            });
            return row;
        });

        // Expandable Search Bar for Orders
        javafx.scene.layout.HBox oSearchBox = new javafx.scene.layout.HBox(0);
        oSearchBox.setAlignment(Pos.CENTER);
        oSearchBox.getStyleClass().add("expandable-search-box");
        oSearchBox.setPrefSize(40, 40); oSearchBox.setMinSize(40, 40); oSearchBox.setMaxSize(40, 40);
        javafx.scene.shape.SVGPath oIcon = new javafx.scene.shape.SVGPath();
        oIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        oIcon.setFill(javafx.scene.paint.Color.web("#90A4AE"));
        javafx.scene.layout.Region oSpacer = new javafx.scene.layout.Region();
        oSpacer.setMinWidth(0); oSpacer.setPrefWidth(0);
        TextField oField = new TextField();
        oField.setPromptText("Search..."); oField.getStyleClass().add("search-text-field");
        oField.setMinWidth(0); oField.setMaxWidth(0); oField.setPrefWidth(0); oField.setOpacity(0);
        oSearchBox.getChildren().addAll(oIcon, oSpacer, oField);
        javafx.animation.Timeline oExpand = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(oSearchBox.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oSearchBox.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oSpacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline oCollapse = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(oSearchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oSearchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oSpacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)));

        oField.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV.toLowerCase().trim();
            if (q.isEmpty()) { table.setItems(orderMaster); } else {
                javafx.collections.ObservableList<com.pbl3.project.pbl3_project.entity.Order> filtered = javafx.collections.FXCollections.observableArrayList();
                for (com.pbl3.project.pbl3_project.entity.Order o : orderMaster) {
                    if (String.valueOf(o.getId()).contains(q) ||
                        (o.getCreatedAt() != null && o.getCreatedAt().toString().toLowerCase().contains(q)) ||
                        (o.getUser() != null && o.getUser().getFullName().toLowerCase().contains(q))) filtered.add(o);
                }
                table.setItems(filtered);
            }
        });

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        oSearchBox.setOnMouseClicked(ev -> {
            if (oSearchBox.getMaxWidth() == 40) { oExpand.play(); oField.requestFocus(); }
            else if (ev.getTarget() == oIcon || ev.getTarget() == oSearchBox) { oField.clear(); content.requestFocus(); oCollapse.play(); }
        });

        // Filter Button (Payment Method)
        javafx.scene.layout.HBox oFilterBox = new javafx.scene.layout.HBox();
        oFilterBox.setAlignment(Pos.CENTER);
        oFilterBox.getStyleClass().add("expandable-search-box");
        oFilterBox.setPrefSize(40, 40); oFilterBox.setMinSize(40, 40); oFilterBox.setMaxSize(40, 40);
        oFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath oFilterIcon = new javafx.scene.shape.SVGPath();
        oFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        oFilterIcon.setFill(javafx.scene.paint.Color.web("#90A4AE"));
        oFilterBox.getChildren().add(oFilterIcon);

        javafx.stage.Popup oFilterPopup = new javafx.stage.Popup();
        oFilterPopup.setAutoHide(true);

        oFilterBox.setOnMouseClicked(fev -> {
            if (oFilterPopup.isShowing()) {
                oFilterPopup.hide();
                return;
            }
            
            VBox popupContainer = new VBox(10);
            popupContainer.setPadding(new Insets(15));
            popupContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-border-color: #ECEFF1; -fx-border-radius: 8;");
            popupContainer.setPrefWidth(350);

            VBox scrollContent = new VBox(10);
            scrollContent.setStyle("-fx-background-color: white;");
            scrollContent.setPadding(new Insets(5, 15, 5, 15));
            javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(scrollContent);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: white; -fx-background-color: transparent; -fx-border-color: transparent;");
            scrollPane.setPrefViewportHeight(350);

            // --- Date Range ---
            Label dateTitle = new Label("Date Range");
            dateTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker();
            startDatePicker.setPromptText("Start Date");
            startDatePicker.setPrefWidth(140);
            startDatePicker.setStyle("-fx-font-size: 13px;");
            javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker();
            endDatePicker.setPromptText("End Date");
            endDatePicker.setPrefWidth(140);
            endDatePicker.setStyle("-fx-font-size: 13px;");
            customizeDatePicker(startDatePicker);
            customizeDatePicker(endDatePicker);
            javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
            dateBox.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.control.Separator sepDate = new javafx.scene.control.Separator();

            // --- Created By ---
            Label userTitle = new Label("Created By");
            userTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            
            javafx.scene.control.CheckBox allUsersCb = new javafx.scene.control.CheckBox("All Users");
            allUsersCb.setSelected(true);
            allUsersCb.setStyle("-fx-font-size: 13px; -fx-text-fill: #455A64;");

            VBox userScroll = new VBox(8);
            userScroll.setPadding(new Insets(5, 5, 5, 20));
            
            java.util.List<javafx.scene.control.CheckBox> userCbs = new java.util.ArrayList<>();
            java.util.Set<String> userNames = orderMaster.stream()
                .map(o -> o.getUser() != null ? o.getUser().getFullName() : "Unknown")
                .collect(java.util.stream.Collectors.toSet());
            
            for (String uName : userNames) {
                if (uName.trim().isEmpty()) continue;
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(uName);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: #455A64;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allUsersCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : userCbs) if (!c.isSelected()) all = false;
                        allUsersCb.setSelected(all);
                    }
                });
                userCbs.add(cb);
                userScroll.getChildren().add(cb);
            }
            allUsersCb.setOnAction(e -> {
                boolean sel = allUsersCb.isSelected();
                for (javafx.scene.control.CheckBox cb : userCbs) cb.setSelected(sel);
            });

            javafx.scene.control.Separator sepUser = new javafx.scene.control.Separator();

            // --- Payment Method ---
            Label methodTitle = new Label("Payment Method");
            methodTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            
            javafx.scene.control.CheckBox allMethodsCb = new javafx.scene.control.CheckBox("All Methods");
            allMethodsCb.setSelected(true);
            allMethodsCb.setStyle("-fx-font-size: 13px; -fx-text-fill: #455A64;");

            VBox methodScroll = new VBox(8);
            methodScroll.setPadding(new Insets(5, 5, 5, 20));
            
            java.util.List<javafx.scene.control.CheckBox> methodCbs = new java.util.ArrayList<>();
            for (com.pbl3.project.pbl3_project.entity.PaymentMethod pm : com.pbl3.project.pbl3_project.entity.PaymentMethod.values()) {
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(pm.name());
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: #455A64;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allMethodsCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : methodCbs) if (!c.isSelected()) all = false;
                        allMethodsCb.setSelected(all);
                    }
                });
                methodCbs.add(cb);
                methodScroll.getChildren().add(cb);
            }

            allMethodsCb.setOnAction(e -> {
                boolean sel = allMethodsCb.isSelected();
                for (javafx.scene.control.CheckBox cb : methodCbs) cb.setSelected(sel);
            });

            javafx.scene.control.Separator sepMethod = new javafx.scene.control.Separator();

            // --- Price Range ---
            Label priceTitle = new Label("Total Price Range");
            priceTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            
            double maxPrice = orderMaster.stream().mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0).max().orElse(1000000);
            if (maxPrice == 0) maxPrice = 1000;
            
            Label priceLabel = new Label("0 - " + String.format("%.0f", maxPrice) + " VND");
            priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1976D2; -fx-font-weight: bold;");
            
            RangeSlider priceSlider = new RangeSlider(0, maxPrice, 0, maxPrice, 280);
            priceSlider.minVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%.0f - %.0f VND", nv.doubleValue(), priceSlider.maxVal.get())));
            priceSlider.maxVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%.0f - %.0f VND", priceSlider.minVal.get(), nv.doubleValue())));

            scrollContent.getChildren().addAll(
                dateTitle, dateBox, sepDate,
                userTitle, allUsersCb, userScroll, sepUser,
                methodTitle, allMethodsCb, methodScroll, sepMethod,
                priceTitle, priceLabel, priceSlider
            );

            // --- Buttons ---
            javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
            btnRow.setAlignment(Pos.CENTER_RIGHT);
            
            final double fMaxPrice = maxPrice;

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(ae -> {
                oFilterBox.setStyle("");
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                allUsersCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : userCbs) cb.setSelected(true);
                allMethodsCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : methodCbs) cb.setSelected(true);
                priceSlider.minVal.set(0); priceSlider.maxVal.set(fMaxPrice);
                table.setItems(orderMaster);
                oFilterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(ae -> {
                java.util.Set<String> selectedUsers = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : userCbs) {
                    if (cb.isSelected()) selectedUsers.add(cb.getText());
                }
                
                java.util.Set<String> selectedMethods = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : methodCbs) {
                    if (cb.isSelected()) selectedMethods.add(cb.getText());
                }
                
                double pMin = priceSlider.minVal.get();
                double pMax = priceSlider.maxVal.get();
                java.time.LocalDate sDate = startDatePicker.getValue();
                java.time.LocalDate eDate = endDatePicker.getValue();

                java.util.List<com.pbl3.project.pbl3_project.entity.Order> result = orderMaster.stream()
                    .filter(o -> {
                        String uName = o.getUser() != null ? o.getUser().getFullName() : "Unknown";
                        return selectedUsers.contains(uName);
                    })
                    .filter(o -> {
                        String mName = o.getPaymentMethod() != null ? o.getPaymentMethod().name() : null;
                        return mName == null || selectedMethods.contains(mName);
                    })
                    .filter(o -> {
                        double price = o.getTotalPrice() != null ? o.getTotalPrice() : 0;
                        return price >= pMin && price <= pMax;
                    })
                    .filter(o -> {
                        if (o.getCreatedAt() == null) return true;
                        java.time.LocalDate oDate = o.getCreatedAt().toLocalDate();
                        if (sDate != null && oDate.isBefore(sDate)) return false;
                        if (eDate != null && oDate.isAfter(eDate)) return false;
                        return true;
                    })
                    .collect(java.util.stream.Collectors.toList());

                table.setItems(javafx.collections.FXCollections.observableArrayList(result));

                boolean hasFilter = !allMethodsCb.isSelected() || !allUsersCb.isSelected() || pMin > 0 || pMax < fMaxPrice || sDate != null || eDate != null;
                oFilterBox.setStyle(hasFilter ? "-fx-border-color: #1976D2;" : "");
                oFilterPopup.hide();
            });

            btnRow.getChildren().addAll(resetBtn, applyBtn);

            popupContainer.getChildren().addAll(scrollPane, btnRow);
            oFilterPopup.getContent().clear();
            oFilterPopup.getContent().add(popupContainer);

            javafx.geometry.Bounds bounds = oFilterBox.localToScreen(oFilterBox.getBoundsInLocal());
            oFilterPopup.show(oFilterBox, bounds.getMinX() - 330 + 40, bounds.getMaxY() + 5);
        });

        Label orderHeader = new Label("Order History");
        orderHeader.getStyleClass().add("header-label");
        javafx.scene.layout.BorderPane orderToolbar = new javafx.scene.layout.BorderPane();
        orderToolbar.setLeft(orderHeader);
        javafx.scene.layout.HBox oRightBox = new javafx.scene.layout.HBox(15, oFilterBox, oSearchBox);
        oRightBox.setAlignment(Pos.CENTER_RIGHT);
        orderToolbar.setRight(oRightBox);
        javafx.scene.layout.BorderPane.setAlignment(orderHeader, Pos.CENTER_LEFT);

        javafx.scene.layout.HBox orderStatusBar = new javafx.scene.layout.HBox(15, createSortStatusLabel(table), createRowCountBox(table));
        orderStatusBar.setAlignment(Pos.CENTER_RIGHT);
        orderStatusBar.setPadding(new Insets(5, 5, 0, 0));

        content.getChildren().addAll(orderToolbar, table, orderStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        enableDeselectOnOutsideClick(content, table);
        return content;
    }
    
    private javafx.scene.Node createSalesView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.control.SplitPane splitPane = new javafx.scene.control.SplitPane();
        splitPane.setDividerPositions(0.65);

        // === LEFT SIDE: Switchable Views ===
        javafx.scene.layout.StackPane leftPane = new javafx.scene.layout.StackPane();
        
        // --- 1. Category Grid View ---
        VBox categoryView = new VBox(20);
        categoryView.setPadding(new Insets(20));
        categoryView.setAlignment(Pos.TOP_CENTER);
        
        Label catTitle = new Label("Select Category");
        catTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        javafx.scene.layout.FlowPane categoryGrid = new javafx.scene.layout.FlowPane();
        categoryGrid.setHgap(20);
        categoryGrid.setVgap(20);
        categoryGrid.setAlignment(Pos.CENTER);
        
        categoryView.getChildren().addAll(catTitle, categoryGrid);
        
        // --- 2. Product List View ---
        VBox productView = new VBox(10);
        productView.setPadding(new Insets(10));
        productView.setVisible(false);
        
        javafx.scene.layout.HBox productHeader = new javafx.scene.layout.HBox(10);
        productHeader.setAlignment(Pos.CENTER_LEFT);
        
        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("button");
        
        Label productTitle = new Label("Products");
        productTitle.getStyleClass().add("header-label");
        
        productHeader.getChildren().addAll(backBtn, productTitle);
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search products...");
        searchField.setStyle("-fx-background-radius: 20; -fx-padding: 8 15;");
        
        javafx.scene.layout.FlowPane productGrid = new javafx.scene.layout.FlowPane();
        productGrid.setHgap(15);
        productGrid.setVgap(15);
        productGrid.setPadding(new Insets(10));
        productGrid.setAlignment(Pos.TOP_LEFT);
        
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(productGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        productView.getChildren().addAll(productHeader, searchField, scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        
        leftPane.getChildren().addAll(categoryView, productView);
        
        // === RIGHT SIDE: Cart ===
        VBox rightBox = new VBox(10);
        rightBox.setPadding(new Insets(10));
        rightBox.setStyle("-fx-background-color: #ECEFF1;");
        Label rightTitle = new Label("Shopping Cart");
        rightTitle.getStyleClass().add("header-label");

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest> cartTable = new javafx.scene.control.TableView<>();
        cartTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest, Long> cIdCol = new javafx.scene.control.TableColumn<>("ID");
        cIdCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("productId"));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest, Integer> cQtyCol = new javafx.scene.control.TableColumn<>("Qty");
        cQtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
        cartTable.getColumns().addAll(cIdCol, cQtyCol);

        javafx.collections.ObservableList<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest> cartItems = javafx.collections.FXCollections.observableArrayList();
        cartTable.setItems(cartItems);

        // === Data & Logic ===
        java.util.List<com.pbl3.project.pbl3_project.entity.Product> allProducts = productService.getAllProducts();
        final com.pbl3.project.pbl3_project.entity.Category[] selectedCategory = {null};
        
        Runnable renderProducts = () -> {
            productGrid.getChildren().clear();
            String query = searchField.getText().toLowerCase();
            
            for (com.pbl3.project.pbl3_project.entity.Product p : allProducts) {
                boolean matchName = p.getName().toLowerCase().contains(query);
                boolean matchCat = selectedCategory[0] != null && p.getCategory() != null && p.getCategory().getId().equals(selectedCategory[0].getId());
                
                if (matchName && matchCat) {
                    VBox card = new VBox(5);
                    card.getStyleClass().add("product-card");
                    card.setPrefSize(140, 180);
                    card.setAlignment(Pos.TOP_CENTER);
                    
                    if (p.getQuantity() <= 0) card.getStyleClass().add("product-card-unavailable");
                    
                    javafx.scene.layout.StackPane imgPlaceholder = new javafx.scene.layout.StackPane();
                    imgPlaceholder.getStyleClass().add("card-image-placeholder");
                    imgPlaceholder.setPrefSize(140, 100);
                    Label initial = new Label(p.getName().substring(0, 1).toUpperCase());
                    initial.setStyle("-fx-font-size: 30px; -fx-text-fill: #009688; -fx-font-weight: bold;");
                    imgPlaceholder.getChildren().add(initial);
                    
                    VBox info = new VBox(3);
                    info.setPadding(new Insets(10));
                    info.setAlignment(Pos.CENTER);
                    
                    Label nameLbl = new Label(p.getName());
                    nameLbl.getStyleClass().add("card-name");
                    nameLbl.setMaxWidth(130);
                    
                    Label priceLbl = new Label("$" + p.getPrice());
                    priceLbl.getStyleClass().add("card-price");
                    
                    Label stockLbl;
                    if (p.getQuantity() > 0) {
                        stockLbl = new Label("Stock: " + p.getQuantity());
                        stockLbl.getStyleClass().add("card-stock");
                    } else {
                        stockLbl = new Label("OUT OF STOCK");
                        stockLbl.getStyleClass().add("card-out-stock");
                    }
                    
                    info.getChildren().addAll(nameLbl, priceLbl, stockLbl);
                    card.getChildren().addAll(imgPlaceholder, info);
                    
                    card.setOnMouseClicked(e -> {
                        if (p.getQuantity() > 0) {
                            var existing = cartItems.stream().filter(i -> i.getProductId().equals(p.getId())).findFirst();
                            if (existing.isPresent()) {
                                existing.get().setQuantity(existing.get().getQuantity() + 1);
                                cartTable.refresh();
                            } else {
                                var item = new com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest();
                                item.setProductId(p.getId());
                                item.setQuantity(1);
                                cartItems.add(item);
                            }
                            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(100), card);
                            st.setFromX(1.0); st.setFromY(1.0);
                            st.setToX(0.95); st.setToY(0.95);
                            st.setAutoReverse(true);
                            st.setCycleCount(2);
                            st.play();
                        } else {
                            toastService.showWarning("Product out of stock!");
                        }
                    });
                    
                    productGrid.getChildren().add(card);
                }
            }
        };
        
        // Load Categories into Grid
        java.util.List<com.pbl3.project.pbl3_project.entity.Category> categories = categoryRepository.findAll();
        for (com.pbl3.project.pbl3_project.entity.Category cat : categories) {
            long count = allProducts.stream().filter(p -> p.getCategory() != null && p.getCategory().getId().equals(cat.getId())).count();
            
            VBox catCard = new VBox(10);
            catCard.getStyleClass().add("category-card");
            catCard.setPrefSize(160, 120);
            catCard.setAlignment(Pos.CENTER);
            
            Label catName = new Label(cat.getName());
            catName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
            
            Label catCount = new Label(count + " products");
            catCount.setStyle("-fx-font-size: 12px; -fx-text-fill: #78909C;");
            
            catCard.getChildren().addAll(catName, catCount);
            
            catCard.setOnMouseClicked(e -> {
                selectedCategory[0] = cat;
                productTitle.setText(cat.getName());
                categoryView.setVisible(false);
                productView.setVisible(true);
                renderProducts.run();
            });
            
            categoryGrid.getChildren().add(catCard);
        }
        
        backBtn.setOnAction(e -> {
            productView.setVisible(false);
            categoryView.setVisible(true);
            selectedCategory[0] = null;
        });
        
        searchField.textProperty().addListener((obs, old, val) -> renderProducts.run());

        Button checkoutButton = new Button("CHECKOUT");
        checkoutButton.getStyleClass().addAll("button", "success-button");
        checkoutButton.setMaxWidth(Double.MAX_VALUE);
        checkoutButton.setOnAction(e -> {
            if (cartItems.isEmpty()) return;
            
            // Calculate total based on current product prices
            double total = cartItems.stream().mapToDouble(item -> {
                var p = allProducts.stream()
                        .filter(prod -> prod.getId().equals(item.getProductId()))
                        .findFirst()
                        .orElse(null);
                return p != null ? p.getPrice() * item.getQuantity() : 0.0;
            }).sum();

            showCheckoutDialog(stage, total, (method, printReceipt) -> {
                try {
                    com.pbl3.project.pbl3_project.dto.CreateOrderRequest req = new com.pbl3.project.pbl3_project.dto.CreateOrderRequest();
                    req.setUserId(user.getId());
                    req.setItems(new java.util.ArrayList<>(cartItems));
                    req.setPaymentMethod(method);
                    com.pbl3.project.pbl3_project.entity.Order newOrder = orderService.createOrder(req);
                    
                    if (printReceipt) {
                        receiptService.generateAndOpenReceipt(newOrder);
                    }
                    
                    toastService.showSuccess("Order Paid via " + method + "!");
                    cartItems.clear();
                    allProducts.clear();
                    allProducts.addAll(productService.getAllProducts());
                    renderProducts.run();
                } catch (Exception ex) {
                    toastService.showError("Order Failed: " + ex.getMessage());
                }
            });
        });

        rightBox.getChildren().addAll(rightTitle, cartTable, checkoutButton);
        VBox.setVgrow(cartTable, javafx.scene.layout.Priority.ALWAYS);
        splitPane.getItems().addAll(leftPane, rightBox);
        return splitPane;
    }
    

    // --- Core Navigation & Animation ---
    
    private void switchScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user, String title, String navId, javafx.scene.Node content) {
        javafx.scene.Scene scene = stage.getScene();
        javafx.scene.layout.BorderPane root = null;
        
        // Try to reuse Layout
        if (scene != null) {
            javafx.scene.Parent currentRoot = scene.getRoot();
            if (currentRoot instanceof javafx.scene.layout.BorderPane && "MAIN_LAYOUT".equals(currentRoot.getUserData())) {
                root = (javafx.scene.layout.BorderPane) currentRoot;
            } else if (currentRoot instanceof javafx.scene.layout.StackPane) {
                // Handle wrapped root from ToastService
                javafx.scene.layout.StackPane stack = (javafx.scene.layout.StackPane) currentRoot;
                if (!stack.getChildren().isEmpty() && stack.getChildren().get(0) instanceof javafx.scene.layout.BorderPane) {
                    javafx.scene.layout.BorderPane possibleRoot = (javafx.scene.layout.BorderPane) stack.getChildren().get(0);
                    if ("MAIN_LAYOUT".equals(possibleRoot.getUserData())) {
                        root = possibleRoot;
                    }
                }
            }
        }
        
        if (root != null) {
            
            // 1. Update Center Content with Float animation
            root.setCenter(content);
            javafx.scene.layout.BorderPane.setMargin(content, new Insets(15)); // Restore margin
            
            // Float up animation (from 30px below to 0)
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), content);
            tt.setFromY(30);
            tt.setToY(0);
            tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
            
            // Combined with subtle fade
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), content);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            
            javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(tt, ft);
            pt.play();
            
            // 2. Update Header Title
            Label pageTitle = (Label) root.lookup("#header-title");
            if (pageTitle != null) pageTitle.setText(title);
            
            // 3. Update Sidebar Active State
            updateSidebarState(root, navId);
            
        } else {
            // First Load: Create Full Layout
            javafx.scene.layout.BorderPane layout = createMainLayout(stage, user, title, content, navId);
            layout.setUserData("MAIN_LAYOUT"); // Tag for reuse
            Scene newScene = new Scene(layout, 1000, 700);
            stage.setScene(newScene);
            toastService.setScene(newScene); // Init toast container
            stage.centerOnScreen();
            
            // Float up animation for initial load
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), layout);
            tt.setFromY(50);
            tt.setToY(0);
            tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
            
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), layout);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            
            javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(tt, ft);
            pt.play();
        }
    }

    private void updateSidebarState(javafx.scene.Parent root, String activeNavId) {
        // Find all nav buttons and update class
        for (String id : new String[]{"nav-dashboard", "nav-products", "nav-import", "nav-sales", "nav-attributes", "nav-history", "nav-stock-history"}) {
            javafx.scene.Node btn = root.lookup("#" + id);
            if (btn != null) {
                if (id.equals(activeNavId)) {
                    if (!btn.getStyleClass().contains("active")) btn.getStyleClass().add("active");
                } else {
                    btn.getStyleClass().remove("active");
                }
            }
        }
    }

    private javafx.scene.layout.BorderPane createMainLayout(Stage stage, com.pbl3.project.pbl3_project.entity.User user, String title, javafx.scene.Node centerContent, String activeNavId) {
        javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
        
        // Sidebar
        VBox sidebar = new VBox(5);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        sidebar.setMaxWidth(220);
        
        Label appTitle = new Label("SALES MGR");
        appTitle.setStyle("-fx-text-fill: #37474F; -fx-font-weight: bold; -fx-font-size: 20px; -fx-padding: 0 0 20 15;");
        
        Button navDashboard = createNavButton("Dashboard", "nav-dashboard", () -> showOverviewScene(stage, user));
        Button navProducts = createNavButton("Products", "nav-products", () -> showDashboardScene(stage, user));
        Button navImport = createNavButton("Import Goods", "nav-import", () -> showImportOrderScene(stage, user));
        Button navSales = createNavButton("Sales (POS)", "nav-sales", () -> showSalesScene(stage, user));
        Button navAttributes = createNavButton("Master Data", "nav-attributes", () -> showAttributesScene(stage, user));
        Button navHistory = createNavButton("Order History", "nav-history", () -> showOrderHistoryScene(stage, user));
        Button navStockHistory = createNavButton("Stock History", "nav-stock-history", () -> showStockHistoryScene(stage, user));
        Button navLogout = new Button("Logout");
        navLogout.setId("nav-logout");
        navLogout.getStyleClass().clear();
        navLogout.getStyleClass().add("nav-logout-btn");
        navLogout.setOnAction(e -> showLoginScene(stage));
        // Logout SVG icon (door with arrow - matching Figma)
        javafx.scene.shape.SVGPath logoutIcon = new javafx.scene.shape.SVGPath();
        logoutIcon.setContent("M10.09 15.59L11.5 17l5-5-5-5-1.41 1.41L12.67 11H3v2h9.67l-2.58 2.59zM19 3H5c-1.11 0-2 .9-2 2v4h2V5h14v14H5v-4H3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z");
        logoutIcon.setFill(javafx.scene.paint.Color.web("#F44336"));
        logoutIcon.setScaleX(0.7); logoutIcon.setScaleY(0.7);
        navLogout.setGraphic(logoutIcon);

        // Initial Active State
        if ("nav-dashboard".equals(activeNavId)) navDashboard.getStyleClass().add("active");
        if ("nav-products".equals(activeNavId)) navProducts.getStyleClass().add("active");
        if ("nav-import".equals(activeNavId)) navImport.getStyleClass().add("active");
        if ("nav-sales".equals(activeNavId)) navSales.getStyleClass().add("active");
        if ("nav-attributes".equals(activeNavId)) navAttributes.getStyleClass().add("active");
        if ("nav-history".equals(activeNavId)) navHistory.getStyleClass().add("active");
        if ("nav-stock-history".equals(activeNavId)) navStockHistory.getStyleClass().add("active");

        sidebar.getChildren().addAll(appTitle, navDashboard, navProducts, navImport, navSales, navAttributes, navHistory, navStockHistory, new javafx.scene.control.Separator(), navLogout);
        sidebar.setPadding(new Insets(15));
        
        // Clip sidebar content during animation (with rounded corners)
        javafx.scene.shape.Rectangle clipRect = new javafx.scene.shape.Rectangle();
        clipRect.widthProperty().bind(sidebar.widthProperty());
        clipRect.heightProperty().bind(sidebar.heightProperty());
        clipRect.setArcWidth(40);  // Match sidebar border-radius (20 * 2)
        clipRect.setArcHeight(40);
        sidebar.setClip(clipRect);
        
        // Header
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
        header.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 0, 5, 0, 0);");
        header.setAlignment(Pos.CENTER_LEFT);
        // Toggle Sidebar Button
        Button toggleSidebar = new Button("≡");
        toggleSidebar.setStyle("-fx-background-color: transparent; -fx-font-size: 22px; -fx-cursor: hand; -fx-padding: 2 8; -fx-alignment: center;");
        
        final double sidebarWidth = 220;
        final boolean[] sidebarHidden = {false}; // Track state
        
        toggleSidebar.setOnAction(e -> {
            if (!sidebarHidden[0]) {
                // === HIDE: Animate width shrinking + fade ===
                sidebarHidden[0] = true;
                javafx.animation.Timeline hideTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(sidebar.prefWidthProperty(), sidebarWidth),
                        new javafx.animation.KeyValue(sidebar.minWidthProperty(), sidebarWidth),
                        new javafx.animation.KeyValue(sidebar.maxWidthProperty(), sidebarWidth),
                        new javafx.animation.KeyValue(sidebar.opacityProperty(), 1.0)
                    ),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(150),
                        new javafx.animation.KeyValue(sidebar.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(sidebar.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(sidebar.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(sidebar.opacityProperty(), 0, javafx.animation.Interpolator.EASE_IN)
                    )
                );
                hideTimeline.setOnFinished(ev -> {
                    // Remove margin when fully hidden
                    javafx.scene.layout.BorderPane.setMargin(sidebar, new Insets(0));
                });
                hideTimeline.play();
                
            } else {
                // === SHOW: Animate width expanding + fade in ===
                sidebarHidden[0] = false;
                // Restore margin first
                javafx.scene.layout.BorderPane.setMargin(sidebar, new Insets(15, 0, 15, 15));
                
                javafx.animation.Timeline showTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(sidebar.prefWidthProperty(), 0),
                        new javafx.animation.KeyValue(sidebar.minWidthProperty(), 0),
                        new javafx.animation.KeyValue(sidebar.maxWidthProperty(), 0),
                        new javafx.animation.KeyValue(sidebar.opacityProperty(), 0)
                    ),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(150),
                        new javafx.animation.KeyValue(sidebar.prefWidthProperty(), sidebarWidth, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(sidebar.minWidthProperty(), sidebarWidth, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(sidebar.maxWidthProperty(), sidebarWidth, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(sidebar.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_OUT)
                    )
                );
                showTimeline.setOnFinished(ev -> {
                    // Ensure exact width and force layout refresh
                    sidebar.setPrefWidth(sidebarWidth);
                    sidebar.setMinWidth(sidebarWidth);
                    sidebar.setMaxWidth(sidebarWidth);
                    root.requestLayout(); 
                });
                showTimeline.play();
            }
        });
        
        Label pageTitle = new Label(title);
        pageTitle.setId("header-title"); // ID for lookup
        pageTitle.getStyleClass().add("header-label");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label userLabel = new Label(user.getFullName() + " (" + user.getRole() + ")");
        userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #78909C;");
        
        header.getChildren().addAll(toggleSidebar, pageTitle, spacer, userLabel);
        
        root.setLeft(sidebar);
        root.setTop(header);
        root.setCenter(centerContent);
        
        javafx.scene.layout.BorderPane.setMargin(sidebar, new Insets(15, 0, 15, 15));
        // Removed margin for centerContent to fix deselect dead zone
        
        root.setStyle("-fx-background-color: #ECEFF1;");
        
        return root;
    }
    
    // Updated helper to accept ID
    private Button createNavButton(String text, String id, Runnable action) {
        Button btn = new Button(text);
        btn.setId(id); // Set ID for lookup
        btn.getStyleClass().clear();
        btn.getStyleClass().add("button");
        btn.getStyleClass().add("nav-button");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();
        
        // Explicitly load fonts to ensure they are available
        try {
            javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/BeVietnamPro-Regular.ttf"), 12);
            javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/BeVietnamPro-Bold.ttf"), 12);
        } catch (Exception e) {
            System.err.println("Could not load fonts: " + e.getMessage());
        }

        // Load CSS globally
        stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
            }
        });
        
        showLoginScene(stage);
        stage.setTitle("Sales Management System");
        stage.show();
    }

    private void showLoginScene(Stage stage) {
        // UI Layout
        javafx.scene.layout.StackPane mainRoot = new javafx.scene.layout.StackPane();
        mainRoot.setStyle("-fx-background-color: #ECEFF1;");

        VBox loginBox = new VBox(15);
        loginBox.getStyleClass().add("login-box");
        loginBox.setAlignment(Pos.CENTER); // Center all children
        loginBox.setPadding(new Insets(30));
        loginBox.setMaxWidth(400);

        // Title
        Label titleLabel = new Label("SYSTEM LOGIN");
        titleLabel.getStyleClass().add("title-label");
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setStyle("-fx-alignment: center; -fx-padding: 10 0 5 0;"); // Reduced padding

        // Error Label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        // --- Floating label input fields ---
        // Username
        TextField usernameField = new TextField();
        usernameField.getStyleClass().add("text-field");
        usernameField.setMaxWidth(350);
        usernameField.setPrefHeight(44);
        usernameField.setStyle("-fx-border-color: #B0BEC5; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        Label usernameLabel = new Label("Username");
        usernameLabel.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 14px; -fx-background-color: #ECEFF1; -fx-background-radius: 20; -fx-padding: 0 4 0 4;");
        usernameLabel.setMouseTransparent(true);

        javafx.scene.layout.StackPane usernamePane = new javafx.scene.layout.StackPane(usernameField, usernameLabel);
        usernamePane.setMaxWidth(350);
        javafx.scene.layout.StackPane.setAlignment(usernameLabel, Pos.CENTER_LEFT);
        usernameLabel.setTranslateX(12);

        javafx.animation.Timeline userAnimUp = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(200),
                new javafx.animation.KeyValue(usernameLabel.translateYProperty(), -22, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(usernameLabel.translateXProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(usernameLabel.scaleXProperty(), 0.85, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(usernameLabel.scaleYProperty(), 0.85, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline userAnimDown = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(200),
                new javafx.animation.KeyValue(usernameLabel.translateYProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(usernameLabel.translateXProperty(), 12, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(usernameLabel.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(usernameLabel.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));

        Runnable updateUsernameState = () -> {
            boolean focused = usernameField.isFocused();
            boolean hasText = !usernameField.getText().isEmpty();
            if (focused) {
                userAnimDown.stop(); userAnimUp.play();
                usernameLabel.setStyle("-fx-text-fill: #1976D2; -fx-font-size: 14px; -fx-background-color: #ECEFF1; -fx-background-radius: 20; -fx-padding: 0 4 0 4;");
                usernameField.setStyle("-fx-border-color: #1976D2; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            } else if (hasText) {
                userAnimDown.stop(); userAnimUp.play();
                usernameLabel.setStyle("-fx-text-fill: #78909C; -fx-font-size: 14px; -fx-background-color: #ECEFF1; -fx-background-radius: 20; -fx-padding: 0 4 0 4;");
                usernameField.setStyle("-fx-border-color: #B0BEC5; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            } else {
                userAnimUp.stop(); userAnimDown.play();
                usernameLabel.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 14px; -fx-background-color: transparent; -fx-padding: 0 4 0 4;");
                usernameField.setStyle("-fx-border-color: #B0BEC5; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
        };
        usernameField.focusedProperty().addListener((obs, old, focused) -> updateUsernameState.run());
        usernameField.textProperty().addListener((obs, old, val) -> updateUsernameState.run());

        // Password
        PasswordField passwordField = new PasswordField();
        passwordField.getStyleClass().add("text-field");
        passwordField.setMaxWidth(350);
        passwordField.setPrefHeight(44);
        passwordField.setStyle("-fx-border-color: #B0BEC5; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        Label passwordLabel = new Label("Password");
        passwordLabel.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 14px; -fx-background-color: #ECEFF1; -fx-background-radius: 20; -fx-padding: 0 4 0 4;");
        passwordLabel.setMouseTransparent(true);

        javafx.scene.layout.StackPane passwordPane = new javafx.scene.layout.StackPane(passwordField, passwordLabel);
        passwordPane.setMaxWidth(350);
        javafx.scene.layout.StackPane.setAlignment(passwordLabel, Pos.CENTER_LEFT);
        passwordLabel.setTranslateX(12);

        javafx.animation.Timeline passAnimUp = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(200),
                new javafx.animation.KeyValue(passwordLabel.translateYProperty(), -22, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(passwordLabel.translateXProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(passwordLabel.scaleXProperty(), 0.85, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(passwordLabel.scaleYProperty(), 0.85, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline passAnimDown = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(200),
                new javafx.animation.KeyValue(passwordLabel.translateYProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(passwordLabel.translateXProperty(), 12, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(passwordLabel.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(passwordLabel.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));

        Runnable updatePasswordState = () -> {
            boolean focused = passwordField.isFocused();
            boolean hasText = !passwordField.getText().isEmpty();
            if (focused) {
                passAnimDown.stop(); passAnimUp.play();
                passwordLabel.setStyle("-fx-text-fill: #1976D2; -fx-font-size: 14px; -fx-background-color: #ECEFF1; -fx-background-radius: 20; -fx-padding: 0 4 0 4;");
                passwordField.setStyle("-fx-border-color: #1976D2; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            } else if (hasText) {
                passAnimDown.stop(); passAnimUp.play();
                passwordLabel.setStyle("-fx-text-fill: #78909C; -fx-font-size: 14px; -fx-background-color: #ECEFF1; -fx-background-radius: 20; -fx-padding: 0 4 0 4;");
                passwordField.setStyle("-fx-border-color: #B0BEC5; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            } else {
                passAnimUp.stop(); passAnimDown.play();
                passwordLabel.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 14px; -fx-background-color: transparent; -fx-padding: 0 4 0 4;");
                passwordField.setStyle("-fx-border-color: #B0BEC5; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
        };
        passwordField.focusedProperty().addListener((obs, old, focused) -> updatePasswordState.run());
        passwordField.textProperty().addListener((obs, old, val) -> updatePasswordState.run());

        // Login Button
        Button loginButton = new Button("LOGIN");
        loginButton.getStyleClass().addAll("button", "primary-button");
        loginButton.setMaxWidth(350);
        loginButton.setDefaultButton(true);
        loginButton.setCursor(javafx.scene.Cursor.HAND);
        
        // Button Click Animation
        loginButton.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), loginButton);
            st.setToX(0.95); st.setToY(0.95);
            st.play();
        });
        loginButton.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), loginButton);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });

        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            var user = authService.login(username, password);
            
            if (user != null) {
                showOverviewScene(stage, user);
            } else {
                errorLabel.setText("Invalid credentials!");
                
                // Shake Animation
                TranslateTransition shake = new TranslateTransition(Duration.millis(50), loginBox);
                shake.setByX(10f);
                shake.setCycleCount(6);
                shake.setAutoReverse(true);
                shake.playFromStart();
            }
        });

        loginBox.getChildren().addAll(titleLabel, errorLabel, usernamePane, passwordPane, loginButton);
        mainRoot.getChildren().add(loginBox);

        Scene scene = new Scene(mainRoot, 450, 350); // Wider window
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
    }


    private void showProductDialog(Stage owner, com.pbl3.project.pbl3_project.entity.Product product, com.pbl3.project.pbl3_project.entity.Category contextCategory, com.pbl3.project.pbl3_project.entity.User user, Runnable onSave) {
        try {
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.setTitle(product == null ? "Add New Product" : "Edit Product");
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);

            VBox root = new VBox(15);
            root.getStyleClass().add("dialog-root");
            root.setPadding(new Insets(20));

            Label titleLabel = new Label(product == null ? "Create New Product" : "Edit Product Details");
            titleLabel.getStyleClass().add("dialog-title");

            // --- Form Layout (GridPane) ---
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(15); 
            grid.setVgap(15);
            
            // Row 0: Basic Info
            TextField nameField = createStyledTextField(product != null ? product.getName() : "", "Product Name");
            TextField skuField = createStyledTextField(product != null ? product.getSku() : "", "SKU (Unique)");
            
            grid.add(createFormLabel("Product Name *"), 0, 0); grid.add(nameField, 1, 0);
            grid.add(createFormLabel("SKU *"), 2, 0); grid.add(skuField, 3, 0);

            // Row 1: Barcode & Category (Implicit)
            TextField barcodeField = createStyledTextField(product != null ? product.getBarcode() : "", "Barcode (Scan)");
            
            // NOTE: Category input removed primarily because it is inferred from context
            // But we should show it as read-only label
            Label catLabel = new Label(contextCategory != null ? contextCategory.getName() : (product != null && product.getCategory() != null ? product.getCategory().getName() : "Unknown"));
            catLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #009688;");
            
            grid.add(createFormLabel("Category"), 0, 1); grid.add(catLabel, 1, 1);
            grid.add(createFormLabel("Barcode"), 2, 1); grid.add(barcodeField, 3, 1);

            // Row 2: Pricing & Stock
            TextField importPriceField = createStyledTextField(product != null && product.getImportPrice() != null ? String.valueOf(product.getImportPrice()) : "", "Import Price");
            TextField priceField = createStyledTextField(product != null ? String.valueOf(product.getPrice()) : "", "Selling Price *");
            TextField qtyField = createStyledTextField(product != null ? String.valueOf(product.getQuantity()) : "0", "Quantity *");

            // Disable import price editing for existing products to enforce "Import Goods" workflow
            if (product != null) {
                importPriceField.setDisable(true);
            }

            grid.add(createFormLabel("Import Price"), 0, 2); grid.add(importPriceField, 1, 2);
            grid.add(createFormLabel("Selling Price *"), 2, 2); grid.add(priceField, 3, 2);
            grid.add(createFormLabel("Quantity *"), 0, 3); grid.add(qtyField, 1, 3);

            // Row 3: Master Data (Brand, Origin)
            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Brand> brandCombo = new javafx.scene.control.ComboBox<>();
            brandCombo.setMaxWidth(Double.MAX_VALUE); brandCombo.setPromptText("Select Brand");
            brandCombo.setItems(javafx.collections.FXCollections.observableArrayList(brandService.getAllBrands()));
            setComboConverter(brandCombo);
            if (product != null) brandCombo.setValue(product.getBrand());

            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Origin> originCombo = new javafx.scene.control.ComboBox<>();
            originCombo.setMaxWidth(Double.MAX_VALUE); originCombo.setPromptText("Select Origin");
            originCombo.setItems(javafx.collections.FXCollections.observableArrayList(originService.getAllOrigins()));
            setComboConverter(originCombo);
            if (product != null) originCombo.setValue(product.getOrigin());
            
            grid.add(createFormLabel("Brand"), 2, 3); grid.add(brandCombo, 3, 3);
            grid.add(createFormLabel("Origin"), 0, 4); grid.add(originCombo, 1, 4);

            // Row 4: Master Data (Unit, Description)
            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Unit> unitCombo = new javafx.scene.control.ComboBox<>();
            unitCombo.setMaxWidth(Double.MAX_VALUE); unitCombo.setPromptText("Select Unit");
            unitCombo.setItems(javafx.collections.FXCollections.observableArrayList(unitService.getAllUnits()));
            setComboConverter(unitCombo);
            if (product != null) unitCombo.setValue(product.getUnit());

            grid.add(createFormLabel("Unit"), 2, 4); grid.add(unitCombo, 3, 4);
            
            // Description and Min Stock Level
            TextField descField = createStyledTextField(product != null ? product.getDescription() : "", "Description");
            TextField minStockField = createStyledTextField(product != null && product.getMinStockLevel() != null ? String.valueOf(product.getMinStockLevel()) : "10", "Min Stock Level");
            grid.add(createFormLabel("Description"), 0, 5); grid.add(descField, 1, 5);
            grid.add(createFormLabel("Min Stock"), 2, 5); grid.add(minStockField, 3, 5);

            // --- Action Buttons ---
            Button saveButton = new Button("SAVE PRODUCT");
            saveButton.getStyleClass().addAll("button", "primary-button");
            saveButton.setMaxWidth(Double.MAX_VALUE);
            saveButton.setOnAction(e -> {
                try {
                    com.pbl3.project.pbl3_project.entity.Product p = product != null ? product : new com.pbl3.project.pbl3_project.entity.Product();
                    
                    if (nameField.getText().isEmpty() || priceField.getText().isEmpty()) {
                       toastService.showError("Please fill required fields (*)");
                       return;
                    }

                    int newQty = Integer.parseInt(qtyField.getText());
                    String reason = "Manual Add/Edit via UI";
                    
                    if (product != null && product.getQuantity() != newQty) {
                        javafx.scene.control.TextInputDialog reasonDialog = new javafx.scene.control.TextInputDialog();
                        reasonDialog.setTitle("Stock Edit Reason");
                        reasonDialog.setHeaderText("Quantity changed: " + product.getQuantity() + " -> " + newQty);
                        reasonDialog.setContentText("Please enter a reason for audit log:");
                        
                        // Apply custom application styling
                        reasonDialog.getDialogPane().getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
                        reasonDialog.getDialogPane().getStyleClass().add("dialog-root");
                        
                        java.util.Optional<String> result = reasonDialog.showAndWait();
                        if (result.isPresent() && !result.get().trim().isEmpty()) {
                            reason = result.get().trim();
                        } else {
                            toastService.showError("Change reason is required to update stock!");
                            return;
                        }
                    }

                    p.setName(nameField.getText());
                    p.setSku(skuField.getText());
                    p.setBarcode(barcodeField.getText());
                    p.setDescription(descField.getText());
                    p.setPrice(Double.parseDouble(priceField.getText()));
                    p.setQuantity(newQty);
                    try { p.setMinStockLevel(Integer.parseInt(minStockField.getText())); } catch (NumberFormatException ex) { p.setMinStockLevel(10); }
                    
                    String importPriceTxt = importPriceField.getText();
                    if (!importPriceTxt.isEmpty()) p.setImportPrice(Double.parseDouble(importPriceTxt));
                    
                    // Implicit Category
                    if (product == null && contextCategory != null) {
                        p.setCategory(contextCategory);
                    } else if (product != null && p.getCategory() == null && contextCategory != null) {
                        p.setCategory(contextCategory);
                    }
                    
                    p.setBrand(brandCombo.getValue());
                    p.setOrigin(originCombo.getValue());
                    p.setUnit(unitCombo.getValue());

                    productService.saveProduct(p, user, reason);
                    toastService.showSuccess("Product saved successfully!");
                    onSave.run();
                    dialog.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    toastService.showError("Save Error: " + ex.getMessage());
                }
            });

            root.getChildren().addAll(titleLabel, grid, new javafx.scene.control.Separator(), saveButton);
            
            Scene scene = new Scene(root, 700, 550); // Wider for Grid
            scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            toastService.showError("Could not open dialog: " + e.getMessage());
        }
    }
    
    // Helper for ComboBox Generic Converter
    private <T> void setComboConverter(javafx.scene.control.ComboBox<T> comboBox) {
        comboBox.setConverter(new javafx.util.StringConverter<T>() {
            @Override
            public String toString(T object) {
                if (object == null) return "";
                try {
                    return (String) object.getClass().getMethod("getName").invoke(object);
                } catch (Exception e) {
                    return object.toString();
                }
            }
            @Override
            public T fromString(String string) { return null; }
        });
    }

    private void showOrderDetailsDialog(Stage owner, com.pbl3.project.pbl3_project.entity.Order order) {
        try {
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.setTitle("Order Details #" + order.getId());
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);

            VBox root = new VBox(15);
            root.getStyleClass().add("dialog-root");

            Label titleLabel = new Label("Order #" + order.getId());
            titleLabel.getStyleClass().add("dialog-title");
            titleLabel.setMaxWidth(Double.MAX_VALUE);
            
            Label dateLabel = new Label("Date: " + order.getCreatedAt());
            dateLabel.getStyleClass().add("header-label");
            
            Label userLabel = new Label("Created By: " + order.getUser().getFullName());
            userLabel.setStyle("-fx-text-fill: #78909C; -fx-font-style: italic;");

            javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.OrderItem> table = new javafx.scene.control.TableView<>();
            table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
            
            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, String> pNameCol = new javafx.scene.control.TableColumn<>("Product");
            pNameCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getProduct().getName()));
            
            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, Double> priceCol = new javafx.scene.control.TableColumn<>("Unit Price");
            priceCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getPrice()));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, Integer> qtyCol = new javafx.scene.control.TableColumn<>("Qty");
            qtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, Double> subTotalCol = new javafx.scene.control.TableColumn<>("Subtotal");
            subTotalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getPrice() * cell.getValue().getQuantity()));

            table.getColumns().addAll(pNameCol, priceCol, qtyCol, subTotalCol);
            table.setItems(javafx.collections.FXCollections.observableArrayList(order.getOrderItems()));
            table.setPrefHeight(300);

            Label totalLabel = new Label("TOTAL: " + order.getTotalPrice() + " VND");
            totalLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #F44336;");
            totalLabel.setMaxWidth(Double.MAX_VALUE);
            totalLabel.setAlignment(Pos.CENTER_RIGHT);

            Button closeButton = new Button("CLOSE");
            closeButton.getStyleClass().add("button");
            closeButton.setMaxWidth(Double.MAX_VALUE);
            closeButton.setOnAction(e -> dialog.close());

            root.getChildren().addAll(titleLabel, dateLabel, userLabel, table, totalLabel, closeButton);
            VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
            
            Scene scene = new Scene(root, 600, 550);
            scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not show details: " + e.getMessage());
        }
    }

    private TextField createStyledTextField(String value, String prompt) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.getStyleClass().add("text-field");
        return tf;
    }

    private Label createFormLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    private <T> void enableDragSelection(javafx.scene.control.TableView<T> table) {
        final int[] dragAnchor = new int[] { -1 };

        // 1. Filter to handle "Click Again to Deselect" logic
        table.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
             if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                 // Check if it's a simple click (no modifiers)
                 boolean isSimpleClick = !event.isShortcutDown() && !event.isShiftDown();
                 
                 javafx.scene.Node node = event.getPickResult().getIntersectedNode();
                 while (node != null && node != table && !(node instanceof javafx.scene.control.TableRow)) {
                     node = node.getParent();
                 }
                 
                 if (node instanceof javafx.scene.control.TableRow) {
                     javafx.scene.control.TableRow<?> row = (javafx.scene.control.TableRow<?>) node;
                     if (!row.isEmpty()) {
                         int index = row.getIndex();
                         if (isSimpleClick && event.getClickCount() == 1 && table.getSelectionModel().isSelected(index)) {
                             // Deselect and consume to prevent "Select Only" behavior
                             table.getSelectionModel().clearSelection(index);
                             dragAnchor[0] = index; 
                             event.consume();
                         } else {
                             // Let standard behavior run or record anchor
                             dragAnchor[0] = index;
                         }
                     }
                 }
             }
        });

        table.setOnMouseDragged(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY && dragAnchor[0] >= 0) {
                javafx.scene.Node node = event.getPickResult().getIntersectedNode();
                while (node != null && node != table && !(node instanceof javafx.scene.control.TableRow)) {
                    node = node.getParent();
                }
                if (node instanceof javafx.scene.control.TableRow) {
                     javafx.scene.control.TableRow<?> row = (javafx.scene.control.TableRow<?>) node;
                     if (!row.isEmpty()) {
                         int currentIndex = row.getIndex();
                         table.getSelectionModel().clearSelection();
                         int start = Math.min(dragAnchor[0], currentIndex);
                         int end = Math.max(dragAnchor[0], currentIndex);
                         table.getSelectionModel().selectRange(start, end + 1);
                     }
                }
            }
        });
    }

    private void enableDeselectOnOutsideClick(javafx.scene.layout.Pane root, javafx.scene.control.TableView<?> table) {
        root.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            boolean isSafe = false;
            javafx.scene.Node curr = (javafx.scene.Node) event.getTarget();
            
            boolean clickedEmptyTableArea = false;
            javafx.scene.Node checkNode = curr;
            while (checkNode != null && checkNode != root) {
                if (checkNode instanceof javafx.scene.control.IndexedCell) {
                    if (((javafx.scene.control.IndexedCell<?>) checkNode).isEmpty()) {
                        clickedEmptyTableArea = true;
                    }
                    break;
                }
                if (checkNode.getClass().getSimpleName().equals("TableBodyStack")) {
                    clickedEmptyTableArea = true;
                    break;
                }
                checkNode = checkNode.getParent();
            }

            while (curr != null && curr != root) {
                if ((curr == table && !clickedEmptyTableArea) || 
                    curr instanceof javafx.scene.control.Button || 
                    curr instanceof javafx.scene.control.TextField || 
                    curr instanceof javafx.scene.control.ComboBox ||
                    curr instanceof javafx.scene.control.DatePicker ||
                    curr instanceof javafx.scene.control.MenuBar) {
                    isSafe = true; 
                    break; 
                }
                curr = curr.getParent();
            }
            if (!isSafe) {
                table.getSelectionModel().clearSelection();
                root.requestFocus();
            }
        });

        root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                table.getSelectionModel().clearSelection();
                // Optionally request focus back to root to drop focus from text fields etc
                root.requestFocus();
            }
        });
    }

    private <T> Label createSortStatusLabel(javafx.scene.control.TableView<T> table) {
        Label sortStatusLabel = new Label("");
        sortStatusLabel.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 12px; -fx-padding: 0 10 0 0;");
        
        Runnable updateLabel = () -> {
            if (table.getSortOrder().isEmpty()) {
                sortStatusLabel.setText("");
            } else {
                javafx.scene.control.TableColumn<T, ?> col = table.getSortOrder().get(0);
                String order = col.getSortType() == javafx.scene.control.TableColumn.SortType.ASCENDING ? "Ascending" : "Descending";
                sortStatusLabel.setText(col.getText() + ": " + order);
            }
        };

        table.getSortOrder().addListener((javafx.collections.ListChangeListener<javafx.scene.control.TableColumn<T, ?>>) c -> updateLabel.run());
        
        for (javafx.scene.control.TableColumn<T, ?> col : table.getColumns()) {
            col.sortTypeProperty().addListener((obs, oldVal, newVal) -> updateLabel.run());
        }

        return sortStatusLabel;
    }

    private <T> Label createRowCountBox(javafx.scene.control.TableView<T> table) {
        Label label = new Label("Total: 0 rows");
        label.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 12px;");
        
        Runnable updateLabel = () -> {
            int total = table.getItems() != null ? table.getItems().size() : 0;
            int selected = table.getSelectionModel().getSelectedItems() != null ? table.getSelectionModel().getSelectedItems().size() : 0;
            if (selected > 0) {
                label.setText("Total: " + total + " rows (Selected: " + selected + ")");
            } else {
                label.setText("Total: " + total + " rows");
            }
        };

        javafx.collections.ListChangeListener<T> listChangeListener = c -> updateLabel.run();
        
        table.itemsProperty().addListener((obs, oldList, newList) -> {
            if (oldList != null) oldList.removeListener(listChangeListener);
            if (newList != null) newList.addListener(listChangeListener);
            updateLabel.run();
        });
        
        if (table.getItems() != null) {
            table.getItems().addListener(listChangeListener);
        }
        
        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<T>) c -> updateLabel.run());
        
        updateLabel.run();
        return label;
    }

    private void showAlert(javafx.scene.control.Alert.AlertType type, String title, String content) {
        // Use ToastService for non-blocking notifications
        switch (type) {
            case ERROR -> toastService.showError(content);
            case WARNING -> toastService.showWarning(content);
            case INFORMATION -> toastService.showSuccess(content);
            default -> toastService.showInfo(content);
        }
    }

    private boolean showConfirmDialog(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        // Custom Figma Graphic
        javafx.scene.layout.StackPane graphicContainer = new javafx.scene.layout.StackPane();
        graphicContainer.setPrefSize(48, 48);
        graphicContainer.setMaxSize(48, 48);
        graphicContainer.setStyle("-fx-background-color: #3B82F6; -fx-background-radius: 50%; -fx-effect: dropshadow(three-pass-box, rgba(59, 130, 246, 0.4), 10, 0, 0, 4);");

        javafx.scene.shape.SVGPath questionSVG = new javafx.scene.shape.SVGPath();
        questionSVG.setContent("M11,18h2v-2h-2V18z M12,2C6.48,2,2,6.48,2,12s4.48,10,10,10s10-4.48,10-10S17.52,2,12,2z M12,20c-4.41,0-8-3.59-8-8s3.59-8,8-8s8,3.59,8,8S16.41,20,12,20z M12,6c-2.21,0-4,1.79-4,4h2c0-1.1,0.9-2,2-2s2,0.9,2,2c0,2-3,1.75-3,5h2c0-2.25,3-2.5,3-5C16,7.79,14.21,6,12,6z");
        questionSVG.setFill(javafx.scene.paint.Color.WHITE);
        questionSVG.setScaleX(1.3);
        questionSVG.setScaleY(1.3);
        
        graphicContainer.getChildren().add(questionSVG);
        alert.setGraphic(graphicContainer);
        
        // Apply Global Styles
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("custom-alert");
        
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK;
    }
    private void showCheckoutDialog(Stage owner, double totalAmount, java.util.function.BiConsumer<com.pbl3.project.pbl3_project.entity.PaymentMethod, Boolean> onConfirm) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Checkout");

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white;");

        Label title = new Label("Payment");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #37474F;");

        Label totalLbl = new Label(String.format("Total to Pay: $%.2f", totalAmount));
        totalLbl.setStyle("-fx-font-size: 18px; -fx-text-fill: #D32F2F; -fx-font-weight: bold;");

        // Payment Method
        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.PaymentMethod> methodCombo = new javafx.scene.control.ComboBox<>();
        methodCombo.getItems().addAll(com.pbl3.project.pbl3_project.entity.PaymentMethod.values());
        methodCombo.setValue(com.pbl3.project.pbl3_project.entity.PaymentMethod.CASH);
        methodCombo.setStyle("-fx-font-size: 14px; -fx-pref-width: 250px;");

        // Cash Input
        VBox cashBox = new VBox(10);
        cashBox.setAlignment(Pos.CENTER_LEFT);
        TextField givenField = new TextField();
        givenField.setPromptText("Amount Given");
        givenField.setStyle("-fx-font-size: 14px; -fx-pref-width: 250px;");
        
        Label changeLbl = new Label("Change: $0.00");
        changeLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #388E3C; -fx-font-weight: bold;");

        cashBox.getChildren().addAll(new Label("Amount Given:"), givenField, changeLbl);

    javafx.scene.control.CheckBox printReceiptCb = new javafx.scene.control.CheckBox("Print Receipt (PDF)");
    printReceiptCb.setSelected(true);
    printReceiptCb.setStyle("-fx-font-size: 14px; -fx-text-fill: #37474F;");

    Button confirmBtn = new Button("PAY & PRINT");
        confirmBtn.getStyleClass().addAll("button", "success-button");
        confirmBtn.setDisable(true);
        confirmBtn.setPrefWidth(250);

        // Logic
        Runnable updateState = () -> {
             boolean isCash = methodCombo.getValue() == com.pbl3.project.pbl3_project.entity.PaymentMethod.CASH;
             cashBox.setVisible(isCash);
             cashBox.setManaged(isCash);
             if (!isCash) {
                 confirmBtn.setDisable(false);
             } else {
                 try {
                     double given = Double.parseDouble(givenField.getText());
                     if (given >= totalAmount) confirmBtn.setDisable(false);
                     else confirmBtn.setDisable(true);
                 } catch (Exception e) {
                     confirmBtn.setDisable(true);
                 }
             }
        };

        methodCombo.setOnAction(e -> updateState.run());

        // Validation
        givenField.textProperty().addListener((obs, old, val) -> {
            try {
                double given = Double.parseDouble(val);
                double change = given - totalAmount;
                changeLbl.setText(String.format("Change: $%.2f", change));
                if (change >= 0) {
                    confirmBtn.setDisable(false);
                    changeLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #388E3C; -fx-font-weight: bold;");
                } else {
                    confirmBtn.setDisable(true);
                    changeLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #D32F2F; -fx-font-weight: bold;");
                }
            } catch (NumberFormatException e) {
                confirmBtn.setDisable(true);
                changeLbl.setText("Invalid Amount");
            }
        });

        // Initialize state
        updateState.run();

        confirmBtn.setOnAction(e -> {
            onConfirm.accept(methodCombo.getValue(), printReceiptCb.isSelected());
            dialog.close();
        });
        
        Button cancelBtn = new Button("CANCEL");
        cancelBtn.getStyleClass().add("button");
        cancelBtn.setStyle("-fx-background-color: #CFD8DC; -fx-text-fill: #37474F;");
        cancelBtn.setPrefWidth(250);
        cancelBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(title, totalLbl, methodCombo, cashBox, printReceiptCb, confirmBtn, cancelBtn);

        Scene scene = new Scene(root, 400, 500);
        if (owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private javafx.scene.layout.VBox createImportOrderView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(35));
        root.setStyle("-fx-background-color: transparent;");

        // Toolbar
        javafx.scene.layout.BorderPane toolbar = new javafx.scene.layout.BorderPane();
        Label title = new Label("Import Orders");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #37474F;");

        // Expandable Search Bar for Import Orders
        javafx.scene.layout.HBox iSearchBox = new javafx.scene.layout.HBox(0);
        iSearchBox.setAlignment(Pos.CENTER);
        iSearchBox.getStyleClass().add("expandable-search-box");
        iSearchBox.setPrefSize(40, 40); iSearchBox.setMinSize(40, 40); iSearchBox.setMaxSize(40, 40);
        javafx.scene.shape.SVGPath iIcon = new javafx.scene.shape.SVGPath();
        iIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        iIcon.setFill(javafx.scene.paint.Color.web("#90A4AE"));
        javafx.scene.layout.Region iSpacer = new javafx.scene.layout.Region();
        iSpacer.setMinWidth(0); iSpacer.setPrefWidth(0);
        TextField iField = new TextField();
        iField.setPromptText("Search..."); iField.getStyleClass().add("search-text-field");
        iField.setMinWidth(0); iField.setMaxWidth(0); iField.setPrefWidth(0); iField.setOpacity(0);
        iSearchBox.getChildren().addAll(iIcon, iSpacer, iField);
        javafx.animation.Timeline iExpand = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(iSearchBox.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iSearchBox.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iSpacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline iCollapse = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(iSearchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iSearchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iSpacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)));
        iSearchBox.setOnMouseClicked(ev -> {
            if (iSearchBox.getMaxWidth() == 40) { iExpand.play(); iField.requestFocus(); }
            else if (ev.getTarget() == iIcon || ev.getTarget() == iSearchBox) { iField.clear(); root.requestFocus(); iCollapse.play(); }
        });

        // Filter Button (Supplier)
        javafx.scene.layout.HBox iFilterBox = new javafx.scene.layout.HBox();
        iFilterBox.setAlignment(Pos.CENTER);
        iFilterBox.getStyleClass().add("expandable-search-box");
        iFilterBox.setPrefSize(40, 40); iFilterBox.setMinSize(40, 40); iFilterBox.setMaxSize(40, 40);
        iFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath iFilterIcon = new javafx.scene.shape.SVGPath();
        iFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        iFilterIcon.setFill(javafx.scene.paint.Color.web("#90A4AE"));
        iFilterBox.getChildren().add(iFilterIcon);

        Button createBtn = new Button();
        createBtn.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(34,197,94,0.3), 12, 0, 0, 4); -fx-padding: 0;");
        createBtn.setPrefSize(40, 40);
        createBtn.setMinSize(40, 40);
        createBtn.setMaxSize(40, 40);
        createBtn.setCursor(javafx.scene.Cursor.HAND);
        
        javafx.scene.shape.SVGPath iPlusIcon = new javafx.scene.shape.SVGPath();
        iPlusIcon.setContent("M12 5v14M5 12h14");
        iPlusIcon.setStroke(javafx.scene.paint.Color.WHITE);
        iPlusIcon.setStrokeWidth(2.5);
        iPlusIcon.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        
        javafx.scene.layout.StackPane iIconWrapper = new javafx.scene.layout.StackPane(iPlusIcon);
        iIconWrapper.setPrefSize(40, 40);
        iIconWrapper.setMinSize(40, 40);
        iIconWrapper.setMaxSize(40, 40);
        
        Label iLabelText = new Label("New Import");
        iLabelText.setStyle("-fx-font-family: 'Be Vietnam Pro', 'Inter', sans-serif; -fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: white; -fx-padding: 0;");
        iLabelText.setOpacity(0);
        iLabelText.setTranslateX(-10);
        
        javafx.scene.layout.StackPane iBtnContent = new javafx.scene.layout.StackPane(iIconWrapper, iLabelText);
        javafx.scene.layout.StackPane.setAlignment(iIconWrapper, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setAlignment(iLabelText, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setMargin(iLabelText, new Insets(0, 0, 0, 36));
        
        javafx.scene.shape.Rectangle iBtnClip = new javafx.scene.shape.Rectangle();
        iBtnClip.setArcWidth(40);
        iBtnClip.setArcHeight(40);
        iBtnClip.widthProperty().bind(createBtn.widthProperty());
        iBtnClip.heightProperty().bind(createBtn.heightProperty());
        iBtnContent.setClip(iBtnClip);
        
        createBtn.setGraphic(iBtnContent);
        createBtn.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        createBtn.setAlignment(Pos.CENTER_LEFT);
        
        javafx.animation.Timeline iHoverInAnim = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(210),
                new javafx.animation.KeyValue(createBtn.minWidthProperty(), 140, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(createBtn.prefWidthProperty(), 140, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(createBtn.maxWidthProperty(), 140, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iPlusIcon.rotateProperty(), 90, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iLabelText.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iIconWrapper.translateXProperty(), 6, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iLabelText.translateXProperty(), 6, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        
        javafx.animation.Timeline iHoverOutAnim = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(210),
                new javafx.animation.KeyValue(createBtn.minWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(createBtn.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(createBtn.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iPlusIcon.rotateProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iLabelText.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iIconWrapper.translateXProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iLabelText.translateXProperty(), -10, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        
        createBtn.setOnMouseEntered(e -> {
            createBtn.setStyle("-fx-background-color: #16a34a; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(34,197,94,0.4), 20, 0, 0, 8); -fx-padding: 0;");
            iHoverOutAnim.stop();
            iHoverInAnim.play();
        });
        
        createBtn.setOnMouseExited(e -> {
            createBtn.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(34,197,94,0.3), 12, 0, 0, 4); -fx-padding: 0;");
            iHoverInAnim.stop();
            iHoverOutAnim.play();
        });
        
        createBtn.setOnMousePressed(e -> {
            createBtn.setScaleX(0.95);
            createBtn.setScaleY(0.95);
        });
        
        createBtn.setOnMouseReleased(e -> {
            createBtn.setScaleX(1.0);
            createBtn.setScaleY(1.0);
        });
        
        createBtn.setOnAction(e -> showCreateImportDialog(stage, user, () -> {
            // Refresh table
            // TO DO: Implement refresh
        }));

        javafx.scene.layout.HBox rightBox = new javafx.scene.layout.HBox(15, iFilterBox, iSearchBox, createBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        toolbar.setLeft(title);
        toolbar.setRight(rightBox);

        // Table
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.ImportOrder> table = new javafx.scene.control.TableView<>();
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrder, String> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty("IMP-" + data.getValue().getId()));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrder, String> suppCol = new javafx.scene.control.TableColumn<>("Supplier");
        suppCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getSupplier().getName()));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrder, String> dateCol = new javafx.scene.control.TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrder, String> costCol = new javafx.scene.control.TableColumn<>("Total Cost");
        costCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.format("%,.0f VND", data.getValue().getTotalCost())));

        table.getColumns().addAll(idCol, suppCol, dateCol, costCol);
        
        javafx.collections.ObservableList<com.pbl3.project.pbl3_project.entity.ImportOrder> importMaster = javafx.collections.FXCollections.observableArrayList(importOrderService.getAllImportOrders());
        table.setItems(importMaster);

        iField.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV.toLowerCase().trim();
            if (q.isEmpty()) { table.setItems(importMaster); } else {
                javafx.collections.ObservableList<com.pbl3.project.pbl3_project.entity.ImportOrder> filtered = javafx.collections.FXCollections.observableArrayList();
                for (com.pbl3.project.pbl3_project.entity.ImportOrder io : importMaster) {
                    if (("IMP-" + io.getId()).toLowerCase().contains(q) ||
                        (io.getSupplier() != null && io.getSupplier().getName().toLowerCase().contains(q)) ||
                        (io.getCreatedAt() != null && io.getCreatedAt().toString().toLowerCase().contains(q))) filtered.add(io);
                }
                table.setItems(filtered);
            }
        });

        javafx.scene.layout.HBox importStatusBar = new javafx.scene.layout.HBox(15, createSortStatusLabel(table), createRowCountBox(table));
        importStatusBar.setAlignment(Pos.CENTER_RIGHT);
        importStatusBar.setPadding(new Insets(5, 5, 0, 0));

        // Wire up supplier filter
        javafx.stage.Popup iFilterPopup = new javafx.stage.Popup();
        iFilterPopup.setAutoHide(true);

        iFilterBox.setOnMouseClicked(fev -> {
            if (iFilterPopup.isShowing()) {
                iFilterPopup.hide();
                return;
            }

            VBox popupContainer = new VBox(10);
            popupContainer.setPadding(new Insets(15));
            popupContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-border-color: #ECEFF1; -fx-border-radius: 8;");
            popupContainer.setPrefWidth(350);

            VBox scrollContent = new VBox(10);
            scrollContent.setStyle("-fx-background-color: white;");
            scrollContent.setPadding(new Insets(5, 15, 5, 15));
            javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(scrollContent);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: white; -fx-background-color: transparent; -fx-border-color: transparent;");
            scrollPane.setPrefViewportHeight(350);

            // --- Date Range ---
            Label dateTitle = new Label("Date Range");
            dateTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker();
            startDatePicker.setPromptText("Start Date");
            startDatePicker.setPrefWidth(140);
            startDatePicker.setStyle("-fx-font-size: 13px;");
            javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker();
            endDatePicker.setPromptText("End Date");
            endDatePicker.setPrefWidth(140);
            endDatePicker.setStyle("-fx-font-size: 13px;");
            customizeDatePicker(startDatePicker);
            customizeDatePicker(endDatePicker);
            javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
            dateBox.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.control.Separator sepDate = new javafx.scene.control.Separator();

            // --- Supplier Filter ---
            Label suppTitle = new Label("Suppliers");
            suppTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            
            javafx.scene.control.CheckBox allSuppCb = new javafx.scene.control.CheckBox("All Suppliers");
            allSuppCb.setSelected(true);
            allSuppCb.setStyle("-fx-font-size: 13px; -fx-text-fill: #455A64;");

            javafx.scene.control.ScrollPane suppScroll = new javafx.scene.control.ScrollPane();
            VBox suppBox = new VBox(8);
            suppBox.setPadding(new Insets(5, 5, 5, 20));
            suppScroll.setContent(suppBox);
            suppScroll.setFitToWidth(true);
            suppScroll.setMaxHeight(140);
            suppScroll.setStyle("-fx-background-color: transparent; -fx-background: white; -fx-border-color: #ECEFF1; -fx-border-radius: 4;");

            java.util.Set<String> supplierNames = new java.util.LinkedHashSet<>();
            for (com.pbl3.project.pbl3_project.entity.ImportOrder io : importMaster) {
                if (io.getSupplier() != null) supplierNames.add(io.getSupplier().getName());
            }

            java.util.List<javafx.scene.control.CheckBox> suppCbs = new java.util.ArrayList<>();
            for (String sName : supplierNames) {
                if (sName.trim().isEmpty()) continue;
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(sName);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: #455A64;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allSuppCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : suppCbs) if (!c.isSelected()) all = false;
                        allSuppCb.setSelected(all);
                    }
                });
                suppCbs.add(cb);
                suppBox.getChildren().add(cb);
            }

            allSuppCb.setOnAction(e -> {
                boolean sel = allSuppCb.isSelected();
                for (javafx.scene.control.CheckBox cb : suppCbs) cb.setSelected(sel);
            });

            javafx.scene.control.Separator sepSupp = new javafx.scene.control.Separator();

            // --- Price Range ---
            Label priceTitle = new Label("Total Cost Range");
            priceTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            
            double maxPrice = importMaster.stream().mapToDouble(io -> io.getTotalCost() != null ? io.getTotalCost() : 0).max().orElse(1000000);
            if (maxPrice == 0) maxPrice = 1000;
            
            Label priceLabel = new Label("0 - " + String.format("%.0f", maxPrice) + " VND");
            priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1976D2; -fx-font-weight: bold;");
            
            RangeSlider priceSlider = new RangeSlider(0, maxPrice, 0, maxPrice, 290);
            priceSlider.minVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%.0f - %.0f VND", nv.doubleValue(), priceSlider.maxVal.get())));
            priceSlider.maxVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%.0f - %.0f VND", priceSlider.minVal.get(), nv.doubleValue())));

            scrollContent.getChildren().addAll(
                dateTitle, dateBox, sepDate,
                suppTitle, allSuppCb, suppScroll, sepSupp,
                priceTitle, priceLabel, priceSlider
            );

            // --- Buttons ---
            javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
            btnRow.setAlignment(Pos.CENTER_RIGHT);
            
            final double fMaxPrice = maxPrice;

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(ae -> {
                iFilterBox.setStyle("");
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                allSuppCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : suppCbs) cb.setSelected(true);
                priceSlider.minVal.set(0); priceSlider.maxVal.set(fMaxPrice);
                table.setItems(importMaster);
                iFilterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(ae -> {
                java.util.Set<String> selectedSupps = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : suppCbs) {
                    if (cb.isSelected()) selectedSupps.add(cb.getText());
                }
                double pMin = priceSlider.minVal.get();
                double pMax = priceSlider.maxVal.get();
                java.time.LocalDate sDate = startDatePicker.getValue();
                java.time.LocalDate eDate = endDatePicker.getValue();

                java.util.List<com.pbl3.project.pbl3_project.entity.ImportOrder> result = importMaster.stream()
                    .filter(io -> {
                        String sName = (io.getSupplier() != null) ? io.getSupplier().getName() : null;
                        return sName == null || selectedSupps.contains(sName);
                    })
                    .filter(io -> {
                        double price = io.getTotalCost() != null ? io.getTotalCost() : 0;
                        return price >= pMin && price <= pMax;
                    })
                    .filter(io -> {
                        if (io.getCreatedAt() == null) return true;
                        java.time.LocalDate ioDate = io.getCreatedAt().toLocalDate();
                        if (sDate != null && ioDate.isBefore(sDate)) return false;
                        if (eDate != null && ioDate.isAfter(eDate)) return false;
                        return true;
                    })
                    .collect(java.util.stream.Collectors.toList());

                table.setItems(javafx.collections.FXCollections.observableArrayList(result));

                boolean hasFilter = !allSuppCb.isSelected() || pMin > 0 || pMax < fMaxPrice || sDate != null || eDate != null;
                iFilterBox.setStyle(hasFilter ? "-fx-border-color: #1976D2;" : "");
                iFilterPopup.hide();
            });

            btnRow.getChildren().addAll(resetBtn, applyBtn);

            popupContainer.getChildren().addAll(scrollPane, btnRow);
            iFilterPopup.getContent().clear();
            iFilterPopup.getContent().add(popupContainer);

            javafx.geometry.Bounds bounds = iFilterBox.localToScreen(iFilterBox.getBoundsInLocal());
            iFilterPopup.show(iFilterBox, bounds.getMinX() - 330 + 40, bounds.getMaxY() + 5);
        });

        root.getChildren().addAll(toolbar, table, importStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private void showCreateImportDialog(Stage owner, com.pbl3.project.pbl3_project.entity.User user, Runnable onSuccess) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("New Import Order");

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setPrefWidth(900);
        root.setPrefHeight(600);
        root.setStyle("-fx-background-color: white;");

        // Top: Supplier
        javafx.scene.layout.HBox topBox = new javafx.scene.layout.HBox(10);
        topBox.setAlignment(Pos.CENTER_LEFT);
        Label suppLbl = new Label("Select Supplier:");
        suppLbl.setStyle("-fx-font-weight: bold;");
        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Supplier> supplierCombo = new javafx.scene.control.ComboBox<>();
        supplierCombo.setPrefWidth(300);
        supplierCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(com.pbl3.project.pbl3_project.entity.Supplier s) { return s == null ? "" : s.getName(); }
            @Override public com.pbl3.project.pbl3_project.entity.Supplier fromString(String s) { return null; }
        });
        supplierCombo.getItems().addAll(supplierService.getAllSuppliers());
        topBox.getChildren().addAll(suppLbl, supplierCombo);

        class TempItem {
            com.pbl3.project.pbl3_project.entity.Product product;
            int quantity;
            double importPrice;
            TempItem(com.pbl3.project.pbl3_project.entity.Product p, int q, double ip) { this.product=p; this.quantity=q; this.importPrice=ip; }
            public double getTotal() { return quantity * importPrice; }
        }

        javafx.scene.control.TableView<TempItem> table = new javafx.scene.control.TableView<>();
        Label totalLabel = new Label("Total Cost: 0 VND");
        totalLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #D32F2F;");

        Runnable updateTotalAction = () -> {
            double t = table.getItems().stream().mapToDouble(TempItem::getTotal).sum();
            totalLabel.setText(String.format("Total Cost: %,.0f VND", t));
        };

        javafx.scene.control.TableColumn<TempItem, String> nameCol = new javafx.scene.control.TableColumn<>("Product");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().product.getName()));
        
        javafx.scene.control.TableColumn<TempItem, Integer> qtyCol = new javafx.scene.control.TableColumn<>("Qty");
        qtyCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().quantity));
        
        javafx.scene.control.TableColumn<TempItem, String> priceCol = new javafx.scene.control.TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.format("%,.0f VND", data.getValue().importPrice)));

        javafx.scene.control.TableColumn<TempItem, String> totalCol = new javafx.scene.control.TableColumn<>("Total");
        totalCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.format("%,.0f VND", data.getValue().getTotal())));

        javafx.scene.control.TableColumn<TempItem, Void> actionCol = new javafx.scene.control.TableColumn<>("Action");
        actionCol.setCellFactory(param -> new javafx.scene.control.TableCell<>() {
            private final Button btn = new Button("Remove");
            {
                btn.getStyleClass().addAll("button", "danger-button");
                btn.setOnAction(e -> {
                    TempItem item = getTableView().getItems().get(getIndex());
                    table.getItems().remove(item);
                    updateTotalAction.run();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        table.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol, actionCol);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);

        // Add Item Form
        javafx.scene.layout.HBox addBox = new javafx.scene.layout.HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);
        
        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Product> productCombo = new javafx.scene.control.ComboBox<>();
        productCombo.setPrefWidth(250);
        productCombo.setPromptText("Select Product");
        productCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(com.pbl3.project.pbl3_project.entity.Product p) { return p == null ? "" : p.getName() + " (Stock: " + p.getQuantity() + ")"; }
            @Override public com.pbl3.project.pbl3_project.entity.Product fromString(String string) { return null; }
        });
        productCombo.getItems().addAll(productService.getAllProducts());

        TextField qtyField = new TextField(); qtyField.setPromptText("Quantity"); qtyField.setPrefWidth(100);
        TextField priceField = new TextField(); priceField.setPromptText("Import Price"); priceField.setPrefWidth(120);

        productCombo.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.Product p = productCombo.getValue();
            if (p != null && p.getImportPrice() != null) priceField.setText(String.valueOf(p.getImportPrice()));
        });

        Button addBtn = new Button("Add Item");
        addBtn.getStyleClass().addAll("button", "secondary-button");
        addBtn.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.Product p = productCombo.getValue();
            if (p == null) { toastService.showWarning("Select a product"); return; }
            try {
                int q = Integer.parseInt(qtyField.getText());
                double pr = Double.parseDouble(priceField.getText());
                if (q <= 0 || pr < 0) throw new NumberFormatException();
                table.getItems().add(new TempItem(p, q, pr));
                updateTotalAction.run();
                productCombo.setValue(null); qtyField.clear(); priceField.clear();
            } catch (Exception ex) {
                toastService.showError("Invalid quantity or price");
            }
        });
        addBox.getChildren().addAll(productCombo, qtyField, priceField, addBtn);

        // Bottom Actions
        javafx.scene.layout.HBox actionBox = new javafx.scene.layout.HBox(15);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button");
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button("Confirm Import");
        confirmBtn.getStyleClass().addAll("button", "success-button");
        confirmBtn.setOnAction(e -> {
            if (supplierCombo.getValue() == null) { toastService.showWarning("Select a supplier!"); return; }
            if (table.getItems().isEmpty()) { toastService.showWarning("Add at least one product!"); return; }
            try {
                com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest req = new com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest();
                req.setUserId(user.getId());
                req.setSupplierId(supplierCombo.getValue().getId());
                req.setNotes("Import via UI");
                java.util.List<com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest.ImportOrderItemRequest> items = new java.util.ArrayList<>();
                for (TempItem ti : table.getItems()) {
                    var itReq = new com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest.ImportOrderItemRequest();
                    itReq.setProductId(ti.product.getId());
                    itReq.setQuantity(ti.quantity);
                    itReq.setImportPrice(ti.importPrice);
                    items.add(itReq);
                }
                req.setItems(items);
                importOrderService.createImportOrder(req);
                toastService.showSuccess("Import Order Created!");
                onSuccess.run();
                dialog.close();
            } catch (Exception ex) {
                toastService.showError("Failed: " + ex.getMessage());
            }
        });

        actionBox.getChildren().addAll(totalLabel, new javafx.scene.layout.Region(), cancelBtn, confirmBtn);
        javafx.scene.layout.HBox.setHgrow(actionBox.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);

        root.getChildren().addAll(topBox, addBox, table, actionBox);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        
        Scene scene = new Scene(root);
        if (owner.getScene() != null) scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showStockHistoryScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox content = createStockHistoryView(stage, user);
        switchScene(stage, user, "Stock History", "nav-stock-history", content);
    }

    private VBox createStockHistoryView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));

        Label header = new Label("Inventory Transactions Log");
        header.getStyleClass().add("header-label");

        // Expandable Search Bar for Stock History
        javafx.scene.layout.HBox hSearchBox = new javafx.scene.layout.HBox(0);
        hSearchBox.setAlignment(Pos.CENTER);
        hSearchBox.getStyleClass().add("expandable-search-box");
        hSearchBox.setPrefSize(40, 40); hSearchBox.setMinSize(40, 40); hSearchBox.setMaxSize(40, 40);
        javafx.scene.shape.SVGPath hIcon = new javafx.scene.shape.SVGPath();
        hIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        hIcon.setFill(javafx.scene.paint.Color.web("#90A4AE"));
        javafx.scene.layout.Region hSpacer = new javafx.scene.layout.Region();
        hSpacer.setMinWidth(0); hSpacer.setPrefWidth(0);
        TextField hField = new TextField();
        hField.setPromptText("Search..."); hField.getStyleClass().add("search-text-field");
        hField.setMinWidth(0); hField.setMaxWidth(0); hField.setPrefWidth(0); hField.setOpacity(0);
        hSearchBox.getChildren().addAll(hIcon, hSpacer, hField);
        javafx.animation.Timeline hExpand = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(hSearchBox.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(hSearchBox.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(hSpacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(hField.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(hField.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(hField.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(hField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline hCollapse = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(hSearchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(hSearchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(hSpacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(hField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(hField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(hField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(hField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)));
        hSearchBox.setOnMouseClicked(ev -> {
            if (hSearchBox.getMaxWidth() == 40) { hExpand.play(); hField.requestFocus(); }
            else if (ev.getTarget() == hIcon || ev.getTarget() == hSearchBox) { hField.clear(); root.requestFocus(); hCollapse.play(); }
        });

        javafx.scene.layout.BorderPane topBar = new javafx.scene.layout.BorderPane();
        // Filter Button (Transaction Type)
        javafx.scene.layout.HBox hFilterBox = new javafx.scene.layout.HBox();
        hFilterBox.setAlignment(Pos.CENTER);
        hFilterBox.getStyleClass().add("expandable-search-box");
        hFilterBox.setPrefSize(40, 40); hFilterBox.setMinSize(40, 40); hFilterBox.setMaxSize(40, 40);
        hFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath hFilterIcon = new javafx.scene.shape.SVGPath();
        hFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        hFilterIcon.setFill(javafx.scene.paint.Color.web("#90A4AE"));
        hFilterBox.getChildren().add(hFilterIcon);

        javafx.scene.layout.HBox hRightBox = new javafx.scene.layout.HBox(15, hFilterBox, hSearchBox);
        hRightBox.setAlignment(Pos.CENTER_RIGHT);
        topBar.setLeft(header); topBar.setRight(hRightBox);
        javafx.scene.layout.BorderPane.setAlignment(header, Pos.CENTER_LEFT);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.InventoryTransaction> table = new javafx.scene.control.TableView<>();
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> dateCol = new javafx.scene.control.TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> {
            java.time.LocalDateTime dt = data.getValue().getCreatedAt();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return new javafx.beans.property.SimpleStringProperty(dt != null ? dt.format(formatter) : "");
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> typeCol = new javafx.scene.control.TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTransactionType()));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> productCol = new javafx.scene.control.TableColumn<>("Product");
        productCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getProduct() != null ? data.getValue().getProduct().getName() : "Unknown"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> qtyCol = new javafx.scene.control.TableColumn<>("Change");
        qtyCol.setCellValueFactory(data -> {
            Integer changeParam = data.getValue().getQuantityChange();
            int change = changeParam != null ? changeParam : 0;
            String prefix = change > 0 ? "+" : "";
            return new javafx.beans.property.SimpleStringProperty(prefix + change);
        });
        qtyCol.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> userCol = new javafx.scene.control.TableColumn<>("User");
        userCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getUser() != null ? data.getValue().getUser().getUsername() : "System"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> notesCol = new javafx.scene.control.TableColumn<>("Notes/Ref");
        notesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNotes()));

        table.getColumns().addAll(dateCol, typeCol, productCol, qtyCol, userCol, notesCol);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);

        // Load Data
        javafx.collections.ObservableList<com.pbl3.project.pbl3_project.entity.InventoryTransaction> txMaster = javafx.collections.FXCollections.observableArrayList(transactionService.getAllTransactions());
        table.setItems(txMaster);

        hField.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV.toLowerCase().trim();
            if (q.isEmpty()) { table.setItems(txMaster); } else {
                javafx.collections.ObservableList<com.pbl3.project.pbl3_project.entity.InventoryTransaction> filtered = javafx.collections.FXCollections.observableArrayList();
                for (com.pbl3.project.pbl3_project.entity.InventoryTransaction tx : txMaster) {
                    String product = tx.getProduct() != null ? tx.getProduct().getName() : "";
                    String type = tx.getTransactionType() != null ? tx.getTransactionType() : "";
                    String userName = tx.getUser() != null ? tx.getUser().getUsername() : "";
                    String notes = tx.getNotes() != null ? tx.getNotes() : "";
                    if (product.toLowerCase().contains(q) || type.toLowerCase().contains(q) ||
                        userName.toLowerCase().contains(q) || notes.toLowerCase().contains(q)) filtered.add(tx);
                }
                table.setItems(filtered);
            }
        });

        javafx.scene.layout.HBox txStatusBar = new javafx.scene.layout.HBox(15, createSortStatusLabel(table), createRowCountBox(table));
        txStatusBar.setAlignment(Pos.CENTER_RIGHT);
        txStatusBar.setPadding(new Insets(5, 5, 0, 0));

        // Wire up transaction type filter
        javafx.stage.Popup hFilterPopup = new javafx.stage.Popup();
        hFilterPopup.setAutoHide(true);

        hFilterBox.setOnMouseClicked(fev -> {
            if (hFilterPopup.isShowing()) {
                hFilterPopup.hide();
                return;
            }

            VBox popupContainer = new VBox(10);
            popupContainer.setPadding(new Insets(15));
            popupContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-border-color: #ECEFF1; -fx-border-radius: 8;");
            popupContainer.setPrefWidth(350);

            VBox scrollContent = new VBox(10);
            scrollContent.setStyle("-fx-background-color: white;");
            scrollContent.setPadding(new Insets(5, 15, 5, 15));
            javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(scrollContent);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: white; -fx-background-color: transparent; -fx-border-color: transparent;");
            scrollPane.setPrefViewportHeight(350);

            // --- Date Range ---
            Label dateTitle = new Label("Date Range");
            dateTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker();
            startDatePicker.setPromptText("Start Date");
            startDatePicker.setPrefWidth(140);
            startDatePicker.setStyle("-fx-font-size: 13px;");
            javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker();
            endDatePicker.setPromptText("End Date");
            endDatePicker.setPrefWidth(140);
            endDatePicker.setStyle("-fx-font-size: 13px;");
            customizeDatePicker(startDatePicker);
            customizeDatePicker(endDatePicker);
            javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
            dateBox.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.control.Separator sepDate = new javafx.scene.control.Separator();

            // --- User Filter ---
            Label userTitle = new Label("User");
            userTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            
            javafx.scene.control.CheckBox allUsersCb = new javafx.scene.control.CheckBox("All Users");
            allUsersCb.setSelected(true);
            allUsersCb.setStyle("-fx-font-size: 13px; -fx-text-fill: #455A64;");

            VBox userScroll = new VBox(8);
            userScroll.setPadding(new Insets(5, 5, 5, 20));
            
            java.util.List<javafx.scene.control.CheckBox> userCbs = new java.util.ArrayList<>();
            java.util.Set<String> userNames = txMaster.stream()
                .map(tx -> tx.getUser() != null ? tx.getUser().getUsername() : "System")
                .collect(java.util.stream.Collectors.toSet());
            
            for (String uName : userNames) {
                if (uName.trim().isEmpty()) continue;
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(uName);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: #455A64;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allUsersCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : userCbs) if (!c.isSelected()) all = false;
                        allUsersCb.setSelected(all);
                    }
                });
                userCbs.add(cb);
                userScroll.getChildren().add(cb);
            }
            allUsersCb.setOnAction(e -> {
                boolean sel = allUsersCb.isSelected();
                for (javafx.scene.control.CheckBox cb : userCbs) cb.setSelected(sel);
            });

            javafx.scene.control.Separator sepUser = new javafx.scene.control.Separator();

            // --- Transaction Type Filter ---
            Label typeTitle = new Label("Transaction Type");
            typeTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            
            javafx.scene.control.CheckBox allTypesCb = new javafx.scene.control.CheckBox("All Types");
            allTypesCb.setSelected(true);
            allTypesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: #455A64;");

            VBox typeScroll = new VBox(8);
            typeScroll.setPadding(new Insets(5, 5, 5, 20));
            
            java.util.List<javafx.scene.control.CheckBox> typeCbs = new java.util.ArrayList<>();
            String[] types = {"IMPORT", "SALE", "ADJUSTMENT"};
            for (String type : types) {
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(type);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: #455A64;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allTypesCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : typeCbs) if (!c.isSelected()) all = false;
                        allTypesCb.setSelected(all);
                    }
                });
                typeCbs.add(cb);
                typeScroll.getChildren().add(cb);
            }

            allTypesCb.setOnAction(e -> {
                boolean sel = allTypesCb.isSelected();
                for (javafx.scene.control.CheckBox cb : typeCbs) cb.setSelected(sel);
            });

            javafx.scene.control.Separator sepType = new javafx.scene.control.Separator();

            // --- Quantity Range Filter ---
            Label qtyTitle = new Label("Quantity Change Range");
            qtyTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
            
            double maxQty = txMaster.stream().mapToDouble(tx -> Math.abs(tx.getQuantityChange() != null ? tx.getQuantityChange() : 0)).max().orElse(100);
            if (maxQty == 0) maxQty = 100;
            
            Label qtyLabel = new Label("0 - " + String.format("%.0f", maxQty));
            qtyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1976D2; -fx-font-weight: bold;");
            
            RangeSlider qtySlider = new RangeSlider(0, maxQty, 0, maxQty, 280);
            qtySlider.minVal.addListener((o, ov, nv) -> qtyLabel.setText(String.format("%.0f - %.0f", nv.doubleValue(), qtySlider.maxVal.get())));
            qtySlider.maxVal.addListener((o, ov, nv) -> qtyLabel.setText(String.format("%.0f - %.0f", qtySlider.minVal.get(), nv.doubleValue())));

            scrollContent.getChildren().addAll(
                dateTitle, dateBox, sepDate,
                userTitle, allUsersCb, userScroll, sepUser,
                typeTitle, allTypesCb, typeScroll, sepType,
                qtyTitle, qtyLabel, qtySlider
            );

            // --- Buttons ---
            javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
            btnRow.setAlignment(Pos.CENTER_RIGHT);
            
            final double fMaxQty = maxQty;

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(ae -> {
                hFilterBox.setStyle("");
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                allUsersCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : userCbs) cb.setSelected(true);
                allTypesCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : typeCbs) cb.setSelected(true);
                qtySlider.minVal.set(0); qtySlider.maxVal.set(fMaxQty);
                table.setItems(txMaster);
                hFilterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(ae -> {
                java.util.Set<String> selectedUsers = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : userCbs) {
                    if (cb.isSelected()) selectedUsers.add(cb.getText());
                }
                
                java.util.Set<String> selectedTypes = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : typeCbs) {
                    if (cb.isSelected()) selectedTypes.add(cb.getText());
                }
                double qMin = qtySlider.minVal.get();
                double qMax = qtySlider.maxVal.get();
                java.time.LocalDate sDate = startDatePicker.getValue();
                java.time.LocalDate eDate = endDatePicker.getValue();

                java.util.List<com.pbl3.project.pbl3_project.entity.InventoryTransaction> result = txMaster.stream()
                    .filter(tx -> {
                        String uName = tx.getUser() != null ? tx.getUser().getUsername() : "System";
                        return selectedUsers.contains(uName);
                    })
                    .filter(tx -> {
                        String tType = tx.getTransactionType() != null ? tx.getTransactionType() : null;
                        return tType == null || selectedTypes.contains(tType);
                    })
                    .filter(tx -> {
                        double qty = Math.abs(tx.getQuantityChange() != null ? tx.getQuantityChange() : 0);
                        return qty >= qMin && qty <= qMax;
                    })
                    .filter(tx -> {
                        if (tx.getCreatedAt() == null) return true;
                        java.time.LocalDate txDate = tx.getCreatedAt().toLocalDate();
                        if (sDate != null && txDate.isBefore(sDate)) return false;
                        if (eDate != null && txDate.isAfter(eDate)) return false;
                        return true;
                    })
                    .collect(java.util.stream.Collectors.toList());

                table.setItems(javafx.collections.FXCollections.observableArrayList(result));

                boolean hasFilter = !allTypesCb.isSelected() || !allUsersCb.isSelected() || qMin > 0 || qMax < fMaxQty || sDate != null || eDate != null;
                hFilterBox.setStyle(hasFilter ? "-fx-border-color: #1976D2;" : "");
                hFilterPopup.hide();
            });

            btnRow.getChildren().addAll(resetBtn, applyBtn);

            popupContainer.getChildren().addAll(scrollPane, btnRow);
            hFilterPopup.getContent().clear();
            hFilterPopup.getContent().add(popupContainer);

            javafx.geometry.Bounds bounds = hFilterBox.localToScreen(hFilterBox.getBoundsInLocal());
            hFilterPopup.show(hFilterBox, bounds.getMinX() - 330 + 40, bounds.getMaxY() + 5);
        });

        root.getChildren().addAll(topBar, table, txStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        enableDeselectOnOutsideClick(root, table);
        return root;
    }
}
