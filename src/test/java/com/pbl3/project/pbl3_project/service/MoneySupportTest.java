package com.pbl3.project.pbl3_project.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneySupportTest {

    @Test
    void normalizeRoundsToTwoDecimalsHalfUp() {
        assertEquals(new BigDecimal("12.35"), MoneySupport.normalize(new BigDecimal("12.345")));
        assertEquals(new BigDecimal("12.34"), MoneySupport.normalize(new BigDecimal("12.344")));
    }

    @Test
    void multiplyAndDivideKeepMoneyScale() {
        assertEquals(new BigDecimal("59.97"), MoneySupport.multiply(new BigDecimal("19.99"), 3));
        assertEquals(new BigDecimal("6.67"), MoneySupport.divide(new BigDecimal("20.00"), 3));
    }
}
