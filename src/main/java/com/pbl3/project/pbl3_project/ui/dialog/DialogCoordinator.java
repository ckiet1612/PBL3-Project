package com.pbl3.project.pbl3_project.ui.dialog;

import com.pbl3.project.pbl3_project.entity.Expense;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.Promotion;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.ExpenseService;
import com.pbl3.project.pbl3_project.service.OrderService;
import com.pbl3.project.pbl3_project.service.ProductService;
import com.pbl3.project.pbl3_project.service.PromotionService;
import com.pbl3.project.pbl3_project.service.ReceiptService;
import com.pbl3.project.pbl3_project.service.ToastService;
import java.util.function.Consumer;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;

public final class DialogCoordinator {

    private final ExpenseService expenseService;
    private final PromotionService promotionService;
    private final ProductService productService;
    private final OrderService orderService;
    private final ReceiptService receiptService;
    private final ToastService toastService;
    private final Consumer<Throwable> errorHandler;
    private final Consumer<DatePicker> datePickerCustomizer;

    public DialogCoordinator(
        ExpenseService expenseService,
        PromotionService promotionService,
        ProductService productService,
        OrderService orderService,
        ReceiptService receiptService,
        ToastService toastService,
        Consumer<Throwable> errorHandler,
        Consumer<DatePicker> datePickerCustomizer
    ) {
        this.expenseService = expenseService;
        this.promotionService = promotionService;
        this.productService = productService;
        this.orderService = orderService;
        this.receiptService = receiptService;
        this.toastService = toastService;
        this.errorHandler = errorHandler;
        this.datePickerCustomizer = datePickerCustomizer;
    }

    public void showExpense(Stage owner, User user, Expense expense, Runnable onSuccess) {
        ExpenseDialog.show(owner, user, expense, onSuccess, new ExpenseDialog.Context(
            expenseService,
            toastService,
            errorHandler,
            datePickerCustomizer
        ));
    }

    public void showPromotion(Stage owner, User user, Promotion promotion, Runnable onSuccess) {
        PromotionDialog.show(owner, user, promotion, onSuccess, new PromotionDialog.Context(
            productService,
            promotionService,
            toastService,
            errorHandler,
            datePickerCustomizer
        ));
    }

    public void showOrderDetails(Stage owner, Order order, User user, Runnable onChanged) {
        OrderDialog.showDetails(owner, order, user, onChanged, new OrderDialog.Context(
            orderService,
            receiptService,
            toastService,
            errorHandler
        ));
    }
}
