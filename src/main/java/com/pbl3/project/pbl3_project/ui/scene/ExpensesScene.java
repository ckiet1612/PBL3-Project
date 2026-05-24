package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.ui.component.ActionTaskbarFactory;
import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.component.ExpandableSearchControl;
import com.pbl3.project.pbl3_project.ui.component.FilterControlFactory;
import com.pbl3.project.pbl3_project.ui.component.FilterDisclosureSection;
import com.pbl3.project.pbl3_project.ui.component.RangeSlider;
import com.pbl3.project.pbl3_project.ui.util.AsyncPageCache;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.SortCriterion;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class ExpensesScene {
    public record Options(LocalDate startDate, LocalDate endDate) {
    }

    private record ExpenseFilterOptions(
        java.util.List<com.pbl3.project.pbl3_project.dto.IdLabelOption> creatorOptions,
        BigDecimal maxAmount
    ) {
    }

    private record ExpensePageResult(
        org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Expense> page,
        int pageIndex
    ) {
    }

    private ExpensesScene() {
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        Stage stage = context.owner();
        LocalDate initialStartDate = options == null ? null : options.startDate();
        LocalDate initialEndDate = options == null ? null : options.endDate();
        SceneUiSupport support = context.support();

        final String expenseSortStateKey = "expenses";
        TableSortState expenseSortState = support.getOrCreateTableSortState(
            expenseSortStateKey,
            new SortCriterion("spentOn", javafx.scene.control.TableColumn.SortType.DESCENDING),
            new SortCriterion("id", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> expenseSortProperties = new java.util.LinkedHashMap<>();
        expenseSortProperties.put("id", "id");
        expenseSortProperties.put("spentOn", "spentOn");
        expenseSortProperties.put("category", "category");
        expenseSortProperties.put("title", "title");
        expenseSortProperties.put("amount", "amount");
        expenseSortProperties.put("paymentMethod", "paymentMethod");
        expenseSortProperties.put("createdBy", "createdBy.fullName");

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Expense> table = new javafx.scene.control.TableView<>();
        support.applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        final int expensePageSize = 20;
        final int[] expenseCurrentPage = {0};
        final int[] expenseTotalPages = {0};
        final long[] expenseTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> expenseSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> expenseStartDateRef = new java.util.concurrent.atomic.AtomicReference<>(initialStartDate);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> expenseEndDateRef = new java.util.concurrent.atomic.AtomicReference<>(initialEndDate);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.ExpenseCategory>> expenseCategoriesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod>> expenseMethodsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<Long>> expenseCreatorsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<BigDecimal> expenseMinAmountRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<BigDecimal> expenseMaxAmountRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<ExpenseFilterOptions> expenseFilterOptionsCache = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicLong expensePageLoadVersion = new java.util.concurrent.atomic.AtomicLong();
        AsyncPageCache<ExpensePageResult> expensePageCache = new AsyncPageCache<>(80);

        Label expenseRowCountLabel = support.createStatusMetaLabel(java.text.MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label expensePageLabel = support.createStatusMetaLabel(java.text.MessageFormat.format("Page {0} / {1}", 0, 0));
        Button expensePrevBtn = ButtonFactory.pageNav("Prev");
        Button expenseNextBtn = ButtonFactory.pageNav("Next");

        Runnable[] refreshExpenseTableRef = new Runnable[1];
        Runnable updateExpenseStatusBar = () -> support.updatePagedStatus(
            table,
            expenseRowCountLabel,
            expensePageLabel,
            expensePrevBtn,
            expenseNextBtn,
            expenseTotalElements[0],
            expenseCurrentPage[0],
            expenseTotalPages[0],
            expensePageSize
        );
        Runnable loadExpensePage = () -> {
            int requestedPage = expenseCurrentPage[0];
            String search = expenseSearchRef.get();
            java.time.LocalDate startDate = expenseStartDateRef.get();
            java.time.LocalDate endDate = expenseEndDateRef.get();
            java.util.Set<com.pbl3.project.pbl3_project.entity.ExpenseCategory> categories =
                new java.util.LinkedHashSet<>(expenseCategoriesRef.get());
            java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> methods =
                new java.util.LinkedHashSet<>(expenseMethodsRef.get());
            java.util.Set<Long> creators = new java.util.LinkedHashSet<>(expenseCreatorsRef.get());
            BigDecimal minAmount = expenseMinAmountRef.get();
            BigDecimal maxAmount = expenseMaxAmountRef.get();
            java.util.List<SortCriterion> sortSnapshot = expenseSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(
                pageIndex,
                search,
                startDate,
                endDate,
                categories,
                methods,
                creators,
                minAmount,
                maxAmount,
                sortSnapshot
            );
            java.util.function.IntFunction<ExpensePageResult> fetchExpensePage = pageIndex -> {
                int resolvedPage = pageIndex;
                org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Expense> pageData = context.expenseService().searchExpenses(
                    user,
                    search,
                    startDate,
                    endDate,
                    categories,
                    methods,
                    creators,
                    minAmount,
                    maxAmount,
                    support.createPageable(sortForLoad, expenseSortProperties, resolvedPage, expensePageSize)
                );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = context.expenseService().searchExpenses(
                        user,
                        search,
                        startDate,
                        endDate,
                        categories,
                        methods,
                        creators,
                        minAmount,
                        maxAmount,
                        support.createPageable(sortForLoad, expenseSortProperties, resolvedPage, expensePageSize)
                    );
                }
                return new ExpensePageResult(pageData, resolvedPage);
            };

            AsyncUiTask.runLatestCachedTableLoad(
                table,
                expensePrevBtn,
                expenseNextBtn,
                expensePageLoadVersion,
                expensePageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchExpensePage.apply(requestedPage),
                result -> {
                    org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Expense> pageData = result.page();
                    expenseCurrentPage[0] = result.pageIndex();
                    expenseTotalElements[0] = pageData.getTotalElements();
                    expenseTotalPages[0] = pageData.getTotalPages();
                    table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
                    updateExpenseStatusBar.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < expenseTotalPages[0]) {
                        expensePageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchExpensePage.apply(nextPage),
                            null,
                            "expenses-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        expensePageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchExpensePage.apply(previousPage),
                            null,
                            "expenses-prev-page-prefetch"
                        );
                    }
                },
                context::showUserFacingError,
                "Loading expenses...",
                "Could not load expenses",
                "expenses-page-loader"
            );
        };
        refreshExpenseTableRef[0] = loadExpensePage;
        Runnable refreshExpenseAfterCrud = () -> {
            expenseFilterOptionsCache.set(null);
            expensePageCache.clear();
            loadExpensePage.run();
        };

        expensePrevBtn.setOnAction(e -> {
            if (expenseCurrentPage[0] > 0) {
                expenseCurrentPage[0]--;
                loadExpensePage.run();
            }
        });
        expenseNextBtn.setOnAction(e -> {
            if (expenseCurrentPage[0] + 1 < expenseTotalPages[0]) {
                expenseCurrentPage[0]++;
                loadExpensePage.run();
            }
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, Long> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        idCol.setPrefWidth(110);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, String> spentOnCol = new javafx.scene.control.TableColumn<>("Spent On");
        spentOnCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(support.formatDate(cell.getValue().getSpentOn())));
        spentOnCol.setPrefWidth(125);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, String> categoryCol = new javafx.scene.control.TableColumn<>("Category");
        categoryCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(support.formatExpenseCategoryLabel(cell.getValue().getCategory())));
        categoryCol.setPrefWidth(145);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, String> titleCol = new javafx.scene.control.TableColumn<>("Title");
        titleCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(220);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, String> amountCol = new javafx.scene.control.TableColumn<>("Amount");
        amountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(support.formatVnd(cell.getValue().getAmount())));
        amountCol.setPrefWidth(140);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, String> methodCol = new javafx.scene.control.TableColumn<>("Payment Method");
        methodCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(support.formatPaymentMethodLabel(cell.getValue().getPaymentMethod())));
        methodCol.setPrefWidth(150);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, String> createdByCol = new javafx.scene.control.TableColumn<>("Created By");
        createdByCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCreatedByDisplayName()));
        createdByCol.setPrefWidth(170);

        table.getColumns().addAll(idCol, spentOnCol, categoryCol, titleCol, amountCol, methodCol, createdByCol);

        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, ?>> expenseSortColumns =
            new java.util.LinkedHashMap<>();
        expenseSortColumns.put("id", idCol);
        expenseSortColumns.put("spentOn", spentOnCol);
        expenseSortColumns.put("category", categoryCol);
        expenseSortColumns.put("title", titleCol);
        expenseSortColumns.put("amount", amountCol);
        expenseSortColumns.put("paymentMethod", methodCol);
        expenseSortColumns.put("createdBy", createdByCol);
        support.installSortHeaderIndicators(expenseSortColumns);

        java.util.LinkedHashMap<String, String> expenseSortLabels = new java.util.LinkedHashMap<>();
        expenseSortLabels.put("id", "ID");
        expenseSortLabels.put("spentOn", "Spent On");
        expenseSortLabels.put("category", "Category");
        expenseSortLabels.put("title", "Title");
        expenseSortLabels.put("amount", "Amount");
        expenseSortLabels.put("paymentMethod", "Payment Method");
        expenseSortLabels.put("createdBy", "Created By");
        Label expenseSortStatusLabel = support.createSortStatusLabel(expenseSortState, expenseSortLabels);
        Runnable applyExpenseSortUi = () -> {
            support.applySortStateToTable(table, expenseSortColumns, expenseSortState);
            expenseSortStatusLabel.setText(support.buildSortStatusText(expenseSortState, expenseSortLabels));
        };
        applyExpenseSortUi.run();
        support.installManualServerSorting(
            table,
            expenseSortColumns,
            expenseSortState,
            () -> {
                applyExpenseSortUi.run();
                expenseCurrentPage[0] = 0;
                loadExpensePage.run();
            }
        );

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Expense> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    support.showExpenseDialog(stage, user, row.getItem(), refreshExpenseTableRef[0]);
                }
            });
            return row;
        });

        ExpandableSearchControl expenseSearchControl = ExpandableSearchControl.create(250, Color.web("#1d7df2"));
        javafx.animation.PauseTransition expenseSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        expenseSearchPause.setOnFinished(e -> {
            expenseCurrentPage[0] = 0;
            expenseSearchRef.set(expenseSearchControl.field().getText());
            loadExpensePage.run();
        });
        expenseSearchControl.field().textProperty().addListener((obs, oldV, newV) -> expenseSearchPause.playFromStart());

        javafx.scene.layout.HBox expenseFilterBox = new javafx.scene.layout.HBox();
        expenseFilterBox.setAlignment(Pos.CENTER);
        expenseFilterBox.getStyleClass().add("expandable-search-box");
        expenseFilterBox.setPrefSize(40, 40);
        expenseFilterBox.setMinSize(40, 40);
        expenseFilterBox.setMaxSize(40, 40);
        expenseFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath expenseFilterIcon = new javafx.scene.shape.SVGPath();
        expenseFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        expenseFilterIcon.setFill(Color.web("#1d7df2"));
        expenseFilterBox.getChildren().add(expenseFilterIcon);
        javafx.scene.control.Tooltip.install(expenseFilterBox, new javafx.scene.control.Tooltip("Filter"));

        Runnable updateExpenseFilterAccent = () -> {
            boolean hasFilter = expenseStartDateRef.get() != null
                || expenseEndDateRef.get() != null
                || !expenseCategoriesRef.get().isEmpty()
                || !expenseMethodsRef.get().isEmpty()
                || !expenseCreatorsRef.get().isEmpty()
                || expenseMinAmountRef.get() != null
                || expenseMaxAmountRef.get() != null;
            expenseFilterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
        };

        javafx.stage.Popup expenseFilterPopup = new javafx.stage.Popup();
        expenseFilterPopup.setAutoHide(true);
        expenseFilterBox.setOnMouseClicked(fev -> {
            fev.consume();
            try {
                if (expenseFilterPopup.isShowing()) {
                    expenseFilterPopup.hide();
                    return;
                }

                java.util.function.Consumer<ExpenseFilterOptions> showFilterContent = filterOptions -> {
                FilterControlFactory.Shell shell = FilterControlFactory.shell(360, 360);
                VBox scrollContent = shell.content();

                Label dateTitle = FilterControlFactory.sectionTitle("Date Range");
                javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(expenseStartDateRef.get());
                startDatePicker.setPromptText("Start Date");
                startDatePicker.setPrefWidth(140);
                javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(expenseEndDateRef.get());
                endDatePicker.setPromptText("End Date");
                endDatePicker.setPrefWidth(140);
                support.customizeDatePicker(startDatePicker);
                support.customizeDatePicker(endDatePicker);
                javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
                dateBox.setAlignment(Pos.CENTER_LEFT);

                javafx.scene.control.Separator sepDate = new javafx.scene.control.Separator();

                Label categoryTitle = FilterControlFactory.sectionTitle("Category");
                javafx.scene.control.CheckBox allCategoriesCb = new javafx.scene.control.CheckBox("All Categories");
                allCategoriesCb.setSelected(expenseCategoriesRef.get().isEmpty());
                allCategoriesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                VBox categoryScroll = new VBox(8);
                categoryScroll.setPadding(new Insets(5, 5, 5, 20));
                java.util.List<javafx.scene.control.CheckBox> categoryCbs = new java.util.ArrayList<>();
                java.util.Set<com.pbl3.project.pbl3_project.entity.ExpenseCategory> activeCategories = expenseCategoriesRef.get();
                for (com.pbl3.project.pbl3_project.entity.ExpenseCategory category : com.pbl3.project.pbl3_project.entity.ExpenseCategory.values()) {
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(support.formatExpenseCategoryLabel(category));
                    cb.setUserData(category);
                    cb.setSelected(activeCategories.isEmpty() || activeCategories.contains(category));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allCategoriesCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox child : categoryCbs) {
                                if (!child.isSelected()) {
                                    all = false;
                                    break;
                                }
                            }
                            allCategoriesCb.setSelected(all);
                        }
                    });
                    categoryCbs.add(cb);
                    categoryScroll.getChildren().add(cb);
                }
                allCategoriesCb.setOnAction(e -> {
                    boolean selected = allCategoriesCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : categoryCbs) {
                        cb.setSelected(selected);
                    }
                });
                FilterDisclosureSection categorySection = new FilterDisclosureSection(allCategoriesCb, categoryScroll);

                javafx.scene.control.Separator sepCategory = new javafx.scene.control.Separator();

                Label methodTitle = FilterControlFactory.sectionTitle("Payment Method");
                javafx.scene.control.CheckBox allMethodsCb = new javafx.scene.control.CheckBox("All Methods");
                allMethodsCb.setSelected(expenseMethodsRef.get().isEmpty());
                allMethodsCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                VBox methodScroll = new VBox(8);
                methodScroll.setPadding(new Insets(5, 5, 5, 20));
                java.util.List<javafx.scene.control.CheckBox> methodCbs = new java.util.ArrayList<>();
                java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> activeMethods = expenseMethodsRef.get();
                for (com.pbl3.project.pbl3_project.entity.PaymentMethod method : com.pbl3.project.pbl3_project.entity.PaymentMethod.values()) {
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(support.formatPaymentMethodLabel(method));
                    cb.setUserData(method);
                    cb.setSelected(activeMethods.isEmpty() || activeMethods.contains(method));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allMethodsCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox child : methodCbs) {
                                if (!child.isSelected()) {
                                    all = false;
                                    break;
                                }
                            }
                            allMethodsCb.setSelected(all);
                        }
                    });
                    methodCbs.add(cb);
                    methodScroll.getChildren().add(cb);
                }
                allMethodsCb.setOnAction(e -> {
                    boolean selected = allMethodsCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : methodCbs) {
                        cb.setSelected(selected);
                    }
                });
                FilterDisclosureSection methodSection = new FilterDisclosureSection(allMethodsCb, methodScroll);

                javafx.scene.control.Separator sepMethod = new javafx.scene.control.Separator();

                Label creatorTitle = FilterControlFactory.sectionTitle("Created By");
                javafx.scene.control.CheckBox allCreatorsCb = new javafx.scene.control.CheckBox("All Creators");
                allCreatorsCb.setSelected(expenseCreatorsRef.get().isEmpty());
                allCreatorsCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                VBox creatorScroll = new VBox(8);
                creatorScroll.setPadding(new Insets(5, 5, 5, 20));
                java.util.List<javafx.scene.control.CheckBox> creatorCbs = new java.util.ArrayList<>();
                java.util.Set<Long> activeCreators = expenseCreatorsRef.get();
                for (com.pbl3.project.pbl3_project.dto.IdLabelOption option : filterOptions.creatorOptions()) {
                    if (option.label() == null || option.label().isBlank()) {
                        continue;
                    }
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(option.label());
                    cb.setUserData(option.id());
                    cb.setSelected(activeCreators.isEmpty() || activeCreators.contains(option.id()));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allCreatorsCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox child : creatorCbs) {
                                if (!child.isSelected()) {
                                    all = false;
                                    break;
                                }
                            }
                            allCreatorsCb.setSelected(all);
                        }
                    });
                    creatorCbs.add(cb);
                    creatorScroll.getChildren().add(cb);
                }
                allCreatorsCb.setOnAction(e -> {
                    boolean selected = allCreatorsCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : creatorCbs) {
                        cb.setSelected(selected);
                    }
                });
                FilterDisclosureSection creatorSection = new FilterDisclosureSection(allCreatorsCb, creatorScroll);

                javafx.scene.control.Separator sepCreator = new javafx.scene.control.Separator();

                Label amountTitle = FilterControlFactory.sectionTitle("Amount Range");
                BigDecimal maxAmountValue = filterOptions.maxAmount();
                double maxAmount = maxAmountValue == null ? 0.0 : maxAmountValue.doubleValue();
                if (maxAmount <= 0) {
                    maxAmount = 1_000_000;
                }
                double initialMinAmount = expenseMinAmountRef.get() == null ? 0.0 : expenseMinAmountRef.get().doubleValue();
                double initialMaxAmount = expenseMaxAmountRef.get() == null ? maxAmount : Math.min(maxAmount, expenseMaxAmountRef.get().doubleValue());
                Label amountLabel = new Label(
                    support.formatVnd(MoneySupport.normalize(BigDecimal.valueOf(initialMinAmount)))
                        + " - "
                        + support.formatVnd(MoneySupport.normalize(BigDecimal.valueOf(initialMaxAmount)))
                );
                amountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");
                RangeSlider amountSlider = new RangeSlider(0, maxAmount, initialMinAmount, initialMaxAmount, 280);
                amountSlider.minVal.addListener((o, ov, nv) -> amountLabel.setText(
                    support.formatVnd(MoneySupport.normalize(BigDecimal.valueOf(nv.doubleValue())))
                        + " - "
                        + support.formatVnd(MoneySupport.normalize(BigDecimal.valueOf(amountSlider.maxVal.get())))
                ));
                amountSlider.maxVal.addListener((o, ov, nv) -> amountLabel.setText(
                    support.formatVnd(MoneySupport.normalize(BigDecimal.valueOf(amountSlider.minVal.get())))
                        + " - "
                        + support.formatVnd(MoneySupport.normalize(BigDecimal.valueOf(nv.doubleValue())))
                ));

                scrollContent.getChildren().addAll(
                    dateTitle, dateBox, sepDate,
                    categoryTitle, categorySection.getNode(), sepCategory,
                    methodTitle, methodSection.getNode(), sepMethod,
                    creatorTitle, creatorSection.getNode(), sepCreator,
                    amountTitle, amountLabel, amountSlider
                );

                final double finalMaxAmount = maxAmount;
                Button resetBtn = new Button("Reset");
                resetBtn.getStyleClass().add("filter-reset-btn");
                resetBtn.setOnAction(ae -> {
                    expenseStartDateRef.set(null);
                    expenseEndDateRef.set(null);
                    expenseCategoriesRef.set(new java.util.LinkedHashSet<>());
                    expenseMethodsRef.set(new java.util.LinkedHashSet<>());
                    expenseCreatorsRef.set(new java.util.LinkedHashSet<>());
                    expenseMinAmountRef.set(null);
                    expenseMaxAmountRef.set(null);
                    expenseCurrentPage[0] = 0;
                    updateExpenseFilterAccent.run();
                    loadExpensePage.run();
                    expenseFilterPopup.hide();
                });

                Button applyBtn = new Button("Apply");
                applyBtn.getStyleClass().add("filter-apply-btn");
                applyBtn.setOnAction(ae -> {
                    java.util.Set<com.pbl3.project.pbl3_project.entity.ExpenseCategory> selectedCategories = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : categoryCbs) {
                        if (cb.isSelected()) {
                            selectedCategories.add((com.pbl3.project.pbl3_project.entity.ExpenseCategory) cb.getUserData());
                        }
                    }

                    java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> selectedMethods = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : methodCbs) {
                        if (cb.isSelected()) {
                            selectedMethods.add((com.pbl3.project.pbl3_project.entity.PaymentMethod) cb.getUserData());
                        }
                    }

                    java.util.Set<Long> selectedCreators = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : creatorCbs) {
                        if (cb.isSelected()) {
                            selectedCreators.add((Long) cb.getUserData());
                        }
                    }

                    double minAmount = amountSlider.minVal.get();
                    double maxAmountSelected = amountSlider.maxVal.get();
                    expenseStartDateRef.set(startDatePicker.getValue());
                    expenseEndDateRef.set(endDatePicker.getValue());
                    expenseCategoriesRef.set(allCategoriesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedCategories);
                    expenseMethodsRef.set(allMethodsCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedMethods);
                    expenseCreatorsRef.set(allCreatorsCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedCreators);
                    expenseMinAmountRef.set(minAmount <= 0 ? null : MoneySupport.normalize(BigDecimal.valueOf(minAmount)));
                    expenseMaxAmountRef.set(maxAmountSelected >= finalMaxAmount ? null : MoneySupport.normalize(BigDecimal.valueOf(maxAmountSelected)));
                    expenseCurrentPage[0] = 0;
                    updateExpenseFilterAccent.run();
                    loadExpensePage.run();
                    expenseFilterPopup.hide();
                });

                shell.container().getChildren().add(FilterControlFactory.actionRow(resetBtn, applyBtn));
                expenseFilterPopup.getContent().clear();
                expenseFilterPopup.getContent().add(shell.container());
                support.showPopupBelow(expenseFilterPopup, expenseFilterBox, -300, 5);
                };

                ExpenseFilterOptions cachedOptions = expenseFilterOptionsCache.get();
                if (cachedOptions != null) {
                    showFilterContent.accept(cachedOptions);
                    return;
                }

                expenseFilterPopup.getContent().setAll(FilterControlFactory.loadingContainer(360, "Loading filters..."));
                support.showPopupBelow(expenseFilterPopup, expenseFilterBox, -300, 5);

                javafx.concurrent.Task<ExpenseFilterOptions> task = new javafx.concurrent.Task<>() {
                    @Override
                    protected ExpenseFilterOptions call() {
                        return new ExpenseFilterOptions(
                            context.expenseService().getExpenseCreatorOptions(user),
                            context.expenseService().getExpenseMaxAmount(user)
                        );
                    }
                };
                task.setOnSucceeded(taskEvent -> {
                    ExpenseFilterOptions optionsValue = task.getValue();
                    expenseFilterOptionsCache.set(optionsValue);
                    if (expenseFilterPopup.isShowing()) {
                        showFilterContent.accept(optionsValue);
                    }
                });
                task.setOnFailed(taskEvent -> {
                    expenseFilterPopup.hide();
                    context.showUserFacingError(task.getException());
                });
                Thread worker = new Thread(task, "expense-filter-options-loader");
                worker.setDaemon(true);
                worker.start();
            } catch (Exception ex) {
                context.showUserFacingError(ex);
            }
        });

        Button newExpenseBtn = ButtonFactory.expandableGreenAction("New Expense", 180);
        newExpenseBtn.setOnAction(e -> support.showExpenseDialog(stage, user, null, refreshExpenseAfterCrud));

        Button editExpenseBtn = ActionTaskbarFactory.createButton(
            ActionTaskbarFactory.icon("edit"),
            "Edit Expense",
            "promotion-taskbar-button-edit"
        );
        editExpenseBtn.setDisable(true);
        editExpenseBtn.setOnAction(e -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Expense> selectedExpenses =
                new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedExpenses.size() != 1) {
                context.toastService().showWarning("Select exactly one expense to edit.");
                return;
            }
            support.showExpenseDialog(stage, user, selectedExpenses.get(0), refreshExpenseAfterCrud);
        });

        Button deleteExpenseBtn = ActionTaskbarFactory.createButton(
            ActionTaskbarFactory.icon("trash"),
            "Delete Expense",
            "promotion-taskbar-button-delete"
        );
        deleteExpenseBtn.setDisable(true);
        deleteExpenseBtn.setOnAction(e -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Expense> selectedExpenses =
                new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedExpenses.isEmpty()) {
                context.toastService().showWarning("Select an expense to delete.");
                return;
            }
            String confirmMessage = selectedExpenses.size() == 1
                ? java.text.MessageFormat.format("Delete expense \"{0}\"?", selectedExpenses.get(0).getTitle())
                : java.text.MessageFormat.format("Delete {0} selected expenses?", selectedExpenses.size());
            boolean confirmed = selectedExpenses.size() >= 5
                ? DialogSupport.showTypedDangerConfirm(
                    stage,
                    "Delete Expenses",
                    confirmMessage + " This is a large delete operation.",
                    "DELETE"
                )
                : support.showConfirmDialog("Delete Expenses", confirmMessage);
            if (!confirmed) {
                return;
            }
            AsyncUiTask.runButtonTask(
                deleteExpenseBtn,
                editExpenseBtn,
                null,
                () -> {
                    for (com.pbl3.project.pbl3_project.entity.Expense expense : selectedExpenses) {
                        context.expenseService().deleteExpense(user, expense.getId());
                    }
                    return selectedExpenses.size();
                },
                deletedCount -> {
                    context.toastService().showSuccess(deletedCount == 1 ? "Expense deleted." : deletedCount + " expenses deleted.");
                    refreshExpenseAfterCrud.run();
                },
                context::showUserFacingError,
                "expenses-delete"
            );
        });

        Runnable updateExpenseSelectionActions = () -> {
            int selectedCount = table.getSelectionModel().getSelectedItems().size();
            editExpenseBtn.setDisable(selectedCount != 1);
            deleteExpenseBtn.setDisable(selectedCount == 0);
            updateExpenseStatusBar.run();
        };
        table.getSelectionModel().getSelectedItems().addListener(
            (javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Expense>) change -> updateExpenseSelectionActions.run()
        );
        table.setOnKeyPressed(event -> {
            javafx.scene.input.KeyCode code = event.getCode();
            if (code == javafx.scene.input.KeyCode.DELETE || code == javafx.scene.input.KeyCode.BACK_SPACE) {
                if (!deleteExpenseBtn.isDisabled()) {
                    deleteExpenseBtn.fire();
                }
            } else if (code == javafx.scene.input.KeyCode.ENTER && !editExpenseBtn.isDisabled()) {
                editExpenseBtn.fire();
            }
        });

        HBox expenseActionTaskbar = ActionTaskbarFactory.create(editExpenseBtn, deleteExpenseBtn);

        javafx.scene.layout.BorderPane expenseToolbar = new javafx.scene.layout.BorderPane();
        javafx.scene.layout.HBox expenseRightBox = new javafx.scene.layout.HBox(
            15,
            expenseFilterBox,
            expenseSearchControl.box(),
            newExpenseBtn
        );
        expenseRightBox.setAlignment(Pos.CENTER_RIGHT);
        expenseToolbar.setLeft(expenseActionTaskbar);
        expenseToolbar.setRight(expenseRightBox);

        javafx.scene.layout.HBox expenseStatusBar = new javafx.scene.layout.HBox(
            15,
            expenseSortStatusLabel,
            expenseRowCountLabel,
            expensePageLabel,
            expensePrevBtn,
            expenseNextBtn
        );
        support.applyStandardTableStatusBar(expenseStatusBar);

        VBox content = new VBox();
        support.applyStandardTablePageLayout(content);
        content.getChildren().addAll(expenseToolbar, table, expenseStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        javafx.application.Platform.runLater(loadExpensePage);
        updateExpenseFilterAccent.run();
        TableViewSupport.enableDeselectOnOutsideClick(content, table);
        return content;
    
    }
}
