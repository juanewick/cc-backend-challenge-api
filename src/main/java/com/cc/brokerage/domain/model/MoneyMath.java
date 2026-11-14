package com.cc.brokerage.domain.model;

import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money arithmetic under the configured limits. One instance per configuration, passed to whoever needs it.
 */
public final class MoneyMath {

    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final MoneyLimits limits;

    public MoneyMath(MoneyLimits limits) {
        if (limits == null) {
            throw new IllegalArgumentException("Money limits are required");
        }
        this.limits = limits;
    }

    public MoneyLimits limits() {
        return limits;
    }

    public int scale() {
        return limits.scale();
    }

    public BigDecimal minAmount() {
        return limits.minAmount();
    }

    public BigDecimal maxAmount() {
        return limits.maxAmount();
    }

    public long minShares() {
        return limits.minShares();
    }

    public long maxShares() {
        return limits.maxShares();
    }

    public String sharesRangeMessage() {
        return limits.sharesRangeMessage();
    }

    public BigDecimal zero() {
        return round(BigDecimal.ZERO);
    }

    public BigDecimal round(BigDecimal value) {
        return value.setScale(limits.scale(), ROUNDING);
    }

    /**
     * Rounds an amount or a price and checks it is inside the configured range.
     */
    public BigDecimal scaled(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal rounded = round(value);
        if (rounded.compareTo(limits.minAmount()) <= 0) {
            throw new DomainException(DomainErrorCodes.AMOUNT_NOT_POSITIVE);
        }
        if (rounded.compareTo(limits.maxAmount()) > 0) {
            throw new DomainException(DomainErrorCodes.AMOUNT_ABOVE_MAXIMUM);
        }
        return rounded;
    }

    public BigDecimal notional(long size, BigDecimal price) {
        return round(price.multiply(BigDecimal.valueOf(size)));
    }

    public int sharesFromAmount(BigDecimal amount, BigDecimal price) {
        BigDecimal shares = amount.divideToIntegralValue(price);
        if (shares.compareTo(BigDecimal.valueOf(limits.maxShares())) > 0) {
            throw new DomainException(DomainErrorCodes.INVALID_SIZE, limits.minShares(), limits.maxShares());
        }
        return shares.intValueExact();
    }

    public BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, limits.scale(), ROUNDING);
    }
}
