package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.ui.component.ActionTaskbarFactory;
import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.component.ExpandableSearchControl;
import com.pbl3.project.pbl3_project.ui.component.FilterControlFactory;
import com.pbl3.project.pbl3_project.ui.component.FilterDisclosureSection;
import com.pbl3.project.pbl3_project.ui.dialog.AccountDialog;
import com.pbl3.project.pbl3_project.ui.util.AsyncPageCache;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.FxFormatters;
import com.pbl3.project.pbl3_project.ui.util.SortCriterion;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;
import javafx.util.Callback;
import javafx.util.Duration;
import org.springframework.data.domain.Page;

public final class AccountsScene {
    private static final Color PRIMARY_COLOR = Color.web("#1d7df2");

    public record Options() {
    }

    private record AccountPageResult(Page<User> page, int pageIndex, Long restoreId, double previousScrollValue) {
    }

    private AccountsScene() {
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        ExpandableSearchControl searchControl = ExpandableSearchControl.create(300, PRIMARY_COLOR);
        TextField searchField = searchControl.field();

        final int pageSize = 20;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        Runnable[] loadPageRef = new Runnable[1];
        AtomicLong pageLoadVersion = new AtomicLong();
        AsyncPageCache<AccountPageResult> pageCache = new AsyncPageCache<>(80);
        AtomicReference<Set<Role>> roleFiltersRef = new AtomicReference<>(new LinkedHashSet<>());
        AtomicReference<Boolean> enabledFilterRef = new AtomicReference<>(null);

        HBox filterBox = createFilterButton();
        Popup filterPopup = new Popup();
        filterPopup.setAutoHide(true);

        FilterControlFactory.Shell filterShell = FilterControlFactory.shell(320, 240);
        VBox popupContainer = filterShell.container();
        VBox scrollContent = filterShell.content();

        Label roleLabel = FilterControlFactory.sectionTitle("Role");
        CheckBox allRolesCb = new CheckBox("All Roles");
        allRolesCb.setSelected(true);
        styleFilterCheckBox(allRolesCb);
        VBox roleScroll = new VBox(8);
        roleScroll.setPadding(new Insets(5, 5, 5, 20));
        List<CheckBox> roleCbs = new ArrayList<>();
        for (Role role : Role.values()) {
            CheckBox cb = new CheckBox(FxFormatters.roleLabel(role));
            cb.setUserData(role);
            cb.setSelected(true);
            styleFilterCheckBox(cb);
            cb.setOnAction(e -> syncAllCheckbox(allRolesCb, roleCbs));
            roleCbs.add(cb);
            roleScroll.getChildren().add(cb);
        }
        allRolesCb.setOnAction(e -> roleCbs.forEach(cb -> cb.setSelected(allRolesCb.isSelected())));
        FilterDisclosureSection roleSection = new FilterDisclosureSection(allRolesCb, roleScroll);

        Label statusLabel = FilterControlFactory.sectionTitle("Status");
        CheckBox allStatusesCb = new CheckBox("All Statuses");
        allStatusesCb.setSelected(true);
        styleFilterCheckBox(allStatusesCb);
        VBox statusScroll = new VBox(8);
        statusScroll.setPadding(new Insets(5, 5, 5, 20));
        List<CheckBox> statusCbs = new ArrayList<>();
        Map<String, Boolean> accountStatuses = new LinkedHashMap<>();
        accountStatuses.put(FxFormatters.userStatus(true), Boolean.TRUE);
        accountStatuses.put(FxFormatters.userStatus(false), Boolean.FALSE);
        for (Map.Entry<String, Boolean> entry : accountStatuses.entrySet()) {
            CheckBox cb = new CheckBox(entry.getKey());
            cb.setUserData(entry.getValue());
            cb.setSelected(true);
            styleFilterCheckBox(cb);
            cb.setOnAction(e -> syncAllCheckbox(allStatusesCb, statusCbs));
            statusCbs.add(cb);
            statusScroll.getChildren().add(cb);
        }
        allStatusesCb.setOnAction(e -> statusCbs.forEach(cb -> cb.setSelected(allStatusesCb.isSelected())));
        FilterDisclosureSection statusSection = new FilterDisclosureSection(allStatusesCb, statusScroll);

        Button resetFilterBtn = new Button("Reset");
        resetFilterBtn.getStyleClass().add("filter-reset-btn");
        resetFilterBtn.setOnAction(e -> {
            allRolesCb.setSelected(true);
            roleCbs.forEach(cb -> cb.setSelected(true));
            roleSection.setExpanded(false);
            allStatusesCb.setSelected(true);
            statusCbs.forEach(cb -> cb.setSelected(true));
            statusSection.setExpanded(false);
            roleFiltersRef.set(new LinkedHashSet<>());
            enabledFilterRef.set(null);
            currentPage[0] = 0;
            loadPageRef[0].run();
            filterBox.setStyle("");
            filterPopup.hide();
        });

        Button applyFilterBtn = new Button("Apply Filter");
        applyFilterBtn.getStyleClass().add("filter-apply-btn");
        applyFilterBtn.setOnAction(e -> {
            Set<Role> selectedRoles = new LinkedHashSet<>();
            for (CheckBox cb : roleCbs) {
                if (cb.isSelected()) {
                    selectedRoles.add((Role) cb.getUserData());
                }
            }
            Set<Boolean> selectedStatuses = new LinkedHashSet<>();
            for (CheckBox cb : statusCbs) {
                if (cb.isSelected()) {
                    selectedStatuses.add((Boolean) cb.getUserData());
                }
            }
            roleFiltersRef.set(allRolesCb.isSelected() ? new LinkedHashSet<>() : selectedRoles);
            enabledFilterRef.set(selectedStatuses.size() == 1 ? selectedStatuses.iterator().next() : null);
            currentPage[0] = 0;
            loadPageRef[0].run();
            boolean hasFilter = !allRolesCb.isSelected() || !allStatusesCb.isSelected();
            filterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
            filterPopup.hide();
        });

        scrollContent.getChildren().addAll(roleLabel, roleSection.getNode(), new Separator(), statusLabel, statusSection.getNode());
        popupContainer.getChildren().add(FilterControlFactory.actionRow(resetFilterBtn, applyFilterBtn));
        filterPopup.getContent().add(popupContainer);
        filterBox.setOnMouseClicked(e -> togglePopup(context, filterPopup, filterBox, -200, 5));

        Button createButton = ButtonFactory.expandableGreenAction("Create Account", 180);
        Button editButton = ActionTaskbarFactory.createButton(ActionTaskbarFactory.icon("edit"), "Edit Account", "promotion-taskbar-button-edit");
        Button resetPwdButton = ActionTaskbarFactory.createButton(ActionTaskbarFactory.icon("key"), "Reset Password", "promotion-taskbar-button-reset");
        Button toggleStatusButton = ActionTaskbarFactory.createButton(ActionTaskbarFactory.icon("power"), "Enable / Disable Account", "promotion-taskbar-button-toggle");
        editButton.setDisable(true);
        resetPwdButton.setDisable(true);
        toggleStatusButton.setDisable(true);

        TableSortState accountSortState = context.support().getOrCreateTableSortState(
            "accounts",
            new SortCriterion("username", TableColumn.SortType.ASCENDING)
        );
        LinkedHashMap<String, String> accountSortProperties = new LinkedHashMap<>();
        accountSortProperties.put("username", "username");
        accountSortProperties.put("fullName", "fullName");
        accountSortProperties.put("role", "role");
        LinkedHashMap<String, String> accountSortLabels = new LinkedHashMap<>();
        accountSortLabels.put("username", "Username");
        accountSortLabels.put("fullName", "Full Name");
        accountSortLabels.put("role", "Role");

        TableView<User> table = new TableView<>();
        context.support().applyStandardTableSizing(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        TableColumn<User, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        TableColumn<User, String> fullNameCol = new TableColumn<>("Full Name");
        fullNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(data -> new SimpleStringProperty(FxFormatters.roleLabel(data.getValue().getRole())));
        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(FxFormatters.userStatus(data.getValue().isEnabled())));
        statusCol.setCellFactory(enabledStatusCellFactory());
        table.getColumns().addAll(usernameCol, fullNameCol, roleCol, statusCol);

        LinkedHashMap<String, TableColumn<User, ?>> accountSortColumns = new LinkedHashMap<>();
        accountSortColumns.put("username", usernameCol);
        accountSortColumns.put("fullName", fullNameCol);
        accountSortColumns.put("role", roleCol);
        context.support().installSortHeaderIndicators(accountSortColumns);

        Label rowCountLabel = context.support().createStatusMetaLabel(MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label pageLabel = context.support().createStatusMetaLabel(MessageFormat.format("Page {0} / {1}", 0, 0));
        Button prevBtn = ButtonFactory.pageNav("Prev");
        Button nextBtn = ButtonFactory.pageNav("Next");
        AtomicReference<Long> accountSelectionRestoreId = new AtomicReference<>();

        Runnable updateStatusBar = () -> context.support().updatePagedStatus(
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

        Runnable loadPage = () -> {
            Long restoreId = accountSelectionRestoreId.getAndSet(null);
            double previousScrollValue = restoreId != null ? context.support().getTableVerticalScrollValue(table) : Double.NaN;
            String searchText = searchField.getText();
            Set<Role> roleFilters = new LinkedHashSet<>(roleFiltersRef.get());
            Boolean enabledFilter = enabledFilterRef.get();
            int requestedPage = currentPage[0];
            List<SortCriterion> sortSnapshot = accountSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(
                pageIndex,
                searchText,
                roleFilters,
                enabledFilter,
                sortSnapshot
            );
            java.util.function.IntFunction<AccountPageResult> fetchAccountPage = pageIndex -> {
                int resolvedPage = pageIndex;
                Page<User> pageData = context.userAccountService().searchUsers(
                    user,
                    searchText,
                    roleFilters,
                    enabledFilter,
                    context.support().createPageable(sortForLoad, accountSortProperties, resolvedPage, pageSize)
                );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = context.userAccountService().searchUsers(
                        user,
                        searchText,
                        roleFilters,
                        enabledFilter,
                        context.support().createPageable(sortForLoad, accountSortProperties, resolvedPage, pageSize)
                    );
                }
                return new AccountPageResult(pageData, resolvedPage, null, Double.NaN);
            };

            AsyncUiTask.runLatestCachedTableLoad(
                table,
                prevBtn,
                nextBtn,
                pageLoadVersion,
                pageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchAccountPage.apply(requestedPage),
                result -> {
                    Page<User> pageData = result.page();
                    currentPage[0] = result.pageIndex();
                    totalElements[0] = pageData.getTotalElements();
                    totalPages[0] = pageData.getTotalPages();
                    table.setItems(FXCollections.observableArrayList(pageData.getContent()));
                    if (restoreId != null) {
                        context.support().restoreTableSelectionById(table, restoreId, User::getId);
                        context.support().restoreTableVerticalScrollValue(table, previousScrollValue);
                    }
                    updateStatusBar.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < totalPages[0]) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchAccountPage.apply(nextPage),
                            null,
                            "accounts-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchAccountPage.apply(previousPage),
                            null,
                            "accounts-prev-page-prefetch"
                        );
                    }
                },
                context::showUserFacingError,
                "Loading accounts...",
                "Could not load accounts",
                "accounts-page-loader"
            );
        };
        loadPageRef[0] = () -> {
            pageCache.clear();
            loadPage.run();
        };

        Label accountSortStatusLabel = context.support().createSortStatusLabel(accountSortState, accountSortLabels);
        Runnable applyAccountSortUi = () -> {
            context.support().applySortStateToTable(table, accountSortColumns, accountSortState);
            accountSortStatusLabel.setText(context.support().buildSortStatusText(accountSortState, accountSortLabels));
        };
        applyAccountSortUi.run();
        context.support().installManualServerSorting(table, accountSortColumns, accountSortState, () -> {
            applyAccountSortUi.run();
            currentPage[0] = 0;
            loadPage.run();
        });

        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                loadPage.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage[0] + 1 < totalPages[0]) {
                currentPage[0]++;
                loadPage.run();
            }
        });

        PauseTransition searchPause = new PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldValue, newValue) -> searchPause.playFromStart());

        createButton.setOnAction(e -> showAccountUpsertDialog(context, user, null, loadPage));
        editButton.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAccountUpsertDialog(context, user, selected, loadPage);
            }
        });
        resetPwdButton.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showResetAccountPasswordDialog(context, user, selected, loadPage);
            }
        });
        toggleStatusButton.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            try {
                accountSelectionRestoreId.set(selected.getId());
                User updatedUser = context.userAccountService().setUserEnabled(user, selected.getId(), !selected.isEnabled());
                syncSessionUser(user, updatedUser);
                context.toastService().showSuccess(updatedUser.isEnabled()
                    ? MessageFormat.format("Enabled {0}", updatedUser.getUsername())
                    : MessageFormat.format("Disabled {0}", updatedUser.getUsername()));
                loadPage.run();
                if (!context.authorizationService().canAccessAccounts(user)) {
                    context.navigator().showDashboard();
                }
            } catch (Exception ex) {
                context.toastService().showError(ex.getMessage());
            }
        });

        table.getSelectionModel().getSelectedItems().addListener((ListChangeListener<User>) c -> {
            boolean single = table.getSelectionModel().getSelectedItems().size() == 1;
            editButton.setDisable(!single);
            resetPwdButton.setDisable(!single);
            toggleStatusButton.setDisable(!single);
            User selected = single ? table.getSelectionModel().getSelectedItem() : null;
            toggleStatusButton.setTooltip(new Tooltip(
                selected == null ? "Enable / Disable Account" : (selected.isEnabled() ? "Disable Account" : "Enable Account")
            ));
            updateStatusBar.run();
        });

        table.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    showAccountUpsertDialog(context, user, row.getItem(), loadPage);
                }
            });
            return row;
        });

        BorderPane toolbar = new BorderPane();
        HBox accountActionTaskbar = ActionTaskbarFactory.create(editButton, resetPwdButton, toggleStatusButton);
        HBox rightBox = new HBox(12, filterBox, searchControl.box(), createButton);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        toolbar.setLeft(accountActionTaskbar);
        toolbar.setRight(rightBox);

        HBox statusBar = new HBox(15, accountSortStatusLabel, rowCountLabel, pageLabel, prevBtn, nextBtn);
        context.support().applyStandardTableStatusBar(statusBar);

        root.getChildren().addAll(toolbar, table, statusBar);
        VBox.setVgrow(table, Priority.ALWAYS);
        javafx.application.Platform.runLater(loadPage);
        TableViewSupport.enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private static HBox createFilterButton() {
        HBox filterBox = new HBox();
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("expandable-search-box");
        filterBox.setPrefSize(40, 40);
        filterBox.setMinSize(40, 40);
        filterBox.setMaxSize(40, 40);
        filterBox.setCursor(Cursor.HAND);
        SVGPath filterIcon = new SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.setFill(PRIMARY_COLOR);
        filterBox.getChildren().add(filterIcon);
        javafx.scene.control.Tooltip.install(filterBox, new javafx.scene.control.Tooltip("Filter"));
        return filterBox;
    }

    private static void togglePopup(SceneRuntimeContext context, Popup popup, Node owner, double xOffset, double yOffset) {
        if (popup.isShowing()) {
            popup.hide();
            return;
        }
        context.support().showPopupBelow(popup, owner, xOffset, yOffset);
    }

    private static void styleFilterCheckBox(CheckBox checkBox) {
        checkBox.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
    }

    private static void syncAllCheckbox(CheckBox allCheckBox, List<CheckBox> checkBoxes) {
        allCheckBox.setSelected(checkBoxes.stream().allMatch(CheckBox::isSelected));
    }

    private static Callback<TableColumn<User, String>, TableCell<User, String>> enabledStatusCellFactory() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                String color = FxFormatters.userStatus(true).equals(item) ? "-app-success-hover" : "-app-danger-hover";
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        };
    }

    private static void showAccountUpsertDialog(SceneRuntimeContext context, User actor, User target, Runnable onSuccess) {
        AccountDialog.showUpsert(context.owner(), actor, target, onSuccess, accountDialogContext(context));
    }

    private static void showResetAccountPasswordDialog(SceneRuntimeContext context, User actor, User target, Runnable onSuccess) {
        AccountDialog.showResetPassword(context.owner(), actor, target, onSuccess, accountDialogContext(context));
    }

    private static AccountDialog.Context accountDialogContext(SceneRuntimeContext context) {
        return new AccountDialog.Context(
            context.userAccountService(),
            context.toastService(),
            AccountsScene::syncSessionUser,
            context.authorizationService()::canAccessAccounts,
            (owner, actor) -> context.navigator().showDashboard()
        );
    }

    private static void syncSessionUser(User sessionUser, User updatedUser) {
        if (sessionUser == null || updatedUser == null || sessionUser.getId() == null || updatedUser.getId() == null) {
            return;
        }
        if (!sessionUser.getId().equals(updatedUser.getId())) {
            return;
        }
        sessionUser.setUsername(updatedUser.getUsername());
        sessionUser.setFullName(updatedUser.getFullName());
        sessionUser.setRole(updatedUser.getRole());
        sessionUser.setEnabled(updatedUser.isEnabled());
        sessionUser.setPassword(updatedUser.getPassword());
    }
}
