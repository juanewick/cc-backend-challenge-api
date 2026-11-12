package com.cc.brokerage.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyMath {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private MoneyMath() {
    }

    public static BigDecimal scaled(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal notional(int size, BigDecimal price) {
        return scaled(price.multiply(BigDecimal.valueOf(size)));
    }

    public static int sharesFromAmount(BigDecimal amount, BigDecimal price) {
        return amount.divideToIntegralValue(price).intValue();
    }

    public static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, SCALE, ROUNDING);
    }
}
