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

@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final com.pbl3.project.pbl3_project.service.AuthService authService;
    private final com.pbl3.project.pbl3_project.service.ProductService productService;
    private final com.pbl3.project.pbl3_project.repository.CategoryRepository categoryRepository;
    private final com.pbl3.project.pbl3_project.service.OrderService orderService;
    private final com.pbl3.project.pbl3_project.service.ReportService reportService;

    public StageInitializer(com.pbl3.project.pbl3_project.service.AuthService authService,
                            com.pbl3.project.pbl3_project.service.ProductService productService,
                            com.pbl3.project.pbl3_project.repository.CategoryRepository categoryRepository,
                            com.pbl3.project.pbl3_project.service.OrderService orderService,
                            com.pbl3.project.pbl3_project.service.ReportService reportService) {
        this.authService = authService;
        this.productService = productService;
        this.categoryRepository = categoryRepository;
        this.orderService = orderService;
        this.reportService = reportService;
    }
    


    private void showDashboardScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        // ... (Table construction logic remains similar but compacted for brevity if needed, or kept same) ...
        // To avoid massive diffs, I will keep the content creation logic here but change the FINAL STEP to call switchScene
        
        // Product Table Wrapper
        VBox content = createProductView(stage, user);
        switchScene(stage, user, "Products", "nav-products", content);
    }

    private void showOrderHistoryScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox content = createOrderHistoryView(stage, user);
        switchScene(stage, user, "Order History", "nav-history", content);
    }
    
    private void showSalesScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.control.SplitPane content = createSalesView(stage, user);
        switchScene(stage, user, "Sales (POS)", "nav-sales", content);
    }
    
    private void showStatisticsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox content = createStatisticsView(stage, user);
        switchScene(stage, user, "Statistics", "nav-stats", content);
    }

    // --- View Creators (Extracted to keep code clean) ---

    private VBox createProductView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Product> table = new javafx.scene.control.TableView<>();
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Long> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> nameCol = new javafx.scene.control.TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Double> priceCol = new javafx.scene.control.TableColumn<>("Price");
        priceCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Integer> qtyCol = new javafx.scene.control.TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> catCol = new javafx.scene.control.TableColumn<>("Category");
        catCol.setCellValueFactory(cellData -> {
            var cat = cellData.getValue().getCategory();
            return new javafx.beans.property.SimpleStringProperty(cat != null ? cat.getName() : "N/A");
        });

        table.getColumns().addAll(idCol, nameCol, priceCol, qtyCol, catCol);
        table.setItems(javafx.collections.FXCollections.observableArrayList(productService.getAllProducts()));

        Button addButton = new Button("Add Product");
        addButton.getStyleClass().addAll("button", "success-button");
        addButton.setOnAction(e -> showProductDialog(stage, null, () -> {
            // Refresh table data without scene transition
            table.setItems(javafx.collections.FXCollections.observableArrayList(productService.getAllProducts()));
        }));

        Button editButton = new Button("Edit Selected");
        editButton.getStyleClass().addAll("button", "primary-button");
        editButton.setOnAction(e -> {
            var selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showProductDialog(stage, selected, () -> {
                // Refresh table data without scene transition
                table.setItems(javafx.collections.FXCollections.observableArrayList(productService.getAllProducts()));
            });
            else showAlert(javafx.scene.control.Alert.AlertType.WARNING, "No Selection", "Please select a product to edit.");
        });
        
        Button deleteButton = new Button("Delete Selected");
        deleteButton.getStyleClass().addAll("button", "danger-button");
        deleteButton.setOnAction(e -> {
            var selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    productService.deleteProduct(selected.getId());
                    // Refresh table data without scene transition
                    table.getItems().remove(selected);
                } catch (Exception ex) {
                    showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not delete: " + ex.getMessage());
                }
            } else showAlert(javafx.scene.control.Alert.AlertType.WARNING, "No Selection", "Please select a product to delete.");
        });

        javafx.scene.layout.HBox toolbar = new javafx.scene.layout.HBox(10, addButton, editButton, deleteButton);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(toolbar, table);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        return content;
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
    
    private javafx.scene.control.SplitPane createSalesView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.control.SplitPane splitPane = new javafx.scene.control.SplitPane();
        splitPane.setDividerPositions(0.6);

        // LEFT: Product List
        VBox leftBox = new VBox(10);
        leftBox.setPadding(new Insets(10));
        Label leftTitle = new Label("Available Products");
        leftTitle.getStyleClass().add("header-label");
        
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Product> productTable = new javafx.scene.control.TableView<>();
        productTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> pNameCol = new javafx.scene.control.TableColumn<>("Product");
        pNameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Double> pPriceCol = new javafx.scene.control.TableColumn<>("Price");
        pPriceCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Integer> pQtyCol = new javafx.scene.control.TableColumn<>("Stock");
        pQtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
        
        productTable.getColumns().addAll(pNameCol, pPriceCol, pQtyCol);
        productTable.setItems(javafx.collections.FXCollections.observableArrayList(productService.getAllProducts()));
        leftBox.getChildren().addAll(leftTitle, productTable);
        VBox.setVgrow(productTable, javafx.scene.layout.Priority.ALWAYS);

        // RIGHT: Cart
        VBox rightBox = new VBox(10);
        rightBox.setPadding(new Insets(10));
        rightBox.setStyle("-fx-background-color: #ECEFF1;");
        Label rightTitle = new Label("Shopping Cart");
        rightTitle.getStyleClass().add("header-label");

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest> cartTable = new javafx.scene.control.TableView<>();
        cartTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        // ... (Simplified Cart Columns for brevity) ...
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest, Long> cIdCol = new javafx.scene.control.TableColumn<>("ID");
        cIdCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("productId"));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest, Integer> cQtyCol = new javafx.scene.control.TableColumn<>("Qty");
        cQtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
        cartTable.getColumns().addAll(cIdCol, cQtyCol);

        javafx.collections.ObservableList<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest> cartItems = javafx.collections.FXCollections.observableArrayList();
        cartTable.setItems(cartItems);

        // Add to Cart Interaction
        productTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                var selected = productTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    if (selected.getQuantity() > 0) {
                        var existing = cartItems.stream().filter(i -> i.getProductId().equals(selected.getId())).findFirst();
                        if (existing.isPresent()) {
                            existing.get().setQuantity(existing.get().getQuantity() + 1);
                            cartTable.refresh();
                        } else {
                            var item = new com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest();
                            item.setProductId(selected.getId());
                            item.setQuantity(1);
                            cartItems.add(item);
                        }
                    } else showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Out of Stock", "Product is out of stock!");
                }
            }
        });

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
                showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Success", "Order created!");
                cartItems.clear();
                productTable.setItems(javafx.collections.FXCollections.observableArrayList(productService.getAllProducts()));
            } catch (Exception ex) {
                showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Order Failed", ex.getMessage());
            }
        });

        rightBox.getChildren().addAll(rightTitle, cartTable, checkoutButton);
        VBox.setVgrow(cartTable, javafx.scene.layout.Priority.ALWAYS);
        splitPane.getItems().addAll(leftBox, rightBox);
        return splitPane;
    }
    
    private VBox createStatisticsView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);
        
        Label title = new Label("Business Statistics");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #333;");

        var daily = reportService.getDailyStats();
        var monthly = reportService.getMonthlyStats();

        javafx.scene.layout.HBox cards = new javafx.scene.layout.HBox(20);
        cards.setAlignment(Pos.CENTER);
        cards.getChildren().add(createStatCard("Today's Revenue", daily.get("revenue") + " VND", "#4CAF50"));
        cards.getChildren().add(createStatCard("Today's Orders", daily.get("orders") + "", "#2196F3"));
        cards.getChildren().add(createStatCard("Monthly Revenue", monthly.get("revenue") + " VND", "#FF9800"));

        root.getChildren().addAll(title, cards);
        return root;
    }

    // --- Core Navigation & Animation ---
    
    private void switchScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user, String title, String navId, javafx.scene.Node content) {
        javafx.scene.Scene scene = stage.getScene();
        
        // Reuse Layout if possible
        if (scene != null && scene.getRoot() instanceof javafx.scene.layout.BorderPane && "MAIN_LAYOUT".equals(scene.getRoot().getUserData())) {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) scene.getRoot();
            
            // 1. Update Center Content with Float animation
            root.setCenter(content);
            javafx.scene.layout.BorderPane.setMargin(content, new Insets(15)); // Restore margin
            
            // Float up animation (from 30px below to 0)
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(200), content);
            tt.setFromY(30);
            tt.setToY(0);
            tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
            
            // Combined with subtle fade
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), content);
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
            stage.centerOnScreen();
            
            // Float up animation for initial load
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(300), layout);
            tt.setFromY(50);
            tt.setToY(0);
            tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
            
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), layout);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            
            javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(tt, ft);
            pt.play();
        }
    }

    private void updateSidebarState(javafx.scene.Parent root, String activeNavId) {
        // Find all nav buttons and update class
        for (String id : new String[]{"nav-products", "nav-sales", "nav-history", "nav-stats"}) {
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
        
        Label appTitle = new Label("SALES MGR");
        appTitle.setStyle("-fx-text-fill: #37474F; -fx-font-weight: bold; -fx-font-size: 20px; -fx-padding: 0 0 20 15;"); // Added left padding (15) to align with buttons
        
        Button navProducts = createNavButton("Products", "nav-products", () -> showDashboardScene(stage, user));
        Button navSales = createNavButton("Sales (POS)", "nav-sales", () -> showSalesScene(stage, user));
        Button navHistory = createNavButton("Order History", "nav-history", () -> showOrderHistoryScene(stage, user));
        Button navStats = createNavButton("Statistics", "nav-stats", () -> showStatisticsScene(stage, user));
        Button navLogout = createNavButton("Logout", "nav-logout", () -> showLoginScene(stage));
        navLogout.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");

        // Initial Active State
        if ("nav-products".equals(activeNavId)) navProducts.getStyleClass().add("active");
        if ("nav-sales".equals(activeNavId)) navSales.getStyleClass().add("active");
        if ("nav-history".equals(activeNavId)) navHistory.getStyleClass().add("active");
        if ("nav-stats".equals(activeNavId)) navStats.getStyleClass().add("active");

        sidebar.getChildren().addAll(appTitle, navProducts, navSales, navHistory, navStats, new javafx.scene.control.Separator(), navLogout);
        
        // Header
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
        header.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 0, 5, 0, 0);");
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Toggle Sidebar Button
        Button toggleSidebar = new Button("☰");
        toggleSidebar.setStyle("-fx-background-color: transparent; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 5 10;");
        
        toggleSidebar.setOnAction(e -> {
            boolean isVisible = sidebar.isVisible() && sidebar.isManaged();
            sidebar.setVisible(!isVisible);
            sidebar.setManaged(!isVisible);
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
        javafx.scene.layout.BorderPane.setMargin(centerContent, new Insets(15));
        
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

    private VBox createStatCard(String title, String value, String colorHex) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefSize(200, 150);
        card.setAlignment(Pos.CENTER);
        
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #757575;");
        
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        
        card.getChildren().addAll(titleLbl, valueLbl);
        return card;
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
                showDashboardScene(stage, user);
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


    private void showProductDialog(Stage owner, com.pbl3.project.pbl3_project.entity.Product product, Runnable onSave) {
        try {
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.setTitle(product == null ? "Add New Product" : "Edit Product");
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);

            VBox root = new VBox(10);
            root.getStyleClass().add("dialog-root");

            Label titleLabel = new Label(product == null ? "Create New Product" : "Edit Product Details");
            titleLabel.getStyleClass().add("dialog-title");
            titleLabel.setMaxWidth(Double.MAX_VALUE);

            // Form Fields Helper
            VBox form = new VBox(10);
            
            // ID Field (editable)
            TextField idField = createStyledTextField(product != null ? String.valueOf(product.getId()) : "", "Auto-generate if empty");
            
            TextField nameField = createStyledTextField(product != null ? product.getName() : "", "Product Name");
            TextField descField = createStyledTextField(product != null ? product.getDescription() : "", "Description or subtitle");
            TextField priceField = createStyledTextField(product != null ? String.valueOf(product.getPrice()) : "", "0.00");
            TextField qtyField = createStyledTextField(product != null ? String.valueOf(product.getQuantity()) : "", "0");

            // Category ComboBox
            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Category> catCombo = new javafx.scene.control.ComboBox<>();
            catCombo.setMaxWidth(Double.MAX_VALUE);
            catCombo.getStyleClass().add("combo-box");
            try {
                catCombo.setItems(javafx.collections.FXCollections.observableArrayList(categoryRepository.findAll()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            catCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(com.pbl3.project.pbl3_project.entity.Category object) {
                    return object != null ? object.getName() : "";
                }
                @Override
                public com.pbl3.project.pbl3_project.entity.Category fromString(String string) {
                    return null; 
                }
            });
            if (product != null) catCombo.setValue(product.getCategory());
            else if (!catCombo.getItems().isEmpty()) catCombo.getSelectionModel().selectFirst();


            Button saveButton = new Button("SAVE PRODUCT");
            saveButton.getStyleClass().addAll("button", "primary-button");
            saveButton.setMaxWidth(Double.MAX_VALUE);
            saveButton.setStyle("-fx-font-size: 14px; -fx-padding: 12;"); 
            
            saveButton.setOnAction(e -> {
                try {
                    com.pbl3.project.pbl3_project.entity.Product p = product != null ? product : new com.pbl3.project.pbl3_project.entity.Product();
                    
                    // Set ID if provided
                    String idText = idField.getText().trim();
                    if (!idText.isEmpty()) {
                        p.setId(Long.parseLong(idText));
                    }
                    
                    p.setName(nameField.getText());
                    p.setDescription(descField.getText());
                    p.setPrice(Double.parseDouble(priceField.getText()));
                    p.setQuantity(Integer.parseInt(qtyField.getText()));
                    p.setCategory(catCombo.getValue());

                    productService.saveProduct(p);
                    onSave.run();
                    dialog.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Save Error", "Could not save product: " + ex.getMessage());
                }
            });

            form.getChildren().addAll(
                createFormLabel("Product ID"), idField,
                createFormLabel("Product Name"), nameField,
                createFormLabel("Description"), descField,
                createFormLabel("Price (VND)"), priceField,
                createFormLabel("Stock Quantity"), qtyField,
                createFormLabel("Category"), catCombo
            );

            root.getChildren().addAll(titleLabel, form, new javafx.scene.layout.Region(), saveButton);
            VBox.setVgrow(form, javafx.scene.layout.Priority.ALWAYS);
            
            Scene scene = new Scene(root, 400, 600);
            scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "System Error", "Could not open dialog: " + e.getMessage());
        }
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

    private void showAlert(javafx.scene.control.Alert.AlertType type, String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

