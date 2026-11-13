package com.cc.brokerage.domain.model;

import com.cc.brokerage.domain.InvalidRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyMath {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private static final BigDecimal MAX_MONEY = new BigDecimal("99999999.99");
    private static final BigDecimal MAX_SHARES = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final String NOT_POSITIVE = "El importe debe ser mayor que cero";
    private static final String ABOVE_MAXIMUM = "El importe supera el máximo permitido";
    private static final String SHARES_OUT_OF_RANGE = "La cantidad debe ser un entero de 1 a 2147483647";

    private MoneyMath() {
    }

    public static BigDecimal scaled(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal rounded = round(value);
        if (rounded.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException(NOT_POSITIVE);
        }
        if (rounded.compareTo(MAX_MONEY) > 0) {
            throw new InvalidRequestException(ABOVE_MAXIMUM);
        }
        return rounded;
    }

    public static BigDecimal notional(int size, BigDecimal price) {
        return round(price.multiply(BigDecimal.valueOf(size)));
    }

    public static int sharesFromAmount(BigDecimal amount, BigDecimal price) {
        BigDecimal shares = amount.divideToIntegralValue(price);
        if (shares.compareTo(MAX_SHARES) > 0) {
            throw new InvalidRequestException(SHARES_OUT_OF_RANGE);
        }
        return shares.intValueExact();
    }

    public static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, SCALE, ROUNDING);
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }
}
