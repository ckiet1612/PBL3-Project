package com.pbl3.project.pbl3_project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneySupport {

    public static final int MONEY_SCALE = 2;
    public static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);

    private MoneySupport() {
    }

    public static BigDecimal normalize(BigDecimal value) {
        return value == null ? ZERO : value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    public static BigDecimal normalize(Double value) {
        return value == null ? ZERO : BigDecimal.valueOf(value).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    public static BigDecimal fromInt(int value) {
        return BigDecimal.valueOf(value).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    public static BigDecimal add(BigDecimal left, BigDecimal right) {
        return normalize(left).add(normalize(right)).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    public static BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return normalize(left).subtract(normalize(right)).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    public static BigDecimal multiply(BigDecimal left, int right) {
        return normalize(left).multiply(BigDecimal.valueOf(right)).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    public static BigDecimal multiply(BigDecimal left, long right) {
        return normalize(left).multiply(BigDecimal.valueOf(right)).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    public static BigDecimal divide(BigDecimal left, int right) {
        if (right == 0) {
            return ZERO;
        }
        return normalize(left).divide(BigDecimal.valueOf(right), MONEY_SCALE, MONEY_ROUNDING);
    }

    public static boolean differs(BigDecimal left, BigDecimal right) {
        return normalize(left).compareTo(normalize(right)) != 0;
    }

    public static boolean isPositive(BigDecimal value) {
        return normalize(value).compareTo(ZERO) > 0;
    }

    public static boolean isZero(BigDecimal value) {
        return normalize(value).compareTo(ZERO) == 0;
    }

    public static BigDecimal max(BigDecimal left, BigDecimal right) {
        return normalize(left).max(normalize(right));
    }
}
