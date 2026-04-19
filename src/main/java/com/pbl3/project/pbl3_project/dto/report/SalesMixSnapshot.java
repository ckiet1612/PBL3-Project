package com.pbl3.project.pbl3_project.dto.report;

import com.pbl3.project.pbl3_project.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record SalesMixSnapshot(
    LocalDate startDate,
    LocalDate endDate,
    Map<String, BigDecimal> revenueSeries,
    Map<String, Long> orderSeries,
    Map<String, Long> canceledOrderSeries,
    Map<PaymentMethod, Long> paymentMethodShare,
    List<TopSellingProductRow> topSellingProducts
) {
}
