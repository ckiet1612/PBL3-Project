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
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import com.pbl3.project.pbl3_project.service.*;

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
                            UnitService unitService) {
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
    }
    


    private void showDashboardScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        // ... (Table construction logic remains similar but compacted for brevity if needed, or kept same) ...
        // To avoid massive diffs, I will keep the content creation logic here but change the FINAL STEP to call switchScene
        
        // Product Table Wrapper
        javafx.scene.Node content = createProductView(stage, user);
        switchScene(stage, user, "Products", "nav-products", content);
    }

    private void showOrderHistoryScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox content = createOrderHistoryView(stage, user);
        switchScene(stage, user, "Order History", "nav-history", content);
    }
    
    private void showSalesScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.Node content = createSalesView(user);
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
        
        javafx.scene.control.TableView<T> table = new javafx.scene.control.TableView<>();
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);
        
        javafx.scene.control.TableColumn<T, Integer> sttCol = new javafx.scene.control.TableColumn<>("STT");
        sttCol.setSortable(false);
        sttCol.setCellValueFactory(column -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
            table.getItems().indexOf(column.getValue()) + 1));
        
        javafx.scene.control.TableColumn<T, String> nameCol = new javafx.scene.control.TableColumn<>("Name");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<T> row = new javafx.scene.control.TableRow<>();
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
            
            javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete");
            deleteItem.setStyle("-fx-text-fill: #F44336;");
            deleteItem.setOnAction(event -> {
                java.util.List<T> selectedItems = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
                if (selectedItems.isEmpty()) {
                    // Fallback to row item if selection is empty (edge case)
                    if (row.getItem() != null) selectedItems.add(row.getItem());
                    else return;
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
                    table.setItems(javafx.collections.FXCollections.observableArrayList(dataFetcher.get()));
                }
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
        table.setItems(javafx.collections.FXCollections.observableArrayList(dataFetcher.get()));
        
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
                table.setItems(javafx.collections.FXCollections.observableArrayList(dataFetcher.get()));
            } catch (Exception ex) {
                toastService.showError("Error: " + ex.getMessage());
            }
        });
        
        addBox.getChildren().addAll(nameField, addBtn);
        root.getChildren().addAll(header, addBox, table);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private VBox createSupplierMasterDataView(Stage stage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(35));
        
        Label header = new Label("Supplier Management");
        header.getStyleClass().add("header-label");
        
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Supplier> table = new javafx.scene.control.TableView<>();
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

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
        
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Supplier> row = new javafx.scene.control.TableRow<>();
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
            
            javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete");
            deleteItem.setStyle("-fx-text-fill: #F44336;");
            deleteItem.setOnAction(event -> {
                java.util.List<com.pbl3.project.pbl3_project.entity.Supplier> selectedItems = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
                 if (selectedItems.isEmpty()) {
                    if (row.getItem() != null) selectedItems.add(row.getItem());
                    else return;
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
                     table.setItems(javafx.collections.FXCollections.observableArrayList(
                         supplierService.getAllSuppliers().stream()
                             .sorted(java.util.Comparator.comparing(com.pbl3.project.pbl3_project.entity.Supplier::getId))
                             .toList()
                     ));
                }
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
        table.setItems(javafx.collections.FXCollections.observableArrayList(
            supplierService.getAllSuppliers().stream()
                .sorted(java.util.Comparator.comparing(com.pbl3.project.pbl3_project.entity.Supplier::getId))
                .toList()
        ));
        
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
                table.setItems(javafx.collections.FXCollections.observableArrayList(
                    supplierService.getAllSuppliers().stream()
                        .sorted(java.util.Comparator.comparing(com.pbl3.project.pbl3_project.entity.Supplier::getId))
                        .toList()
                ));
            } catch (Exception ex) {
                toastService.showError("Error: " + ex.getMessage());
            }
        });
        
        addBox.getChildren().addAll(nameField, phoneField, addrField, addBtn);
        root.getChildren().addAll(header, addBox, table);
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
        
        Button addButton = new Button("+ Add Product");
        addButton.getStyleClass().addAll("button", "success-button");
        toolbar.setRight(addButton);
        
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Product> table = new javafx.scene.control.TableView<>();
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Long> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> nameCol = new javafx.scene.control.TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Double> priceCol = new javafx.scene.control.TableColumn<>("Price");
        priceCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Integer> qtyCol = new javafx.scene.control.TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Product> row = new javafx.scene.control.TableRow<>();
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
            
            javafx.scene.control.MenuItem editItem = new javafx.scene.control.MenuItem("Edit");
            editItem.setStyle("-fx-text-fill: #1976D2;");
            editItem.setOnAction(event -> {
                com.pbl3.project.pbl3_project.entity.Product product = row.getItem();
                showProductDialog(stage, product, product.getCategory(), () -> {
                     if (productListView.getUserData() instanceof Runnable) ((Runnable)productListView.getUserData()).run();
                });
            });

            javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete");
            deleteItem.setStyle("-fx-text-fill: #F44336;");
            deleteItem.setOnAction(event -> {
                java.util.List<com.pbl3.project.pbl3_project.entity.Product> selectedItems = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
                if (selectedItems.isEmpty()) {
                     if (row.getItem() != null) selectedItems.add(row.getItem());
                     else return;
                }

                int deletedCount = 0;
                for (com.pbl3.project.pbl3_project.entity.Product product : selectedItems) {
                    try {
                        productService.deleteProduct(product.getId());
                        table.getItems().remove(product);
                        deletedCount++;
                    } catch (Exception ex) {
                        toastService.showError("Could not delete product: " + ex.getMessage());
                    }
                }
                
                if (deletedCount > 0) {
                    toastService.showSuccess("Deleted " + deletedCount + " products!");
                }
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
        productListView.getChildren().addAll(toolbar, table);
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
        
        categoryView.getChildren().addAll(title, categoryGrid);
        
        // --- Logic: Navigation & Refresh ---
        final com.pbl3.project.pbl3_project.entity.Category[] selectedCategory = {null};
        
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
                nameLbl.getStyleClass().add("category-card-title");
                
                Label countLbl = new Label(count + " Products");
                countLbl.getStyleClass().add("category-card-count");
                
                card.getChildren().addAll(nameLbl, countLbl);
                
                card.setOnMouseClicked(e -> {
                    // Navigate to Product List
                    selectedCategory[0] = cat;
                    categoryTitle.setText(cat.getName());
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
        };
        
        // Refresh product list logic (store as userData)
        Runnable refreshProductList = () -> {
            if (selectedCategory[0] != null) {
                java.util.List<com.pbl3.project.pbl3_project.entity.Product> filtered = productService.getAllProducts().stream()
                        .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(selectedCategory[0].getId()))
                        .collect(java.util.stream.Collectors.toList());
                table.setItems(javafx.collections.FXCollections.observableArrayList(filtered));
            }
        };
        productListView.setUserData(refreshProductList);
        
        backBtn.setOnAction(e -> {
            productListView.setVisible(false);
            categoryView.setVisible(true);
            selectedCategory[0] = null;
            loadCategories.run(); // Refresh counts
        });
        
        addButton.setOnAction(e -> showProductDialog(stage, null, selectedCategory[0], () -> {
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
        long lowStockCount = reportService.getLowStockCount(10); // threshold: 10 items
        
        // Create 3 stat cards
        VBox revenueCard = createDashboardCard("Today's Revenue", 
            String.format("%,.0f VND", todayRevenue), "#4CAF50");
        VBox ordersCard = createDashboardCard("Orders Today", 
            String.valueOf(todayOrders), "#2196F3");
        VBox lowStockCard = createDashboardCard("Low Stock Items", 
            String.valueOf(lowStockCount), lowStockCount > 0 ? "#FF9800" : "#9E9E9E");
        
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
        
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Revenue");
        
        java.util.Map<String, Double> last7Days = reportService.getLast7DaysRevenue();
        for (java.util.Map.Entry<String, Double> entry : last7Days.entrySet()) {
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        barChart.getData().add(series);
        VBox.setVgrow(barChart, javafx.scene.layout.Priority.ALWAYS);
        
        // Style chart bars with green color
        barChart.lookupAll(".chart-bar").forEach(node -> 
            node.setStyle("-fx-bar-fill: #4CAF50;"));
        
        content.getChildren().addAll(statsRow, chartTitle, barChart);
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
        table.setItems(javafx.collections.FXCollections.observableArrayList(orderService.getAllOrders()));

        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                var selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    try {
                        showOrderDetailsDialog(stage, orderService.getOrderWithItems(selected.getId()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not load order details: " + ex.getMessage());
                    }
                }
            }
        });

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().add(table);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        return content;
    }
    
    private javafx.scene.Node createSalesView(com.pbl3.project.pbl3_project.entity.User user) {
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
            try {
                com.pbl3.project.pbl3_project.dto.CreateOrderRequest req = new com.pbl3.project.pbl3_project.dto.CreateOrderRequest();
                req.setUserId(user.getId());
                req.setItems(new java.util.ArrayList<>(cartItems));
                orderService.createOrder(req);
                toastService.showSuccess("Order created successfully!");
                cartItems.clear();
                allProducts.clear();
                allProducts.addAll(productService.getAllProducts());
                renderProducts.run();
            } catch (Exception ex) {
                toastService.showError("Order Failed: " + ex.getMessage());
            }
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
        for (String id : new String[]{"nav-dashboard", "nav-products", "nav-sales", "nav-attributes", "nav-history"}) {
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
        Button navSales = createNavButton("Sales (POS)", "nav-sales", () -> showSalesScene(stage, user));
        Button navAttributes = createNavButton("Master Data", "nav-attributes", () -> showAttributesScene(stage, user));
        Button navHistory = createNavButton("Order History", "nav-history", () -> showOrderHistoryScene(stage, user));
        Button navLogout = createNavButton("Logout", "nav-logout", () -> showLoginScene(stage));
        navLogout.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");

        // Initial Active State
        if ("nav-dashboard".equals(activeNavId)) navDashboard.getStyleClass().add("active");
        if ("nav-products".equals(activeNavId)) navProducts.getStyleClass().add("active");
        if ("nav-sales".equals(activeNavId)) navSales.getStyleClass().add("active");
        if ("nav-attributes".equals(activeNavId)) navAttributes.getStyleClass().add("active");
        if ("nav-history".equals(activeNavId)) navHistory.getStyleClass().add("active");

        sidebar.getChildren().addAll(appTitle, navDashboard, navProducts, navSales, navAttributes, navHistory, new javafx.scene.control.Separator(), navLogout);
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
        
        root.setStyle("-fx-background-color: #F5F5F5;");
        
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
        mainRoot.setStyle("-fx-background-color: #F5F5F5;");

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

        // Input Fields
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("text-field");
        usernameField.setMaxWidth(350);
        
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("text-field");
        passwordField.setMaxWidth(350);

        // Login Button
        Button loginButton = new Button("LOGIN");
        loginButton.getStyleClass().addAll("button", "primary-button");
        loginButton.setMaxWidth(350);
        loginButton.setDefaultButton(true);
        
        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            var user = authService.login(username, password);
            
            if (user != null) {
                showOverviewScene(stage, user);
            } else {
                errorLabel.setText("Invalid credentials!");
            }
        });

        loginBox.getChildren().addAll(titleLabel, errorLabel, usernameField, passwordField, loginButton);
        mainRoot.getChildren().add(loginBox);

        Scene scene = new Scene(mainRoot, 450, 350); // Wider window
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
    }


    private void showProductDialog(Stage owner, com.pbl3.project.pbl3_project.entity.Product product, com.pbl3.project.pbl3_project.entity.Category contextCategory, Runnable onSave) {
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

            grid.add(createFormLabel("Import Price"), 0, 2); grid.add(importPriceField, 1, 2);
            grid.add(createFormLabel("Selling Price *"), 2, 2); grid.add(priceField, 3, 2);
            grid.add(createFormLabel("Quantity *"), 0, 3); grid.add(qtyField, 1, 3);

            // Row 3: Master Data (Brand, Supplier)
            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Brand> brandCombo = new javafx.scene.control.ComboBox<>();
            brandCombo.setMaxWidth(Double.MAX_VALUE); brandCombo.setPromptText("Select Brand");
            brandCombo.setItems(javafx.collections.FXCollections.observableArrayList(brandService.getAllBrands()));
            setComboConverter(brandCombo);
            if (product != null) brandCombo.setValue(product.getBrand());

            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Supplier> supplierCombo = new javafx.scene.control.ComboBox<>();
            supplierCombo.setMaxWidth(Double.MAX_VALUE); supplierCombo.setPromptText("Select Supplier");
            supplierCombo.setItems(javafx.collections.FXCollections.observableArrayList(supplierService.getAllSuppliers()));
            setComboConverter(supplierCombo);
            if (product != null) supplierCombo.setValue(product.getSupplier());
            
            grid.add(createFormLabel("Brand"), 2, 3); grid.add(brandCombo, 3, 3);
            grid.add(createFormLabel("Supplier"), 0, 4); grid.add(supplierCombo, 1, 4);

            // Row 4: Master Data (Origin, Unit)
            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Origin> originCombo = new javafx.scene.control.ComboBox<>();
            originCombo.setMaxWidth(Double.MAX_VALUE); originCombo.setPromptText("Select Origin");
            originCombo.setItems(javafx.collections.FXCollections.observableArrayList(originService.getAllOrigins()));
            setComboConverter(originCombo);
            if (product != null) originCombo.setValue(product.getOrigin());

            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Unit> unitCombo = new javafx.scene.control.ComboBox<>();
            unitCombo.setMaxWidth(Double.MAX_VALUE); unitCombo.setPromptText("Select Unit");
            unitCombo.setItems(javafx.collections.FXCollections.observableArrayList(unitService.getAllUnits()));
            setComboConverter(unitCombo);
            if (product != null) unitCombo.setValue(product.getUnit());

            grid.add(createFormLabel("Origin"), 2, 4); grid.add(originCombo, 3, 4);
            grid.add(createFormLabel("Unit"), 0, 5); grid.add(unitCombo, 1, 5);
            
            // Description (Spans 2 columns)
            TextField descField = createStyledTextField(product != null ? product.getDescription() : "", "Description");
            grid.add(createFormLabel("Description"), 2, 5); grid.add(descField, 3, 5);

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

                    p.setName(nameField.getText());
                    p.setSku(skuField.getText());
                    p.setBarcode(barcodeField.getText());
                    p.setDescription(descField.getText());
                    p.setPrice(Double.parseDouble(priceField.getText()));
                    p.setQuantity(Integer.parseInt(qtyField.getText()));
                    
                    String importPriceTxt = importPriceField.getText();
                    if (!importPriceTxt.isEmpty()) p.setImportPrice(Double.parseDouble(importPriceTxt));
                    
                    // Implicit Category
                    if (product == null && contextCategory != null) {
                        p.setCategory(contextCategory);
                    } else if (product != null && p.getCategory() == null && contextCategory != null) {
                        p.setCategory(contextCategory);
                    }
                    
                    p.setBrand(brandCombo.getValue());
                    p.setSupplier(supplierCombo.getValue());
                    p.setOrigin(originCombo.getValue());
                    p.setUnit(unitCombo.getValue());

                    productService.saveProduct(p);
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
                         if (isSimpleClick && table.getSelectionModel().isSelected(index)) {
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
        root.setOnMousePressed(event -> {
            boolean isSafe = false;
            javafx.scene.Node curr = (javafx.scene.Node) event.getTarget();
            // Traverse up to check if we clicked inside table or an interactive control
            while (curr != null && curr != root) {
                if (curr == table || 
                    curr instanceof javafx.scene.control.Button || 
                    curr instanceof javafx.scene.control.TextField || 
                    curr instanceof javafx.scene.control.MenuBar) { // Removed MenuItem check
                    isSafe = true; 
                    break; 
                }
                curr = curr.getParent();
            }
            if (!isSafe) {
                table.getSelectionModel().clearSelection();
            }
        });
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
}

