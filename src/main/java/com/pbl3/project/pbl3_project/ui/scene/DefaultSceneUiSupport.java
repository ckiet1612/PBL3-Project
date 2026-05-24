package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.Expense;
import com.pbl3.project.pbl3_project.entity.ExpenseCategory;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.Promotion;
import com.pbl3.project.pbl3_project.entity.PromotionDiscountType;
import com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus;
import com.pbl3.project.pbl3_project.entity.PromotionScope;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.ExpenseService;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.OrderService;
import com.pbl3.project.pbl3_project.service.ProductService;
import com.pbl3.project.pbl3_project.service.PromotionService;
import com.pbl3.project.pbl3_project.service.ReceiptService;
import com.pbl3.project.pbl3_project.service.ToastService;
import com.pbl3.project.pbl3_project.ui.dialog.DialogCoordinator;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.SortCriterion;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.control.skin.NestedTableColumnHeader;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class DefaultSceneUiSupport implements SceneUiSupport {

    private static final Color PRIMARY_COLOR = Color.web("#1d7df2");
    private static final Color TEXT_MUTED_COLOR = Color.web("#78909C");
    private static final String SORT_HANDLER_ATTACHED_KEY = "manualSortHandlerAttached";
    private static final String SORT_HEADER_BASE_TEXT_KEY = "sortHeaderBaseText";
    private static final String SORT_HEADER_LABEL_KEY = "sortHeaderLabel";
    private static final String SORT_HEADER_TRIANGLE_KEY = "sortHeaderTriangle";
    private static final double STANDARD_TABLE_PAGE_SPACING = 12;
    private static final Insets STANDARD_TABLE_PAGE_PADDING = new Insets(20);
    private static final Insets STANDARD_TABLE_STATUS_PADDING = new Insets(8, 0, 0, 0);
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter PROMOTION_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final Interpolator SLIDING_MENU_INTERPOLATOR = Interpolator.SPLINE(0.22, 0.82, 0.2, 1.0);
    private static final String SLIDING_MENU_FONT_FAMILY = "Be Vietnam Pro";
    private static final double SLIDING_MENU_FONT_SIZE = 15;
    private static final double SLIDING_MENU_HORIZONTAL_PADDING = 24;
    private static final double SLIDING_MENU_WIDTH_BUFFER = 8;

    private final Map<String, TableSortState> sessionSortStates;
    private final ExpenseService expenseService;
    private final PromotionService promotionService;
    private final ProductService productService;
    private final OrderService orderService;
    private final ReceiptService receiptService;
    private final ToastService toastService;
    private final Consumer<Throwable> errorHandler;
    private final DialogCoordinator dialogCoordinator;

    public DefaultSceneUiSupport(
        Map<String, TableSortState> sessionSortStates,
        ExpenseService expenseService,
        PromotionService promotionService,
        ProductService productService,
        OrderService orderService,
        ReceiptService receiptService,
        ToastService toastService,
        Consumer<Throwable> errorHandler
    ) {
        this.sessionSortStates = sessionSortStates;
        this.expenseService = expenseService;
        this.promotionService = promotionService;
        this.productService = productService;
        this.orderService = orderService;
        this.receiptService = receiptService;
        this.toastService = toastService;
        this.errorHandler = errorHandler;
        this.dialogCoordinator = new DialogCoordinator(
            expenseService,
            promotionService,
            productService,
            orderService,
            receiptService,
            toastService,
            errorHandler,
            this::customizeDatePicker
        );
    }

    @Override
    public TableSortState getOrCreateTableSortState(String stateKey, SortCriterion... defaultCriteria) {
        return sessionSortStates.computeIfAbsent(
            stateKey,
            key -> new TableSortState(Arrays.asList(defaultCriteria))
        );
    }

    @Override
    public void applyStandardTableSizing(TableView<?> table) {
        table.setMinHeight(0);
        table.setMaxHeight(Double.MAX_VALUE);
        table.setPrefWidth(Double.MAX_VALUE);
        table.setMaxWidth(Double.MAX_VALUE);
    }

    @Override
    public void applyStandardTablePageLayout(VBox root) {
        applyStandardTablePageLayout(root, STANDARD_TABLE_PAGE_PADDING);
    }

    @Override
    public void applyStandardTablePageLayout(VBox root, Insets padding) {
        root.setSpacing(STANDARD_TABLE_PAGE_SPACING);
        root.setPadding(padding);
        root.setFillWidth(true);
    }

    @Override
    public void applyStandardTableStatusBar(HBox statusBar) {
        statusBar.setAlignment(Pos.CENTER_RIGHT);
        statusBar.setPadding(STANDARD_TABLE_STATUS_PADDING);
        statusBar.setMaxWidth(Double.MAX_VALUE);
    }

    @Override
    public Label createStatusMetaLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
        return label;
    }

    @Override
    public void updatePagedStatus(
        TableView<?> table,
        Label rowCountLabel,
        Label pageLabel,
        Button prevButton,
        Button nextButton,
        long totalElements,
        int currentPage,
        int totalPages,
        int pageSize
    ) {
        long safeTotal = Math.max(0, totalElements);
        int safePage = Math.max(0, currentPage);
        int safeTotalPages = Math.max(totalPages, safeTotal == 0 ? 0 : 1);
        long startRow = safeTotal == 0 ? 0 : (long) safePage * pageSize + 1;
        long endRow = safeTotal == 0 ? 0 : Math.min(safeTotal, (long) (safePage + 1) * pageSize);
        int selected = table.getSelectionModel().getSelectedItems() != null
            ? table.getSelectionModel().getSelectedItems().size()
            : 0;

        String baseText = "Showing " + startRow + "-" + endRow + " of " + safeTotal + " Row(s)";
        rowCountLabel.setText(selected > 0 ? baseText + " (Selected: " + selected + ")" : baseText);
        pageLabel.setText(safeTotalPages <= 0 ? "Page 0 / 0" : "Page " + (safePage + 1) + " / " + safeTotalPages);
        prevButton.setDisable(safePage <= 0 || safeTotalPages <= 1);
        nextButton.setDisable(safeTotalPages <= 1 || safePage >= safeTotalPages - 1);
    }

    @Override
    public Pageable createPageable(TableSortState sortState, Map<String, String> propertyByUiKey, int page, int size) {
        if (sortState == null || propertyByUiKey == null || propertyByUiKey.isEmpty()) {
            return PageRequest.of(page, size);
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (SortCriterion criterion : sortState.snapshot()) {
            String property = propertyByUiKey.get(criterion.uiKey());
            if (property == null || property.isBlank()) {
                continue;
            }
            Sort.Direction direction = criterion.direction() == TableColumn.SortType.ASCENDING
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
            orders.add(new Sort.Order(direction, property));
        }
        return orders.isEmpty()
            ? PageRequest.of(page, size)
            : PageRequest.of(page, size, Sort.by(orders));
    }

    @Override
    public double getTableVerticalScrollValue(TableView<?> table) {
        ScrollBar scrollBar = findTableVerticalScrollBar(table);
        return scrollBar != null ? scrollBar.getValue() : Double.NaN;
    }

    @Override
    public <T> void restoreTableSelectionById(TableView<T> table, Long id, Function<T, Long> idExtractor) {
        if (table == null || id == null || idExtractor == null || table.getItems() == null) {
            return;
        }
        for (int i = 0; i < table.getItems().size(); i++) {
            T item = table.getItems().get(i);
            if (item != null && Objects.equals(idExtractor.apply(item), id)) {
                final int rowIndex = i;
                table.getSelectionModel().select(rowIndex);
                table.getFocusModel().focus(rowIndex);
                Platform.runLater(() -> {
                    table.getSelectionModel().select(rowIndex);
                    table.getFocusModel().focus(rowIndex);
                    table.requestFocus();
                });
                return;
            }
        }
    }

    @Override
    public void restoreTableVerticalScrollValue(TableView<?> table, double value) {
        if (table == null || Double.isNaN(value)) {
            return;
        }
        Platform.runLater(() -> {
            ScrollBar scrollBar = findTableVerticalScrollBar(table);
            if (scrollBar != null) {
                scrollBar.setValue(Math.max(scrollBar.getMin(), Math.min(scrollBar.getMax(), value)));
            }
            Platform.runLater(() -> {
                ScrollBar secondPassScrollBar = findTableVerticalScrollBar(table);
                if (secondPassScrollBar != null) {
                    secondPassScrollBar.setValue(Math.max(secondPassScrollBar.getMin(), Math.min(secondPassScrollBar.getMax(), value)));
                }
            });
        });
    }

    @Override
    public <T> void installSortHeaderIndicators(LinkedHashMap<String, TableColumn<T, ?>> columnsByKey) {
        if (columnsByKey == null || columnsByKey.isEmpty()) {
            return;
        }
        for (TableColumn<T, ?> column : columnsByKey.values()) {
            if (column == null) {
                continue;
            }
            String baseText = column.getProperties().get(SORT_HEADER_BASE_TEXT_KEY) instanceof String storedText
                ? storedText
                : (column.getText() == null ? "" : column.getText());
            column.getProperties().put(SORT_HEADER_BASE_TEXT_KEY, baseText);
            if (column.getProperties().get(SORT_HEADER_TRIANGLE_KEY) instanceof Polygon) {
                continue;
            }

            Label headerLabel = new Label(baseText);
            headerLabel.setAlignment(Pos.CENTER);
            headerLabel.setMaxWidth(Double.MAX_VALUE);
            headerLabel.setMouseTransparent(true);
            headerLabel.setTextAlignment(TextAlignment.CENTER);

            Polygon triangle = new Polygon(0.0, 6.0, 5.0, 0.0, 10.0, 6.0);
            triangle.setFill(TEXT_MUTED_COLOR);
            triangle.setOpacity(0.0);
            triangle.setMouseTransparent(true);

            Region leftSpacer = fixedRegion(12);
            StackPane arrowBox = new StackPane(triangle);
            arrowBox.setMinWidth(12);
            arrowBox.setPrefWidth(12);
            arrowBox.setMaxWidth(12);
            arrowBox.setAlignment(Pos.CENTER_RIGHT);
            arrowBox.setMouseTransparent(true);

            BorderPane headerGraphic = new BorderPane();
            headerGraphic.setMaxWidth(Double.MAX_VALUE);
            headerGraphic.setMouseTransparent(true);
            headerGraphic.setLeft(leftSpacer);
            headerGraphic.setCenter(headerLabel);
            headerGraphic.setRight(arrowBox);
            headerGraphic.prefWidthProperty().bind(
                Bindings.createDoubleBinding(() -> Math.max(0.0, column.getWidth()), column.widthProperty())
            );
            BorderPane.setAlignment(headerLabel, Pos.CENTER);
            BorderPane.setAlignment(arrowBox, Pos.CENTER_RIGHT);

            column.setText(null);
            column.setGraphic(headerGraphic);
            column.getProperties().put(SORT_HEADER_LABEL_KEY, headerLabel);
            column.getProperties().put(SORT_HEADER_TRIANGLE_KEY, triangle);
        }
    }

    @Override
    public <T> void applySortStateToTable(TableView<T> table, LinkedHashMap<String, TableColumn<T, ?>> columnsByKey, TableSortState sortState) {
        if (table == null || columnsByKey == null || sortState == null) {
            return;
        }
        table.getSortOrder().clear();
        updateSortHeaderIndicators(columnsByKey, sortState);
    }

    @Override
    public String buildSortStatusText(TableSortState sortState, Map<String, String> labelsByKey) {
        if (sortState == null || sortState.isEmpty() || labelsByKey == null || labelsByKey.isEmpty()) {
            return "";
        }
        List<SortCriterion> criteria = sortState.snapshot();
        StringBuilder builder = new StringBuilder();
        int visibleCriteria = Math.min(criteria.size(), 2);
        for (int i = 0; i < visibleCriteria; i++) {
            SortCriterion criterion = criteria.get(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(labelsByKey.getOrDefault(criterion.uiKey(), criterion.uiKey()));
            builder.append(criterion.direction() == TableColumn.SortType.ASCENDING ? " ↑" : " ↓");
        }
        if (criteria.size() > visibleCriteria) {
            builder.append(", +").append(criteria.size() - visibleCriteria);
        }
        return builder.toString();
    }

    @Override
    public Label createSortStatusLabel(TableSortState sortState, Map<String, String> labelsByKey) {
        Label label = new Label(buildSortStatusText(sortState, labelsByKey));
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px; -fx-padding: 0 10 0 0;");
        return label;
    }

    @Override
    public <T> void installManualServerSorting(
        TableView<T> table,
        LinkedHashMap<String, TableColumn<T, ?>> columnsByKey,
        TableSortState sortState,
        Runnable onSortChanged
    ) {
        if (table == null || columnsByKey == null || columnsByKey.isEmpty() || sortState == null || onSortChanged == null) {
            return;
        }
        table.setSortPolicy(tv -> true);
        table.getColumns().forEach(column -> column.setSortable(false));

        Map<TableColumn<?, ?>, String> keyByColumn = new java.util.HashMap<>();
        for (Map.Entry<String, TableColumn<T, ?>> entry : columnsByKey.entrySet()) {
            keyByColumn.put(entry.getValue(), entry.getKey());
        }

        Runnable attachHandlers = () -> {
            table.applyCss();
            table.layout();
            Node headerNode = table.lookup(".column-header-background");
            if (headerNode instanceof TableHeaderRow headerRow) {
                attachSortHandlersRecursive(headerRow.getRootHeader(), keyByColumn, sortState, onSortChanged);
            }
        };
        table.skinProperty().addListener((obs, oldSkin, newSkin) -> Platform.runLater(attachHandlers));
        Platform.runLater(attachHandlers);
    }

    @Override
    public void customizeDatePicker(DatePicker datePicker) {
        datePicker.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (!isShowing) {
                return;
            }
            Platform.runLater(() -> customizeDatePickerPopup(datePicker));
        });
    }

    @Override
    public void showPopupBelow(Popup popup, Node owner, double xOffset, double yOffset) {
        if (popup == null || owner == null) {
            return;
        }
        Runnable showPopup = () -> {
            if (owner.getScene() == null || owner.getScene().getWindow() == null) {
                return;
            }
            Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
            if (bounds == null) {
                return;
            }
            double anchorX = bounds.getMinX() + xOffset;
            double anchorY = bounds.getMaxY() + yOffset;
            double popupWidth = resolvePopupWidth(popup);
            Screen screen = Screen.getScreensForRectangle(
                bounds.getMinX(),
                bounds.getMinY(),
                Math.max(bounds.getWidth(), 1),
                Math.max(bounds.getHeight(), 1)
            ).stream().findFirst().orElse(Screen.getPrimary());
            Rectangle2D visualBounds = screen.getVisualBounds();
            if (popupWidth > 0.0) {
                anchorX = Math.max(visualBounds.getMinX() + 8, Math.min(anchorX, visualBounds.getMaxX() - popupWidth - 8));
            }
            anchorY = Math.max(visualBounds.getMinY() + 8, anchorY);
            popup.show(owner, anchorX, anchorY);
        };
        if (Platform.isFxApplicationThread()) {
            showPopup.run();
        } else {
            Platform.runLater(showPopup);
        }
    }

    @Override
    public Node createSlidingMenu(String[] tabNames, Consumer<Integer> onSelect) {
        HBox container = new HBox();
        container.getStyleClass().add("sliding-menu-root");
        container.setMaxWidth(Region.USE_PREF_SIZE);
        container.setAlignment(Pos.CENTER_LEFT);

        StackPane wrapper = new StackPane();
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.getStyleClass().add("sliding-menu-wrapper");

        Region outerCapsule = new Region();
        outerCapsule.getStyleClass().add("sliding-menu-container");
        outerCapsule.setManaged(false);
        outerCapsule.setMouseTransparent(true);

        Region hoverIndicator = new Region();
        hoverIndicator.getStyleClass().add("hover-capsule");
        hoverIndicator.setManaged(false);
        hoverIndicator.setMouseTransparent(true);
        hoverIndicator.setOpacity(0);

        Region indicator = new Region();
        indicator.getStyleClass().add("active-capsule");
        indicator.setManaged(false);
        indicator.setMouseTransparent(true);

        HBox tabsBox = new HBox(0);
        tabsBox.setAlignment(Pos.CENTER_LEFT);
        tabsBox.getStyleClass().add("menu-items-layer");

        ToggleGroup toggleGroup = new ToggleGroup();
        List<ToggleButton> tabButtons = new ArrayList<>();
        DoubleProperty indicatorX = new SimpleDoubleProperty(0);
        DoubleProperty indicatorY = new SimpleDoubleProperty(0);
        DoubleProperty indicatorW = new SimpleDoubleProperty(0);
        DoubleProperty indicatorH = new SimpleDoubleProperty(0);
        DoubleProperty hoverX = new SimpleDoubleProperty(0);
        DoubleProperty hoverY = new SimpleDoubleProperty(0);
        DoubleProperty hoverW = new SimpleDoubleProperty(0);
        DoubleProperty hoverH = new SimpleDoubleProperty(0);
        ObjectProperty<ToggleButton> hoveredButtonRef = new SimpleObjectProperty<>();

        Runnable applyIndicatorBounds = () -> indicator.resizeRelocate(indicatorX.get(), indicatorY.get(), Math.max(0, indicatorW.get()), Math.max(0, indicatorH.get()));
        Runnable applyHoverBounds = () -> hoverIndicator.resizeRelocate(hoverX.get(), hoverY.get(), Math.max(0, hoverW.get()), Math.max(0, hoverH.get()));
        ChangeListener<Number> indicatorGeometryListener = (obs, oldVal, newVal) -> applyIndicatorBounds.run();
        indicatorX.addListener(indicatorGeometryListener);
        indicatorY.addListener(indicatorGeometryListener);
        indicatorW.addListener(indicatorGeometryListener);
        indicatorH.addListener(indicatorGeometryListener);
        ChangeListener<Number> hoverGeometryListener = (obs, oldVal, newVal) -> applyHoverBounds.run();
        hoverX.addListener(hoverGeometryListener);
        hoverY.addListener(hoverGeometryListener);
        hoverW.addListener(hoverGeometryListener);
        hoverH.addListener(hoverGeometryListener);

        Timeline[] indicatorTimelineRef = new Timeline[1];
        Timeline[] hoverTimelineRef = new Timeline[1];
        Consumer<Boolean> hideHoverIndicator = animate -> hideSlidingIndicator(hoverIndicator, hoverTimelineRef, animate);
        java.util.function.BiConsumer<ToggleButton, Boolean> moveHoverToButton = (button, animate) -> moveSlidingIndicator(
            wrapper, hoverIndicator, hoverTimelineRef, hoverX, hoverY, hoverW, hoverH, button, animate, 180, true
        );
        java.util.function.BiConsumer<ToggleButton, Boolean> moveIndicatorToButton = (button, animate) -> moveSlidingIndicator(
            wrapper, indicator, indicatorTimelineRef, indicatorX, indicatorY, indicatorW, indicatorH, button, animate, 240, false
        );

        Runnable syncOuterCapsule = () -> outerCapsule.resizeRelocate(0, 0, wrapper.getWidth(), wrapper.getHeight());
        Runnable syncSelectedIndicator = () -> {
            if (toggleGroup.getSelectedToggle() instanceof ToggleButton selectedButton) {
                moveIndicatorToButton.accept(selectedButton, false);
            }
        };
        Runnable syncHoverIndicator = () -> {
            ToggleButton hoveredButton = hoveredButtonRef.get();
            if (hoveredButton == null || !hoveredButton.isHover() || hoveredButton == toggleGroup.getSelectedToggle()) {
                hideHoverIndicator.accept(false);
                return;
            }
            moveHoverToButton.accept(hoveredButton, false);
        };

        for (int i = 0; i < tabNames.length; i++) {
            final int index = i;
            ToggleButton tabButton = new ToggleButton(tabNames[i]);
            tabButton.getStyleClass().setAll("tab-button-label");
            tabButton.setToggleGroup(toggleGroup);
            tabButton.setUserData(index);
            tabButton.setFocusTraversable(false);
            tabButton.setMnemonicParsing(false);
            tabButton.setTooltip(new Tooltip(tabNames[i]));
            double stableButtonWidth = computeSlidingMenuButtonWidth(tabNames[i]);
            tabButton.setMinWidth(stableButtonWidth);
            tabButton.setPrefWidth(stableButtonWidth);
            tabButton.setMaxWidth(stableButtonWidth);
            tabButton.setOnAction(e -> onSelect.accept(index));
            tabButton.hoverProperty().addListener((obs, wasHovering, isHovering) -> handleSlidingMenuHover(
                tabButton,
                tabButtons,
                toggleGroup,
                hoveredButtonRef,
                moveHoverToButton,
                hideHoverIndicator,
                isHovering
            ));
            tabButtons.add(tabButton);
            tabsBox.getChildren().add(tabButton);
        }

        toggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                if (oldToggle != null) {
                    toggleGroup.selectToggle(oldToggle);
                }
                return;
            }
            if (newToggle instanceof ToggleButton selectedButton) {
                moveIndicatorToButton.accept(selectedButton, oldToggle != null);
                syncHoverIndicator.run();
            }
        });
        if (!tabButtons.isEmpty()) {
            toggleGroup.selectToggle(tabButtons.get(0));
        }

        ChangeListener<Number> geometrySync = (obs, oldVal, newVal) -> {
            syncOuterCapsule.run();
            syncSelectedIndicator.run();
            syncHoverIndicator.run();
        };
        wrapper.widthProperty().addListener(geometrySync);
        wrapper.heightProperty().addListener(geometrySync);
        tabsBox.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> geometrySync.changed(null, null, null));
        for (ToggleButton tabButton : tabButtons) {
            tabButton.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
                syncSelectedIndicator.run();
                syncHoverIndicator.run();
            });
            tabButton.localToSceneTransformProperty().addListener((obs, oldVal, newVal) -> {
                syncSelectedIndicator.run();
                syncHoverIndicator.run();
            });
        }
        Platform.runLater(() -> {
            syncOuterCapsule.run();
            syncSelectedIndicator.run();
            syncHoverIndicator.run();
        });
        wrapper.setOnMouseExited(event -> {
            hoveredButtonRef.set(null);
            hideHoverIndicator.accept(true);
        });
        wrapper.getChildren().addAll(outerCapsule, hoverIndicator, indicator, tabsBox);
        container.getChildren().add(wrapper);
        return container;
    }

    @Override
    public boolean showConfirmDialog(String title, String content) {
        return DialogSupport.showConfirm(null, title, content);
    }

    @Override
    public void showExpenseDialog(Stage owner, User user, Expense expense, Runnable onSuccess) {
        dialogCoordinator.showExpense(owner, user, expense, onSuccess);
    }

    @Override
    public void showPromotionDialog(Stage owner, User user, Promotion promotion, Runnable onSuccess) {
        dialogCoordinator.showPromotion(owner, user, promotion, onSuccess);
    }

    @Override
    public String formatExpenseCategoryLabel(ExpenseCategory category) {
        return enumText(category);
    }

    @Override
    public String formatPaymentMethodLabel(PaymentMethod method) {
        if (method == PaymentMethod.QR) {
            return "QR / VietQR";
        }
        return enumText(method);
    }

    @Override
    public String formatUserDisplayName(User user) {
        if (user == null) {
            return "-";
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "-";
    }

    @Override
    public String formatDate(LocalDate date) {
        return date != null ? date.format(DISPLAY_DATE_FORMATTER) : "-";
    }

    @Override
    public String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DISPLAY_DATE_TIME_FORMATTER) : "-";
    }

    @Override
    public String formatOrderStatus(OrderStatus status) {
        return enumText(status != null ? status : OrderStatus.COMPLETED);
    }

    @Override
    public String formatVnd(BigDecimal amount) {
        return String.format("%,.0f VND", MoneySupport.normalize(amount).doubleValue());
    }

    @Override
    public void showOrderDetailsDialog(Stage owner, Order order, User user, Runnable onChanged) {
        dialogCoordinator.showOrderDetails(owner, order, user, onChanged);
    }

    @Override
    public String formatPromotionScopeLabel(PromotionScope scope) {
        return enumText(scope);
    }

    @Override
    public String formatPromotionDiscountTypeLabel(PromotionDiscountType discountType) {
        return enumText(discountType);
    }

    @Override
    public String formatPromotionLifecycleStatusLabel(PromotionLifecycleStatus status) {
        return enumText(status);
    }

    @Override
    public String formatPromotionTargetLabel(Promotion promotion) {
        if (promotion == null) {
            return "-";
        }
        if (promotion.getScope() == PromotionScope.PRODUCT) {
            return promotion.getTargetProduct() != null ? promotion.getTargetProduct().getName() : "Specific Product";
        }
        if (promotion.getMinOrderTotal() != null) {
            return "Min order " + formatVnd(promotion.getMinOrderTotal());
        }
        return "Any order";
    }

    @Override
    public String formatPromotionScheduleLabel(Promotion promotion) {
        if (promotion == null) {
            return "-";
        }
        String start = promotion.getStartsAt() == null ? "-" : formatPromotionDate(promotion.getStartsAt().toLocalDate());
        String end = promotion.getEndsAt() == null ? "-" : formatPromotionDate(promotion.getEndsAt().toLocalDate());
        if (promotion.getStartsAt() == null && promotion.getEndsAt() == null) {
            return "Always";
        }
        if (promotion.getStartsAt() == null) {
            return "Until " + end;
        }
        if (promotion.getEndsAt() == null) {
            return "From " + start;
        }
        return start + " - " + end;
    }

    @Override
    public String formatPromotionOwnerLabel(Promotion promotion) {
        if (promotion == null || promotion.getCreatedBy() == null || promotion.getCreatedBy().getRole() == null) {
            return promotion != null ? compactOwnerName(promotion.getCreatedByDisplayName()) : "-";
        }
        return formatCompactRoleLabel(promotion.getCreatedBy().getRole());
    }

    @Override
    public String formatPromotionDiscountLabel(Promotion promotion) {
        if (promotion == null) {
            return "-";
        }
        if (promotion.getDiscountType() == PromotionDiscountType.PERCENT) {
            return MoneySupport.normalize(promotion.getDiscountValue()).stripTrailingZeros().toPlainString() + "%";
        }
        return formatVnd(promotion.getDiscountValue());
    }

    private void updateSortHeaderIndicators(Map<String, ? extends TableColumn<?, ?>> columnsByKey, TableSortState sortState) {
        if (columnsByKey == null || columnsByKey.isEmpty() || sortState == null) {
            return;
        }
        for (TableColumn<?, ?> column : columnsByKey.values()) {
            if (column == null) {
                continue;
            }
            if (column.getProperties().get(SORT_HEADER_LABEL_KEY) instanceof Label headerLabel
                && column.getProperties().get(SORT_HEADER_BASE_TEXT_KEY) instanceof String baseText) {
                headerLabel.setText(baseText);
            }
            if (column.getProperties().get(SORT_HEADER_TRIANGLE_KEY) instanceof Polygon triangle) {
                triangle.setOpacity(0.0);
                triangle.setRotate(0.0);
                triangle.setFill(TEXT_MUTED_COLOR);
            }
        }

        List<SortCriterion> criteria = sortState.snapshot();
        for (int index = 0; index < criteria.size(); index++) {
            SortCriterion criterion = criteria.get(index);
            TableColumn<?, ?> column = columnsByKey.get(criterion.uiKey());
            if (column == null || !(column.getProperties().get(SORT_HEADER_TRIANGLE_KEY) instanceof Polygon triangle)) {
                continue;
            }
            triangle.setOpacity(index == 0 ? 1.0 : 0.8);
            triangle.setRotate(criterion.direction() == TableColumn.SortType.ASCENDING ? 0.0 : 180.0);
            triangle.setFill(index == 0 ? PRIMARY_COLOR : TEXT_MUTED_COLOR);
        }
    }

    private void advanceSortState(TableSortState sortState, String uiKey, boolean multiSort) {
        if (sortState == null || uiKey == null || uiKey.isBlank()) {
            return;
        }
        List<SortCriterion> current = sortState.snapshot();
        int existingIndex = -1;
        for (int i = 0; i < current.size(); i++) {
            if (uiKey.equals(current.get(i).uiKey())) {
                existingIndex = i;
                break;
            }
        }
        if (!multiSort) {
            if (existingIndex < 0) {
                sortState.replace(List.of(new SortCriterion(uiKey, TableColumn.SortType.ASCENDING)));
                return;
            }
            TableColumn.SortType direction = current.get(existingIndex).direction();
            if (direction == TableColumn.SortType.ASCENDING) {
                sortState.replace(List.of(new SortCriterion(uiKey, TableColumn.SortType.DESCENDING)));
            } else {
                sortState.clear();
            }
            return;
        }
        if (existingIndex < 0) {
            current.add(new SortCriterion(uiKey, TableColumn.SortType.ASCENDING));
            sortState.replace(current);
            return;
        }
        SortCriterion existing = current.get(existingIndex);
        if (existing.direction() == TableColumn.SortType.ASCENDING) {
            current.set(existingIndex, new SortCriterion(uiKey, TableColumn.SortType.DESCENDING));
        } else {
            current.remove(existingIndex);
        }
        sortState.replace(current);
    }

    private void attachSortHandlersRecursive(
        TableColumnHeader header,
        Map<TableColumn<?, ?>, String> keyByColumn,
        TableSortState sortState,
        Runnable onSortChanged
    ) {
        if (header == null) {
            return;
        }
        TableColumnBase<?, ?> columnBase = header.getTableColumn();
        if (columnBase instanceof TableColumn<?, ?> column) {
            String uiKey = keyByColumn.get(column);
            if (uiKey != null && !Boolean.TRUE.equals(header.getProperties().get(SORT_HANDLER_ATTACHED_KEY))) {
                header.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                    if (event.getButton() != MouseButton.PRIMARY || !event.isStillSincePress()) {
                        return;
                    }
                    event.consume();
                    advanceSortState(sortState, uiKey, event.isShiftDown());
                    onSortChanged.run();
                });
                header.getProperties().put(SORT_HANDLER_ATTACHED_KEY, Boolean.TRUE);
            }
        }
        if (header instanceof NestedTableColumnHeader nestedHeader) {
            for (TableColumnHeader childHeader : nestedHeader.getColumnHeaders()) {
                attachSortHandlersRecursive(childHeader, keyByColumn, sortState, onSortChanged);
            }
        }
    }

    private ScrollBar findTableVerticalScrollBar(TableView<?> table) {
        if (table == null) {
            return null;
        }
        table.applyCss();
        for (Node node : table.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar scrollBar && scrollBar.getOrientation() == Orientation.VERTICAL) {
                return scrollBar;
            }
        }
        return null;
    }

    private void customizeDatePickerPopup(DatePicker datePicker) {
        DatePickerSkin skin = (DatePickerSkin) datePicker.getSkin();
        if (skin == null) {
            return;
        }
        Node popup = skin.getPopupContent();
        if (popup == null) {
            return;
        }
        Set<Node> spinners = popup.lookupAll(".spinner");
        int index = 0;
        for (Node spinner : spinners) {
            if (!(spinner instanceof HBox hbox)) {
                continue;
            }
            Label label = null;
            for (Node child : hbox.getChildren()) {
                if (child instanceof Label candidate) {
                    label = candidate;
                    break;
                }
            }
            if (label == null) {
                continue;
            }
            boolean monthSelector = index == 0;
            index++;
            installDatePickerHeaderSelector(datePicker, label, monthSelector);
        }
    }

    private void installDatePickerHeaderSelector(DatePicker datePicker, Label label, boolean monthSelector) {
        if ("customized".equals(label.getUserData())) {
            return;
        }
        label.setUserData("customized");
        Polygon upArrow = new Polygon(0, 4, 3.5, 0, 7, 4);
        upArrow.setFill(TEXT_MUTED_COLOR);
        Polygon downArrow = new Polygon(0, 0, 3.5, 4, 7, 0);
        downArrow.setFill(TEXT_MUTED_COLOR);
        VBox arrowBox = new VBox(1, upArrow, downArrow);
        arrowBox.setAlignment(Pos.CENTER);
        label.setGraphic(arrowBox);
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.setCursor(Cursor.HAND);
        label.setOnMouseClicked(event -> showDateSelectorPopup(datePicker, label, monthSelector));
    }

    private void showDateSelectorPopup(DatePicker datePicker, Label label, boolean monthSelector) {
        Popup selectorPopup = new Popup();
        selectorPopup.setAutoHide(true);
        VBox listBox = new VBox(2);
        listBox.setPadding(new Insets(6));
        listBox.setStyle("-fx-background-color: -app-surface; -fx-background-radius: 8; -fx-border-color: -app-border; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, -app-shadow, 8, 0, 0, 3);");

        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: -app-surface; -fx-background-color: transparent; -fx-border-color: transparent;");
        scrollPane.setPrefViewportHeight(200);
        scrollPane.setPrefWidth(120);

        if (monthSelector) {
            fillMonthSelector(datePicker, selectorPopup, listBox);
        } else {
            fillYearSelector(datePicker, selectorPopup, listBox, scrollPane);
        }

        VBox popupContent = new VBox(scrollPane);
        popupContent.setStyle("-fx-background-color: -app-surface; -fx-background-radius: 8;");
        selectorPopup.getContent().add(popupContent);
        Bounds screenBounds = label.localToScreen(label.getBoundsInLocal());
        selectorPopup.show(label, screenBounds.getMinX(), screenBounds.getMaxY() + 2);
    }

    private void fillMonthSelector(DatePicker datePicker, Popup selectorPopup, VBox listBox) {
        String[] months = {
            "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
            "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        };
        LocalDate current = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
        for (int i = 0; i < months.length; i++) {
            int monthIndex = i + 1;
            Label item = createDateSelectorItem(months[i], current.getMonthValue() == monthIndex);
            item.setOnMouseEntered(e -> updateDateSelectorItemStyle(item, current.getMonthValue() == monthIndex, true));
            item.setOnMouseExited(e -> updateDateSelectorItemStyle(item, current.getMonthValue() == monthIndex, false));
            item.setOnMouseClicked(e -> {
                LocalDate value = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
                int maxDay = YearMonth.of(value.getYear(), monthIndex).lengthOfMonth();
                datePicker.setValue(LocalDate.of(value.getYear(), monthIndex, Math.min(value.getDayOfMonth(), maxDay)));
                selectorPopup.hide();
            });
            listBox.getChildren().add(item);
        }
    }

    private void fillYearSelector(DatePicker datePicker, Popup selectorPopup, VBox listBox, ScrollPane scrollPane) {
        int currentYear = datePicker.getValue() != null ? datePicker.getValue().getYear() : LocalDate.now().getYear();
        int startYear = currentYear - 50;
        int endYear = currentYear + 10;
        int selectedIndex = 0;
        List<Label> items = new ArrayList<>();
        for (int year = startYear; year <= endYear; year++) {
            int selectedYear = year;
            Label item = createDateSelectorItem(String.valueOf(selectedYear), selectedYear == currentYear);
            item.setOnMouseEntered(e -> updateDateSelectorItemStyle(item, selectedYear == currentYear, true));
            item.setOnMouseExited(e -> updateDateSelectorItemStyle(item, selectedYear == currentYear, false));
            item.setOnMouseClicked(e -> {
                LocalDate value = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
                int maxDay = YearMonth.of(selectedYear, value.getMonthValue()).lengthOfMonth();
                datePicker.setValue(LocalDate.of(selectedYear, value.getMonthValue(), Math.min(value.getDayOfMonth(), maxDay)));
                selectorPopup.hide();
            });
            if (selectedYear == currentYear) {
                selectedIndex = year - startYear;
            }
            items.add(item);
            listBox.getChildren().add(item);
        }
        int scrollIndex = selectedIndex;
        Platform.runLater(() -> {
            double total = items.size();
            if (total > 0) {
                scrollPane.setVvalue(Math.max(0, (scrollIndex - 3.0) / total));
            }
        });
    }

    private Label createDateSelectorItem(String text, boolean selected) {
        Label item = new Label(text);
        item.setPrefWidth(100);
        item.setPadding(new Insets(6, 10, 6, 10));
        item.setCursor(Cursor.HAND);
        updateDateSelectorItemStyle(item, selected, false);
        return item;
    }

    private void updateDateSelectorItemStyle(Label item, boolean selected, boolean hover) {
        if (selected) {
            item.setStyle("-fx-background-color: -app-primary; -fx-text-fill: -app-surface; -fx-background-radius: 6; -fx-font-size: 13px; -fx-font-weight: bold;");
        } else if (hover) {
            item.setStyle("-fx-background-color: -app-primary-soft; -fx-text-fill: -app-primary; -fx-background-radius: 6; -fx-font-size: 13px;");
        } else {
            item.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-primary; -fx-background-radius: 6; -fx-font-size: 13px;");
        }
    }

    private double resolvePopupWidth(Popup popup) {
        if (popup.getContent().isEmpty()) {
            return 0.0;
        }
        Node content = popup.getContent().get(0);
        if (content instanceof Region region) {
            double prefWidth = region.getPrefWidth();
            if (prefWidth > 0.0 && prefWidth != Region.USE_COMPUTED_SIZE) {
                return prefWidth;
            }
            double layoutWidth = region.getLayoutBounds().getWidth();
            return layoutWidth > 0.0 ? layoutWidth : Math.max(0.0, region.prefWidth(-1));
        }
        return content.getLayoutBounds().getWidth();
    }

    private Region fixedRegion(double width) {
        Region region = new Region();
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
        region.setMouseTransparent(true);
        return region;
    }

    private void moveSlidingIndicator(
        StackPane wrapper,
        Region target,
        Timeline[] timelineRef,
        DoubleProperty x,
        DoubleProperty y,
        DoubleProperty width,
        DoubleProperty height,
        ToggleButton button,
        boolean animate,
        double durationMillis,
        boolean fadeIn
    ) {
        if (button == null || wrapper.getScene() == null) {
            return;
        }
        Bounds targetBounds = wrapper.sceneToLocal(button.localToScene(button.getBoundsInLocal()));
        if (targetBounds.getWidth() <= 0 || targetBounds.getHeight() <= 0) {
            return;
        }
        if (timelineRef[0] != null) {
            timelineRef[0].stop();
        }
        if (!animate || width.get() <= 0 || height.get() <= 0) {
            x.set(targetBounds.getMinX());
            y.set(targetBounds.getMinY());
            width.set(targetBounds.getWidth());
            height.set(targetBounds.getHeight());
            if (fadeIn) {
                target.setOpacity(1);
            }
            return;
        }
        List<KeyValue> keyValues = new ArrayList<>(List.of(
            new KeyValue(x, targetBounds.getMinX(), SLIDING_MENU_INTERPOLATOR),
            new KeyValue(y, targetBounds.getMinY(), SLIDING_MENU_INTERPOLATOR),
            new KeyValue(width, targetBounds.getWidth(), SLIDING_MENU_INTERPOLATOR),
            new KeyValue(height, targetBounds.getHeight(), SLIDING_MENU_INTERPOLATOR)
        ));
        if (fadeIn) {
            keyValues.add(new KeyValue(target.opacityProperty(), 1.0, Interpolator.EASE_BOTH));
        }
        timelineRef[0] = new Timeline(new KeyFrame(Duration.millis(durationMillis), keyValues.toArray(KeyValue[]::new)));
        timelineRef[0].play();
    }

    private void hideSlidingIndicator(Region hoverIndicator, Timeline[] timelineRef, boolean animate) {
        if (timelineRef[0] != null) {
            timelineRef[0].stop();
        }
        if (!animate || hoverIndicator.getOpacity() <= 0) {
            hoverIndicator.setOpacity(0);
            return;
        }
        timelineRef[0] = new Timeline(
            new KeyFrame(Duration.millis(120), new KeyValue(hoverIndicator.opacityProperty(), 0.0, Interpolator.EASE_BOTH))
        );
        timelineRef[0].play();
    }

    private void handleSlidingMenuHover(
        ToggleButton tabButton,
        List<ToggleButton> tabButtons,
        ToggleGroup toggleGroup,
        ObjectProperty<ToggleButton> hoveredButtonRef,
        java.util.function.BiConsumer<ToggleButton, Boolean> moveHoverToButton,
        Consumer<Boolean> hideHoverIndicator,
        boolean hovering
    ) {
        if (hovering) {
            hoveredButtonRef.set(tabButton);
            if (tabButton == toggleGroup.getSelectedToggle()) {
                hideHoverIndicator.accept(true);
            } else {
                moveHoverToButton.accept(tabButton, true);
            }
            return;
        }
        Platform.runLater(() -> {
            ToggleButton nextHoveredButton = null;
            for (ToggleButton candidate : tabButtons) {
                if (candidate.isHover()) {
                    nextHoveredButton = candidate;
                    break;
                }
            }
            hoveredButtonRef.set(nextHoveredButton);
            if (nextHoveredButton == null || nextHoveredButton == toggleGroup.getSelectedToggle()) {
                hideHoverIndicator.accept(true);
            } else {
                moveHoverToButton.accept(nextHoveredButton, true);
            }
        });
    }

    private double computeSlidingMenuButtonWidth(String labelText) {
        Text mediumText = new Text(labelText);
        mediumText.setFont(Font.font(SLIDING_MENU_FONT_FAMILY, FontWeight.MEDIUM, SLIDING_MENU_FONT_SIZE));
        Text boldText = new Text(labelText);
        boldText.setFont(Font.font(SLIDING_MENU_FONT_FAMILY, FontWeight.BOLD, SLIDING_MENU_FONT_SIZE));
        return Math.ceil(
            Math.max(mediumText.getLayoutBounds().getWidth(), boldText.getLayoutBounds().getWidth())
                + (SLIDING_MENU_HORIZONTAL_PADDING * 2)
                + SLIDING_MENU_WIDTH_BUFFER
        );
    }

    private String enumText(Enum<?> value) {
        return value == null ? "-" : humanizeEnumToken(value.name());
    }

    private String humanizeEnumToken(String token) {
        if (token == null || token.isBlank()) {
            return "Unknown";
        }
        String normalized = token.toLowerCase().replace('_', ' ');
        StringBuilder result = new StringBuilder(normalized.length());
        boolean nextUpper = true;
        for (char c : normalized.toCharArray()) {
            if (Character.isWhitespace(c)) {
                result.append(c);
                nextUpper = true;
            } else if (nextUpper) {
                result.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private String formatPromotionDate(LocalDate date) {
        return date != null ? date.format(PROMOTION_DATE_FORMATTER) : "-";
    }

    private String compactOwnerName(String value) {
        if (value == null || value.isBlank()) {
            return "System";
        }
        String trimmed = value.trim();
        if ("System Administrator".equalsIgnoreCase(trimmed)) {
            return "Admin";
        }
        return trimmed;
    }

    private String formatCompactRoleLabel(Role role) {
        if (role == null) {
            return "-";
        }
        return switch (role) {
            case ADMIN -> "Admin";
            case MANAGER -> "Manager";
            case STAFF -> "Staff";
        };
    }
}
