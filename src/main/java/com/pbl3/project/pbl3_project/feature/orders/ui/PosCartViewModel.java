package com.pbl3.project.pbl3_project.feature.orders.ui;

import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.entity.Customer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class PosCartViewModel {
    private final int number;
    private final ObservableList<CreateOrderRequest.OrderItemRequest> items = FXCollections.observableArrayList();
    private Customer customer;

    public PosCartViewModel(int number) {
        this.number = number;
    }

    public String title() {
        return "Order " + number;
    }

    public ObservableList<CreateOrderRequest.OrderItemRequest> items() {
        return items;
    }

    public Customer customer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public boolean hasUnsavedCart() {
        return !items.isEmpty() || customer != null;
    }

    public int totalItems() {
        int total = 0;
        for (CreateOrderRequest.OrderItemRequest item : items) {
            if (item != null) {
                total += Math.max(0, item.getQuantity());
            }
        }
        return total;
    }
}
