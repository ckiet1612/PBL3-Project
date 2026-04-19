package com.pbl3.project.pbl3_project.dto.report;

import com.pbl3.project.pbl3_project.entity.ExpenseCategory;

import java.math.BigDecimal;

public record ExpenseCategorySummaryRow(
    ExpenseCategory category,
    BigDecimal totalAmount,
    long entryCount
) {
}
