package com.pbl3.project.pbl3_project.ui.dialog;

import com.pbl3.project.pbl3_project.entity.Expense;
import com.pbl3.project.pbl3_project.entity.ExpenseCategory;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.ExpenseService;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.ToastService;
import com.pbl3.project.pbl3_project.service.ValidationException;
import com.pbl3.project.pbl3_project.ui.component.DialogFormFactory;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.FxFormatters;
import java.math.BigDecimal;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class ExpenseDialog {

    private ExpenseDialog() {
    }

    public record Context(
        ExpenseService expenseService,
        ToastService toastService,
        Consumer<Throwable> errorHandler,
        Consumer<DatePicker> datePickerCustomizer
    ) {
    }

    public static void show(Stage owner, User user, Expense expense, Runnable onSave, Context context) {
        try {
            boolean editing = expense != null && expense.getId() != null;
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.setTitle(editing ? "Edit Expense" : "New Expense");
            dialog.initModality(Modality.WINDOW_MODAL);

            VBox root = new VBox(16);
            root.getStyleClass().addAll("dialog-root", "product-dialog-root", "expense-dialog-root");
            root.setPadding(new Insets(22, 24, 20, 24));

            DatePicker spentOnPicker = new DatePicker(
                expense != null && expense.getSpentOn() != null ? expense.getSpentOn() : java.time.LocalDate.now()
            );
            if (context.datePickerCustomizer() != null) {
                context.datePickerCustomizer().accept(spentOnPicker);
            }
            spentOnPicker.getStyleClass().add("promotion-dialog-date-picker");
            spentOnPicker.setMaxWidth(Double.MAX_VALUE);

            ComboBox<ExpenseCategory> categoryCombo = new ComboBox<>();
            categoryCombo.getItems().addAll(ExpenseCategory.values());
            categoryCombo.setValue(expense != null ? expense.getCategory() : null);
            categoryCombo.getStyleClass().addAll("product-dialog-combo-box", "promotion-dialog-combo-box");
            categoryCombo.setMaxWidth(Double.MAX_VALUE);
            installEnumCells(categoryCombo);

            TextField titleField = new TextField(expense != null && expense.getTitle() != null ? expense.getTitle() : "");
            titleField.setPromptText("Title");
            titleField.getStyleClass().add("product-dialog-input");

            TextField amountField = new TextField(
                expense != null && expense.getAmount() != null ? expense.getAmount().toPlainString() : ""
            );
            amountField.setPromptText("0.00");
            amountField.getStyleClass().add("product-dialog-input");
            amountField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,12}(\\.\\d{0,2})?") ? change : null
            ));

            ComboBox<PaymentMethod> methodCombo = new ComboBox<>();
            methodCombo.getItems().addAll(PaymentMethod.values());
            methodCombo.setValue(
                expense != null && expense.getPaymentMethod() != null
                    ? expense.getPaymentMethod()
                    : PaymentMethod.CASH
            );
            methodCombo.getStyleClass().addAll("product-dialog-combo-box", "promotion-dialog-combo-box");
            methodCombo.setMaxWidth(Double.MAX_VALUE);
            installEnumCells(methodCombo);

            TextArea noteArea = new TextArea(expense != null && expense.getNote() != null ? expense.getNote() : "");
            noteArea.setPromptText("Optional note");
            noteArea.setWrapText(true);
            noteArea.setPrefRowCount(3);
            noteArea.getStyleClass().add("product-dialog-text-area");

            Label titleError = DialogFormFactory.errorLabel();
            Label categoryError = DialogFormFactory.errorLabel();
            Label spentOnError = DialogFormFactory.errorLabel();
            Label amountError = DialogFormFactory.errorLabel();

            javafx.scene.layout.GridPane basicsGrid = DialogFormFactory.grid();
            VBox titleBlock = DialogFormFactory.fieldBlock("Title *", titleField, titleError);
            basicsGrid.add(titleBlock, 0, 0);
            javafx.scene.layout.GridPane.setColumnSpan(titleBlock, 2);
            basicsGrid.add(DialogFormFactory.fieldBlock("Category *", categoryCombo, categoryError), 0, 1);
            basicsGrid.add(DialogFormFactory.fieldBlock("Spent On *", spentOnPicker, spentOnError), 1, 1);

            javafx.scene.layout.GridPane paymentGrid = DialogFormFactory.grid();
            paymentGrid.add(DialogFormFactory.fieldBlock("Amount *", amountField, amountError), 0, 0);
            paymentGrid.add(DialogFormFactory.fieldBlock("Payment Method", methodCombo, null), 1, 0);

            VBox noteSectionContent = new VBox(0, DialogFormFactory.fieldBlock("Note", noteArea, null));
            noteSectionContent.setFillWidth(true);

            VBox formContent = new VBox(
                14,
                DialogFormFactory.section("Basics", basicsGrid),
                DialogFormFactory.section("Payment", paymentGrid),
                DialogFormFactory.section("Notes", noteSectionContent)
            );
            formContent.setFillWidth(true);

            ScrollPane formScroll = new ScrollPane(formContent);
            formScroll.setFitToWidth(true);
            formScroll.getStyleClass().addAll("product-dialog-scroll", "promotion-dialog-scroll");
            VBox.setVgrow(formScroll, Priority.ALWAYS);

            Button cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().addAll("button", "product-dialog-secondary-button", "dialog-cancel-button");
            cancelButton.setOnAction(e -> dialog.close());

            Button saveButton = new Button(editing ? "Save" : "Create");
            saveButton.getStyleClass().addAll("button", "primary-button", "product-dialog-primary-button");
            saveButton.setDefaultButton(true);
            saveButton.setOnAction(e -> saveExpense(
                dialog,
                saveButton,
                cancelButton,
                user,
                expense,
                editing,
                spentOnPicker,
                categoryCombo,
                titleField,
                amountField,
                methodCombo,
                noteArea,
                titleError,
                categoryError,
                spentOnError,
                amountError,
                onSave,
                context
            ));

            HBox actionRow = new HBox(10, cancelButton, saveButton);
            actionRow.getStyleClass().add("product-dialog-footer");
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(
                DialogFormFactory.header(editing ? "Edit Expense" : "New Expense", null),
                formScroll,
                actionRow
            );

            Scene scene = new Scene(root, 640, 740);
            applyApplicationStyles(scene);
            dialog.setScene(scene);
            DialogSupport.preventInitialFieldFocus(dialog, root);
            dialog.setMinWidth(640);
            dialog.setMinHeight(740);
            dialog.showAndWait();
        } catch (Exception ex) {
            handleError(context, ex);
        }
    }

    private static void saveExpense(
        Stage dialog,
        Button saveButton,
        Button cancelButton,
        User user,
        Expense expense,
        boolean editing,
        DatePicker spentOnPicker,
        ComboBox<ExpenseCategory> categoryCombo,
        TextField titleField,
        TextField amountField,
        ComboBox<PaymentMethod> methodCombo,
        TextArea noteArea,
        Label titleError,
        Label categoryError,
        Label spentOnError,
        Label amountError,
        Runnable onSave,
        Context context
    ) {
        try {
            clearErrors(titleError, categoryError, spentOnError, amountError);
            boolean valid = true;
            if (titleField.getText() == null || titleField.getText().trim().isBlank()) {
                DialogFormFactory.setError(titleError, "Title is required");
                valid = false;
            }
            if (categoryCombo.getValue() == null) {
                DialogFormFactory.setError(categoryError, "Category is required");
                valid = false;
            }
            if (spentOnPicker.getValue() == null) {
                DialogFormFactory.setError(spentOnError, "Date is required");
                valid = false;
            }

            BigDecimal parsedAmount = null;
            try {
                parsedAmount = parseMoneyInput(amountField.getText(), "Expense amount");
                if (parsedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    DialogFormFactory.setError(amountError, "Amount must be greater than 0");
                    valid = false;
                }
            } catch (ValidationException validationException) {
                DialogFormFactory.setError(amountError, validationException.getMessage());
                valid = false;
            }
            if (!valid) {
                return;
            }

            java.time.LocalDate spentOn = spentOnPicker.getValue();
            ExpenseCategory category = categoryCombo.getValue();
            String title = titleField.getText().trim();
            BigDecimal amount = parsedAmount;
            PaymentMethod paymentMethod = methodCombo.getValue();
            String note = noteArea.getText();
            AsyncUiTask.runButtonTask(
                saveButton,
                cancelButton,
                editing ? "Saving..." : "Creating...",
                () -> {
                    if (editing) {
                        return context.expenseService().updateExpense(
                            user,
                            expense.getId(),
                            spentOn,
                            category,
                            title,
                            amount,
                            paymentMethod,
                            note
                        );
                    }
                    return context.expenseService().createExpense(
                        user,
                        spentOn,
                        category,
                        title,
                        amount,
                        paymentMethod,
                        note
                    );
                },
                saved -> {
                    context.toastService().showSuccess(editing ? "Expense updated." : "Expense created.");
                    if (onSave != null) {
                        onSave.run();
                    }
                    dialog.close();
                },
                ex -> handleError(context, ex),
                "expense-save"
            );
        } catch (Exception ex) {
            handleError(context, ex);
        }
    }

    private static void clearErrors(Label... labels) {
        for (Label label : labels) {
            DialogFormFactory.setError(label, null);
        }
    }

    private static <T extends Enum<T>> void installEnumCells(ComboBox<T> comboBox) {
        comboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(T value) {
                return FxFormatters.enumText(value);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        });
        comboBox.setButtonCell(enumListCell());
        comboBox.setCellFactory(list -> enumListCell());
    }

    private static <T extends Enum<T>> ListCell<T> enumListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : FxFormatters.enumText(item));
            }
        };
    }

    private static BigDecimal parseMoneyInput(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return MoneySupport.normalize(new BigDecimal(value.replace(",", "").trim()));
        } catch (NumberFormatException ex) {
            throw new ValidationException(java.text.MessageFormat.format("{0} must be a valid number", fieldLabel));
        }
    }

    private static void applyApplicationStyles(Scene scene) {
        String stylesheet = ExpenseDialog.class.getResource("/application.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    private static void handleError(Context context, Throwable throwable) {
        if (context.errorHandler() != null) {
            context.errorHandler().accept(throwable);
        } else {
            context.toastService().showError(throwable.getMessage() != null ? throwable.getMessage() : "Operation failed");
        }
    }
}
