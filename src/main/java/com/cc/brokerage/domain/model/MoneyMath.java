package com.cc.brokerage.domain.model;

import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyMath {

    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private static volatile MoneyLimits limits = MoneyLimits.defaults();

    private MoneyMath() {
    }

    public static void configure(MoneyLimits next) {
        if (next == null) {
            throw new IllegalArgumentException("Money limits are required");
        }
        limits = next;
    }

    public static int scale() {
        return limits.scale();
    }

    public static BigDecimal minAmount() {
        return limits.minAmount();
    }

    public static BigDecimal maxAmount() {
        return limits.maxAmount();
    }

    public static long minShares() {
        return limits.minShares();
    }

    public static long maxShares() {
        return limits.maxShares();
    }

    public static String sharesRangeMessage() {
        return limits.sharesRangeMessage();
    }

    public static BigDecimal scaled(BigDecimal value) {
        if (value == null) {
            return null;
        }
        MoneyLimits current = limits;
        BigDecimal rounded = value.setScale(current.scale(), ROUNDING);
        if (rounded.compareTo(current.minAmount()) <= 0) {
            throw new DomainException(DomainErrorCodes.AMOUNT_NOT_POSITIVE);
        }
        if (rounded.compareTo(current.maxAmount()) > 0) {
            throw new DomainException(DomainErrorCodes.AMOUNT_ABOVE_MAXIMUM);
        }
        return rounded;
    }

    public static BigDecimal notional(int size, BigDecimal price) {
        return round(price.multiply(BigDecimal.valueOf(size)));
    }

    public static int sharesFromAmount(BigDecimal amount, BigDecimal price) {
        MoneyLimits current = limits;
        BigDecimal shares = amount.divideToIntegralValue(price);
        if (shares.compareTo(BigDecimal.valueOf(current.maxShares())) > 0) {
            throw new DomainException(DomainErrorCodes.INVALID_SIZE, current.minShares(), current.maxShares());
        }
        return shares.intValueExact();
    }

    public static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        MoneyLimits current = limits;
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, current.scale(), ROUNDING);
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(limits.scale(), ROUNDING);
    }
}
