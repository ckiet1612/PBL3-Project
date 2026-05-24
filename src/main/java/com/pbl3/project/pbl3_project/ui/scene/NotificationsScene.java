package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.NotificationActionTarget;
import com.pbl3.project.pbl3_project.entity.NotificationCategory;
import com.pbl3.project.pbl3_project.entity.NotificationSeverity;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.NotificationService;
import com.pbl3.project.pbl3_project.service.NotificationService.CreateTaskRequest;
import com.pbl3.project.pbl3_project.service.NotificationService.NotificationFilter;
import com.pbl3.project.pbl3_project.service.NotificationService.NotificationView;
import com.pbl3.project.pbl3_project.service.NotificationService.RecipientOption;
import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.component.DialogFormFactory;
import com.pbl3.project.pbl3_project.ui.scene.model.ProductViewPreset;
import com.pbl3.project.pbl3_project.ui.scene.model.ReportFocusTarget;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Window;
import javafx.util.StringConverter;

public final class NotificationsScene {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record Options() {
    }

    private record NotificationLoadResult(List<NotificationView> items, long unreadCount) {
    }

    private record NotificationCreateResult(NotificationView visibleItem) {
    }

    private NotificationsScene() {
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        VBox root = new VBox(16);
        root.getStyleClass().addAll("reports-page", "notifications-page");
        root.setPadding(new Insets(24));
        root.setFillWidth(true);

        AtomicReference<NotificationFilter> activeFilter = new AtomicReference<>(NotificationFilter.ALL);
        AtomicLong loadVersion = new AtomicLong();
        AtomicLong unreadCount = new AtomicLong();

        Label countLabel = new Label("Loading...");
        countLabel.getStyleClass().add("notifications-count-label");

        HBox filterRow = new HBox(8);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Button allButton = filterButton("All");
        Button unreadButton = filterButton("Unread");
        Button criticalButton = filterButton("Critical");
        Button taskButton = filterButton("Tasks");
        List<Button> filterButtons = List.of(allButton, unreadButton, criticalButton, taskButton);
        applyFilterButtonState(filterButtons, allButton);
        filterRow.getChildren().addAll(allButton, unreadButton, criticalButton, taskButton);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button refreshButton = toolbarIconButton("Refresh", refreshIcon());
        Button createTaskButton = toolbarIconButton("New Task", plusIcon());
        createTaskButton.setVisible(context.authorizationService().hasAnyRole(user, Role.ADMIN, Role.MANAGER));
        createTaskButton.setManaged(createTaskButton.isVisible());
        HBox toolbarMenu = new HBox(refreshButton, createTaskButton);
        toolbarMenu.setAlignment(Pos.CENTER_LEFT);
        toolbarMenu.getStyleClass().add("notifications-toolbar-capsule");
        Label selectedCountLabel = new Label("0 selected");
        selectedCountLabel.getStyleClass().add("notification-selection-label");
        Button markSelectedButton = ButtonFactory.pageNav("Mark read");
        Button dismissSelectedButton = ButtonFactory.pageNav("Dismiss");
        markSelectedButton.setDisable(true);
        dismissSelectedButton.setDisable(true);
        HBox bulkActions = new HBox(8, selectedCountLabel, markSelectedButton, dismissSelectedButton);
        bulkActions.setAlignment(Pos.CENTER_LEFT);
        bulkActions.getStyleClass().add("notifications-bulk-actions");
        updateSelectionControls(null, selectedCountLabel, markSelectedButton, dismissSelectedButton);

        HBox toolbar = new HBox(10, filterRow, headerSpacer, bulkActions, countLabel, toolbarMenu);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Runnable[] loadRef = new Runnable[1];
        ListView<NotificationView> listView = new ListView<>();
        listView.getStyleClass().add("notifications-list");
        listView.setFocusTraversable(false);
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        enableNotificationDragSelection(listView);
        installNotificationSelectionClearOnOutsideClick(root, listView);
        listView.getSelectionModel().getSelectedItems().addListener(
            (ListChangeListener<NotificationView>) change ->
                updateSelectionControls(listView, selectedCountLabel, markSelectedButton, dismissSelectedButton)
        );
        listView.setPlaceholder(createLoadingPlaceholder());
        listView.setCellFactory(view -> new ListCell<>() {
            private Region cardRegion;

            {
                selectedProperty().addListener((obs, wasSelected, isSelected) ->
                    syncNotificationCardSelection(cardRegion, isSelected)
                );
            }

            @Override
            protected void updateItem(NotificationView item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    cardRegion = null;
                    setGraphic(null);
                    return;
                }
                Node card = createNotificationCard(context, user, item, listView, activeFilter, countLabel, unreadCount);
                if (card instanceof Region region) {
                    region.setMaxWidth(Double.MAX_VALUE);
                    cardRegion = region;
                    syncNotificationCardSelection(cardRegion, isSelected());
                } else {
                    cardRegion = null;
                }
                setGraphic(card);
            }
        });
        VBox.setVgrow(listView, Priority.ALWAYS);

        root.getChildren().addAll(toolbar, listView);

        boolean[] refreshNextLoad = {false};
        loadRef[0] = () -> {
            boolean refreshSystemState = refreshNextLoad[0];
            refreshNextLoad[0] = false;
            loadNotifications(context, user, activeFilter.get(), loadVersion, listView, countLabel, loadRef, unreadCount, refreshSystemState);
        };

        allButton.setOnAction(event -> {
            activeFilter.set(NotificationFilter.ALL);
            applyFilterButtonState(filterButtons, allButton);
            loadRef[0].run();
        });
        unreadButton.setOnAction(event -> {
            activeFilter.set(NotificationFilter.UNREAD);
            applyFilterButtonState(filterButtons, unreadButton);
            loadRef[0].run();
        });
        criticalButton.setOnAction(event -> {
            activeFilter.set(NotificationFilter.CRITICAL);
            applyFilterButtonState(filterButtons, criticalButton);
            loadRef[0].run();
        });
        taskButton.setOnAction(event -> {
            activeFilter.set(NotificationFilter.TASKS);
            applyFilterButtonState(filterButtons, taskButton);
            loadRef[0].run();
        });
        refreshButton.setOnAction(event -> {
            refreshNextLoad[0] = true;
            loadRef[0].run();
        });
        createTaskButton.setOnAction(event ->
            showCreateTaskDialog(context, user, listView, activeFilter, countLabel, unreadCount)
        );
        markSelectedButton.setOnAction(event -> runBulkStateAction(
            markSelectedButton,
            dismissSelectedButton,
            context,
            user,
            listView,
            activeFilter,
            countLabel,
            unreadCount,
            "Saving...",
            (service, notificationId) -> service.markRead(user, notificationId),
            "notification-bulk-mark-read"
        ));
        dismissSelectedButton.setOnAction(event -> runBulkStateAction(
            dismissSelectedButton,
            markSelectedButton,
            context,
            user,
            listView,
            activeFilter,
            countLabel,
            unreadCount,
            "Dismissing...",
            (service, notificationId) -> service.dismiss(user, notificationId),
            "notification-bulk-dismiss"
        ));

        Platform.runLater(loadRef[0]);
        return root;
    }

    private static void loadNotifications(
        SceneRuntimeContext context,
        User user,
        NotificationFilter filter,
        AtomicLong loadVersion,
        ListView<NotificationView> listView,
        Label countLabel,
        Runnable[] loadRef,
        AtomicLong unreadCount,
        boolean refreshSystemState
    ) {
        long version = loadVersion.incrementAndGet();
        listView.setItems(FXCollections.observableArrayList());
        listView.setPlaceholder(createLoadingPlaceholder());
        javafx.concurrent.Task<NotificationLoadResult> task = new javafx.concurrent.Task<>() {
            @Override
            protected NotificationLoadResult call() {
                if (refreshSystemState) {
                    context.notificationService().refreshSystemNotifications();
                }
                List<NotificationView> items = context.notificationService().listForUser(user, filter);
                return new NotificationLoadResult(items, context.notificationService().countUnread(user));
            }
        };
        task.setOnSucceeded(event -> {
            if (version != loadVersion.get()) {
                return;
            }
            NotificationLoadResult result = task.getValue();
            List<NotificationView> items = result == null || result.items() == null ? List.of() : result.items();
            unreadCount.set(result == null ? 0L : Math.max(0L, result.unreadCount()));
            countLabel.setText(items.size() + (items.size() == 1 ? " item" : " items"));
            if (items.isEmpty()) {
                listView.setPlaceholder(createEmptyPlaceholder(filter));
                listView.setItems(FXCollections.observableArrayList());
            } else {
                listView.setItems(FXCollections.observableArrayList(items));
            }
            updateHeaderBadge(context, unreadCount.get());
        });
        task.setOnFailed(event -> {
            if (version != loadVersion.get()) {
                return;
            }
            Throwable ex = task.getException();
            context.showUserFacingError(ex);
            countLabel.setText("Could not load");
            listView.setItems(FXCollections.observableArrayList());
            listView.setPlaceholder(createErrorPlaceholder(loadRef[0]));
        });
        Thread worker = new Thread(task, "notifications-load");
        worker.setDaemon(true);
        worker.start();
    }

    private static Node createNotificationCard(
        SceneRuntimeContext context,
        User user,
        NotificationView item,
        ListView<NotificationView> listView,
        AtomicReference<NotificationFilter> activeFilter,
        Label countLabel,
        AtomicLong unreadCount
    ) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("notification-card", severityStyle(item.severity()));
        if (item.unread()) {
            card.getStyleClass().add("notification-card-unread");
        }

        Label severity = new Label(severityLabel(item.severity()));
        severity.getStyleClass().addAll("notification-severity-chip", severityStyle(item.severity()));
        Label type = new Label(typeLabel(item));
        type.getStyleClass().add("notification-type-chip");
        Region chipSpacer = new Region();
        HBox.setHgrow(chipSpacer, Priority.ALWAYS);
        Label time = new Label(formatDateTime(item.createdAt()));
        time.getStyleClass().add("notification-time-label");
        HBox chipRow = new HBox(8, severity, type, chipSpacer, time);
        chipRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(item.title());
        title.getStyleClass().add("notification-title");
        title.setWrapText(true);

        Label message = new Label(item.message());
        message.getStyleClass().add("notification-message");
        message.setWrapText(true);

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        if (item.createdByLabel() != null && !item.createdByLabel().isBlank()) {
            Label createdBy = new Label("From " + item.createdByLabel());
            createdBy.getStyleClass().add("notification-meta-label");
            metaRow.getChildren().add(createdBy);
        }
        if (item.completed()) {
            Label completed = new Label("Completed");
            completed.getStyleClass().add("notification-completed-chip");
            metaRow.getChildren().add(completed);
        }

        Button openButton = ButtonFactory.pageNav("Open");
        openButton.setVisible(item.actionTarget() != null);
        openButton.setManaged(item.actionTarget() != null);
        openButton.setOnAction(event -> {
            runStateAction(
                openButton,
                context,
                user,
                item,
                "Opening...",
                service -> service.markRead(user, item.id()),
                listView,
                activeFilter,
                countLabel,
                unreadCount
            );
            openActionTarget(context, item.actionTarget());
        });

        Button readButton = ButtonFactory.pageNav("Mark read");
        readButton.setDisable(!item.unread());
        readButton.setOnAction(event -> runStateAction(
            readButton,
            context,
            user,
            item,
            "Saving...",
            service -> service.markRead(user, item.id()),
            listView,
            activeFilter,
            countLabel,
            unreadCount
        ));

        Button completeButton = ButtonFactory.pageNav("Complete");
        completeButton.setVisible(item.category() == NotificationCategory.TASK && !item.completed());
        completeButton.setManaged(completeButton.isVisible());
        completeButton.setOnAction(event -> runStateAction(
            completeButton,
            context,
            user,
            item,
            "Saving...",
            service -> service.completeTask(user, item.id()),
            listView,
            activeFilter,
            countLabel,
            unreadCount
        ));

        Button dismissButton = ButtonFactory.pageNav("Dismiss");
        dismissButton.setOnAction(event -> runStateAction(
            dismissButton,
            context,
            user,
            item,
            "Saving...",
            service -> service.dismiss(user, item.id()),
            listView,
            activeFilter,
            countLabel,
            unreadCount
        ));

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        HBox actionRow = new HBox(8, actionSpacer, openButton, readButton, completeButton, dismissButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(chipRow, title, message);
        if (!metaRow.getChildren().isEmpty()) {
            card.getChildren().add(metaRow);
        }
        card.getChildren().add(actionRow);
        return card;
    }

    private static void runStateAction(
        Button sourceButton,
        SceneRuntimeContext context,
        User user,
        NotificationView currentItem,
        String busyText,
        java.util.function.Function<NotificationService, NotificationView> action,
        ListView<NotificationView> listView,
        AtomicReference<NotificationFilter> activeFilter,
        Label countLabel,
        AtomicLong unreadCount
    ) {
        AsyncUiTask.runButtonTask(
            sourceButton,
            null,
            busyText,
            () -> action.apply(context.notificationService()),
            result -> {
                updateUnreadCountAfterStateChange(context, currentItem, result, unreadCount);
                applyLocalNotificationUpdate(listView, activeFilter.get(), countLabel, result);
            },
            context::showUserFacingError,
            "notification-state-action-" + currentItem.id()
        );
    }

    private static void runBulkStateAction(
        Button primaryButton,
        Button secondaryButton,
        SceneRuntimeContext context,
        User user,
        ListView<NotificationView> listView,
        AtomicReference<NotificationFilter> activeFilter,
        Label countLabel,
        AtomicLong unreadCount,
        String busyText,
        BiFunction<NotificationService, Long, NotificationView> action,
        String threadName
    ) {
        List<NotificationView> selectedItems = listView == null
            ? List.of()
            : new ArrayList<>(listView.getSelectionModel().getSelectedItems());
        if (selectedItems.isEmpty()) {
            return;
        }
        Map<Long, NotificationView> beforeById = new LinkedHashMap<>();
        for (NotificationView item : selectedItems) {
            if (item != null && item.id() != null) {
                beforeById.put(item.id(), item);
            }
        }

        AsyncUiTask.runButtonTask(
            primaryButton,
            secondaryButton,
            busyText,
            () -> {
                List<NotificationView> results = new ArrayList<>();
                for (NotificationView item : selectedItems) {
                    if (item == null || item.id() == null) {
                        continue;
                    }
                    results.add(action.apply(context.notificationService(), item.id()));
                }
                return results;
            },
            results -> {
                if (results != null) {
                    for (NotificationView result : results) {
                        if (result == null) {
                            continue;
                        }
                        updateUnreadCountAfterStateChange(context, beforeById.get(result.id()), result, unreadCount);
                        applyLocalNotificationUpdate(listView, activeFilter.get(), countLabel, result);
                    }
                }
                listView.getSelectionModel().clearSelection();
                updateSelectionControls(listView, null, primaryButton, secondaryButton);
            },
            context::showUserFacingError,
            threadName
        );
    }

    private static void updateUnreadCountAfterStateChange(
        SceneRuntimeContext context,
        NotificationView before,
        NotificationView after,
        AtomicLong unreadCount
    ) {
        if (before != null && before.unread() && (after == null || !after.unread())) {
            unreadCount.updateAndGet(value -> Math.max(0L, value - 1L));
        }
        updateHeaderBadge(context, unreadCount.get());
    }

    private static void applyLocalNotificationUpdate(
        ListView<NotificationView> listView,
        NotificationFilter filter,
        Label countLabel,
        NotificationView updated
    ) {
        if (updated == null || listView.getItems() == null) {
            return;
        }
        ObservableList<NotificationView> items = listView.getItems();
        int index = indexOfNotification(items, updated.id());
        boolean shouldRemainVisible = updated.dismissedAt() == null && matchesFilter(updated, filter);
        if (!shouldRemainVisible) {
            if (index >= 0) {
                items.remove(index);
            }
        } else if (index >= 0) {
            items.set(index, updated);
            FXCollections.sort(items, viewComparator());
        } else {
            items.add(updated);
            FXCollections.sort(items, viewComparator());
        }
        updateListCountAndPlaceholder(listView, countLabel, filter);
    }

    private static int indexOfNotification(List<NotificationView> items, Long notificationId) {
        if (notificationId == null) {
            return -1;
        }
        for (int i = 0; i < items.size(); i++) {
            NotificationView item = items.get(i);
            if (item != null && notificationId.equals(item.id())) {
                return i;
            }
        }
        return -1;
    }

    private static void updateListCountAndPlaceholder(
        ListView<NotificationView> listView,
        Label countLabel,
        NotificationFilter filter
    ) {
        int size = listView.getItems() == null ? 0 : listView.getItems().size();
        countLabel.setText(size + (size == 1 ? " item" : " items"));
        if (size == 0) {
            listView.setPlaceholder(createEmptyPlaceholder(filter));
        }
    }

    private static void updateSelectionControls(
        ListView<NotificationView> listView,
        Label selectedCountLabel,
        Button markSelectedButton,
        Button dismissSelectedButton
    ) {
        List<NotificationView> selectedItems = listView == null
            ? List.of()
            : listView.getSelectionModel().getSelectedItems();
        int selectedCount = selectedItems.size();
        boolean hasSelection = selectedCount > 0;
        boolean hasUnreadSelection = selectedItems.stream().anyMatch(NotificationView::unread);

        if (selectedCountLabel != null) {
            selectedCountLabel.setText(selectedCount + " selected");
            selectedCountLabel.setVisible(hasSelection);
            selectedCountLabel.setManaged(hasSelection);
        }
        if (markSelectedButton != null) {
            markSelectedButton.setDisable(!hasUnreadSelection);
            markSelectedButton.setVisible(hasSelection);
            markSelectedButton.setManaged(hasSelection);
        }
        if (dismissSelectedButton != null) {
            dismissSelectedButton.setDisable(!hasSelection);
            dismissSelectedButton.setVisible(hasSelection);
            dismissSelectedButton.setManaged(hasSelection);
        }
    }

    private static void syncNotificationCardSelection(Region card, boolean selected) {
        if (card == null) {
            return;
        }
        if (selected) {
            if (!card.getStyleClass().contains("notification-card-selected")) {
                card.getStyleClass().add("notification-card-selected");
            }
        } else {
            card.getStyleClass().remove("notification-card-selected");
        }
    }

    private static void enableNotificationDragSelection(ListView<NotificationView> listView) {
        final int[] dragAnchor = new int[] { -1 };
        final int[] pressedIndex = new int[] { -1 };
        final int[] lastRangeStart = new int[] { -1 };
        final int[] lastRangeEnd = new int[] { -1 };
        final boolean[] selectedOnPress = new boolean[] { false };
        final boolean[] rangeDragActive = new boolean[] { false };
        listView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || isInteractiveTarget(event.getTarget(), listView)) {
                dragAnchor[0] = -1;
                pressedIndex[0] = -1;
                return;
            }
            ListCell<?> cell = findNotificationCell(event.getPickResult().getIntersectedNode(), listView);
            if (cell == null || cell.isEmpty()) {
                listView.getSelectionModel().clearSelection();
                dragAnchor[0] = -1;
                pressedIndex[0] = -1;
                event.consume();
                return;
            }
            int index = cell.getIndex();
            dragAnchor[0] = index;
            pressedIndex[0] = index;
            lastRangeStart[0] = -1;
            lastRangeEnd[0] = -1;
            selectedOnPress[0] = listView.getSelectionModel().isSelected(index);
            rangeDragActive[0] = false;
            event.consume();
        });
        listView.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!event.isPrimaryButtonDown() || dragAnchor[0] < 0) {
                return;
            }
            ListCell<?> cell = findNotificationCell(event.getPickResult().getIntersectedNode(), listView);
            if (cell == null || cell.isEmpty()) {
                return;
            }
            int currentIndex = cell.getIndex();
            int start = Math.min(dragAnchor[0], currentIndex);
            int end = Math.max(dragAnchor[0], currentIndex);
            if (start == lastRangeStart[0] && end == lastRangeEnd[0]) {
                event.consume();
                return;
            }
            listView.getSelectionModel().clearSelection();
            listView.getSelectionModel().selectRange(start, end + 1);
            lastRangeStart[0] = start;
            lastRangeEnd[0] = end;
            rangeDragActive[0] = true;
            event.consume();
        });
        listView.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && pressedIndex[0] >= 0 && !rangeDragActive[0]) {
                ListCell<?> cell = findNotificationCell(event.getPickResult().getIntersectedNode(), listView);
                if (cell != null && !cell.isEmpty() && cell.getIndex() == pressedIndex[0]) {
                    if (selectedOnPress[0]) {
                        listView.getSelectionModel().clearSelection(pressedIndex[0]);
                    } else {
                        listView.getSelectionModel().select(pressedIndex[0]);
                    }
                    event.consume();
                }
            }
            dragAnchor[0] = -1;
            pressedIndex[0] = -1;
            lastRangeStart[0] = -1;
            lastRangeEnd[0] = -1;
            selectedOnPress[0] = false;
            rangeDragActive[0] = false;
        });
    }

    private static void installNotificationSelectionClearOnOutsideClick(
        VBox root,
        ListView<NotificationView> listView
    ) {
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY
                || isInsideNode(event.getTarget(), listView)
                || isInteractiveTarget(event.getTarget(), root)) {
                return;
            }
            listView.getSelectionModel().clearSelection();
        });
    }

    private static ListCell<?> findNotificationCell(Node node, ListView<?> listView) {
        Node current = node;
        while (current != null && current != listView) {
            if (current instanceof ListCell<?> cell) {
                return cell;
            }
            current = current.getParent();
        }
        return null;
    }

    private static boolean isInsideNode(Object target, Node boundary) {
        if (!(target instanceof Node node)) {
            return false;
        }
        Node current = node;
        while (current != null) {
            if (current == boundary) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static boolean isInteractiveTarget(Object target, Node boundary) {
        if (!(target instanceof Node node)) {
            return false;
        }
        Node current = node;
        while (current != null && current != boundary) {
            if (current instanceof Button
                || current instanceof CheckBox
                || current instanceof ComboBox<?>
                || current instanceof TextField
                || current instanceof TextArea) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static void openActionTarget(SceneRuntimeContext context, NotificationActionTarget target) {
        if (target == null) {
            return;
        }
        switch (target) {
            case PRODUCTS -> context.navigator().showProducts(ProductViewPreset.LOW_STOCK);
            case SALES_POS -> context.navigator().showSales();
            case REPORTS_SHIFTS -> context.navigator().showReports(null, null, ReportFocusTarget.SHIFTS);
            case SETTINGS_QR -> context.navigator().showSettings();
            case PROMOTIONS -> context.navigator().showPromotions();
            case STOCKTAKE -> context.navigator().showStocktake();
        }
    }

    private static void showCreateTaskDialog(
        SceneRuntimeContext context,
        User actor,
        ListView<NotificationView> listView,
        AtomicReference<NotificationFilter> activeFilter,
        Label countLabel,
        AtomicLong unreadCount
    ) {
        javafx.concurrent.Task<List<RecipientOption>> loadRecipientsTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<RecipientOption> call() {
                return context.notificationService().listAssignableUsers(actor);
            }
        };
        loadRecipientsTask.setOnSucceeded(event -> openCreateTaskDialog(
            context,
            actor,
            loadRecipientsTask.getValue(),
            listView,
            activeFilter,
            countLabel,
            unreadCount
        ));
        loadRecipientsTask.setOnFailed(event -> context.showUserFacingError(loadRecipientsTask.getException()));
        Thread worker = new Thread(loadRecipientsTask, "notification-task-recipients-load");
        worker.setDaemon(true);
        worker.start();
    }

    private static void openCreateTaskDialog(
        SceneRuntimeContext context,
        User actor,
        List<RecipientOption> recipients,
        ListView<NotificationView> listView,
        AtomicReference<NotificationFilter> activeFilter,
        Label countLabel,
        AtomicLong unreadCount
    ) {
        Dialog<CreateTaskRequest> dialog = new Dialog<>();
        Window owner = context.owner();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("New Task");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("notifications-task-dialog-pane");
        ButtonType createType = new ButtonType("Create Task", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, createType);

        TextField titleField = DialogFormFactory.textField("", "Task title");
        titleField.getStyleClass().add("product-dialog-input");
        TextArea messageArea = DialogFormFactory.textArea("", "Task details", 3);
        messageArea.getStyleClass().add("product-dialog-text-area");
        ComboBox<NotificationSeverity> severityBox = new ComboBox<>(FXCollections.observableArrayList(NotificationSeverity.values()));
        severityBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(NotificationSeverity severity) {
                return severity == null ? "" : severityLabel(severity);
            }

            @Override
            public NotificationSeverity fromString(String value) {
                return severityBox.getValue();
            }
        });
        severityBox.setValue(NotificationSeverity.INFO);
        severityBox.setMaxWidth(Double.MAX_VALUE);
        severityBox.getStyleClass().addAll("product-dialog-combo-box", "promotion-dialog-combo-box");
        DialogFormFactory.installPopupCells(severityBox);

        CheckBox allStaff = new CheckBox("All Staff");
        CheckBox allManagers = new CheckBox("All Managers");
        VBox userChecks = new VBox(6);
        userChecks.getStyleClass().add("notifications-task-user-checks");
        List<CheckBox> userCheckBoxes = new ArrayList<>();
        for (RecipientOption recipient : recipients == null ? List.<RecipientOption>of() : recipients) {
            CheckBox checkBox = new CheckBox(recipient.displayLabel());
            checkBox.setUserData(recipient.id());
            userCheckBoxes.add(checkBox);
            userChecks.getChildren().add(checkBox);
        }
        ScrollPane recipientScroll = new ScrollPane(userChecks);
        recipientScroll.setFitToWidth(true);
        recipientScroll.setPrefHeight(150);
        recipientScroll.getStyleClass().add("notifications-task-recipient-scroll");

        VBox header = DialogFormFactory.header("New Task", "Internal notification");
        VBox taskDetails = DialogFormFactory.section("Task Details", new VBox(12,
            DialogFormFactory.fieldBlock("Title *", titleField, null),
            DialogFormFactory.fieldBlock("Details", messageArea, null),
            DialogFormFactory.fieldBlock("Severity", severityBox, null)
        ));
        HBox recipientGroups = new HBox(14, allStaff, allManagers);
        recipientGroups.getStyleClass().add("notifications-task-recipient-groups");
        VBox recipientsContent = new VBox(10, recipientGroups, recipientScroll);
        VBox recipientsSection = DialogFormFactory.section("Recipients", recipientsContent);
        VBox content = new VBox(16,
            header,
            taskDetails,
            recipientsSection
        );
        content.getStyleClass().add("notifications-task-dialog");
        content.setPrefWidth(540);
        dialog.getDialogPane().setContent(content);

        Button createButton = (Button) dialog.getDialogPane().lookupButton(createType);
        createButton.getStyleClass().addAll("button", "primary-button", "product-dialog-primary-button", "no-hover-button");
        createButton.disableProperty().bind(titleField.textProperty().isEmpty());
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().addAll("button", "product-dialog-secondary-button", "dialog-cancel-button", "no-hover-button");
        dialog.setResultConverter(buttonType -> {
            if (buttonType != createType) {
                return null;
            }
            Set<Role> roles = EnumSet.noneOf(Role.class);
            if (allStaff.isSelected()) {
                roles.add(Role.STAFF);
            }
            if (allManagers.isSelected()) {
                roles.add(Role.MANAGER);
            }
            Set<Long> userIds = userCheckBoxes.stream()
                .filter(CheckBox::isSelected)
                .map(checkBox -> (Long) checkBox.getUserData())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            return new CreateTaskRequest(
                titleField.getText(),
                messageArea.getText(),
                userIds,
                roles,
                severityBox.getValue(),
                null,
                null,
                null
            );
        });
        dialog.showAndWait().ifPresent(request -> AsyncUiTask.runButtonTask(
            createTaskButtonPlaceholder(),
            null,
            "Creating...",
            () -> {
                NotificationView created = context.notificationService().createTask(actor, request);
                NotificationView visibleItem = context.notificationService()
                    .findVisibleForUser(actor, created.id())
                    .orElse(null);
                return new NotificationCreateResult(visibleItem);
            },
            result -> {
                context.toastService().showSuccess("Task notification created");
                NotificationView visibleItem = result == null ? null : result.visibleItem();
                if (visibleItem != null) {
                    if (visibleItem.unread()) {
                        unreadCount.updateAndGet(value -> value + 1L);
                        updateHeaderBadge(context, unreadCount.get());
                    }
                    applyLocalNotificationUpdate(listView, activeFilter.get(), countLabel, visibleItem);
                }
            },
            context::showUserFacingError,
            "notification-create-task"
        ));
    }

    private static Button createTaskButtonPlaceholder() {
        Button button = new Button();
        button.setVisible(false);
        return button;
    }

    private static Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("notification-form-label");
        return label;
    }

    private static Button filterButton(String label) {
        Button button = ButtonFactory.pageNav(label);
        button.getStyleClass().add("notification-filter-button");
        return button;
    }

    private static Button toolbarIconButton(String tooltipText, Node icon) {
        Button button = new Button();
        button.getStyleClass().add("notification-toolbar-icon-button");
        button.setGraphic(icon);
        button.setTooltip(new Tooltip(tooltipText));
        button.setFocusTraversable(false);
        return button;
    }

    private static Node refreshIcon() {
        SVGPath icon = toolbarStrokeIcon("M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16M3 21v-5h5M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8M21 3v5h-5");
        StackPane wrap = new StackPane(icon);
        wrap.setMinSize(20, 20);
        wrap.setPrefSize(20, 20);
        wrap.setMaxSize(20, 20);
        return wrap;
    }

    private static Node plusIcon() {
        SVGPath icon = toolbarStrokeIcon("M12 5v14M5 12h14");
        StackPane wrap = new StackPane(icon);
        wrap.setMinSize(20, 20);
        wrap.setPrefSize(20, 20);
        wrap.setMaxSize(20, 20);
        return wrap;
    }

    private static SVGPath toolbarStrokeIcon(String content) {
        SVGPath icon = new SVGPath();
        icon.setContent(content);
        icon.getStyleClass().add("notification-toolbar-icon-stroke");
        icon.setFill(Color.TRANSPARENT);
        icon.setStrokeLineCap(StrokeLineCap.ROUND);
        icon.setStrokeWidth(2.1);
        icon.setMouseTransparent(true);
        return icon;
    }

    private static void applyFilterButtonState(List<Button> buttons, Button active) {
        for (Button button : buttons) {
            button.getStyleClass().remove("notification-filter-button-active");
        }
        active.getStyleClass().add("notification-filter-button-active");
    }

    private static Node createLoadingPlaceholder() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(36, 36);
        Label label = new Label("Loading notifications...");
        label.getStyleClass().add("notification-placeholder-label");
        VBox box = new VBox(12, spinner, label);
        box.setAlignment(Pos.CENTER);
        box.setMinHeight(320);
        return box;
    }

    private static Node createEmptyPlaceholder(NotificationFilter filter) {
        Label label = new Label(filter == NotificationFilter.ALL ? "No active notifications" : "No notifications match this filter");
        label.getStyleClass().add("notification-placeholder-label");
        StackPane pane = new StackPane(label);
        pane.setMinHeight(320);
        return pane;
    }

    private static Node createErrorPlaceholder(Runnable retryAction) {
        Label label = new Label("Could not load notifications");
        label.getStyleClass().add("notification-placeholder-label");
        Button retry = ButtonFactory.pageNav("Retry");
        retry.setOnAction(event -> retryAction.run());
        VBox box = new VBox(12, label, retry);
        box.setAlignment(Pos.CENTER);
        box.setMinHeight(320);
        return box;
    }

    private static boolean matchesFilter(NotificationView view, NotificationFilter filter) {
        NotificationFilter effectiveFilter = filter == null ? NotificationFilter.ALL : filter;
        return switch (effectiveFilter) {
            case ALL -> true;
            case UNREAD -> view.unread();
            case CRITICAL -> view.severity() == NotificationSeverity.CRITICAL;
            case TASKS -> view.category() == NotificationCategory.TASK;
        };
    }

    private static Comparator<NotificationView> viewComparator() {
        return Comparator
            .comparing((NotificationView view) -> view.unread() ? 0 : 1)
            .thenComparing(view -> severityRank(view.severity()))
            .thenComparing(NotificationView::createdAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static int severityRank(NotificationSeverity severity) {
        if (severity == NotificationSeverity.CRITICAL) {
            return 0;
        }
        if (severity == NotificationSeverity.WARNING) {
            return 1;
        }
        return 2;
    }

    private static void updateHeaderBadge(SceneRuntimeContext context, long unread) {
        if (context.owner() == null || context.owner().getScene() == null) {
            return;
        }
        Node badgeNode = context.owner().getScene().getRoot().lookup(".header-notification-badge");
        if (badgeNode instanceof Label badge) {
            badge.setText(unread > 99 ? "99+" : Long.toString(unread));
            badge.setVisible(unread > 0);
            badge.setManaged(unread > 0);
        }
    }

    private static String severityStyle(NotificationSeverity severity) {
        if (severity == NotificationSeverity.CRITICAL) {
            return "notification-critical";
        }
        if (severity == NotificationSeverity.WARNING) {
            return "notification-warning";
        }
        return "notification-info";
    }

    private static String severityLabel(NotificationSeverity severity) {
        if (severity == NotificationSeverity.CRITICAL) {
            return "Critical";
        }
        if (severity == NotificationSeverity.WARNING) {
            return "Warning";
        }
        return "Info";
    }

    private static String typeLabel(NotificationView view) {
        if (view.category() == NotificationCategory.TASK) {
            return "Task";
        }
        String name = view.type() == null ? "System" : view.type().name().replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }
}
