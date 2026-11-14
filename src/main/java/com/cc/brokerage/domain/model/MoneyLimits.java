package com.cc.brokerage.domain.model;

import com.cc.brokerage.domain.DomainErrorCodes;

import java.math.BigDecimal;

public record MoneyLimits(
        int scale,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        long minShares,
        long maxShares
) {

    public static MoneyLimits defaults() {
        return new MoneyLimits(2, BigDecimal.ZERO, new BigDecimal("99999999.99"), 1L, Integer.MAX_VALUE);
    }

    public MoneyLimits {
        if (scale < 0) {
            throw new IllegalArgumentException("The money scale cannot be negative");
        }
        if (minAmount == null || maxAmount == null) {
            throw new IllegalArgumentException("Money limits are required");
        }
        if (maxAmount.compareTo(minAmount) <= 0) {
            throw new IllegalArgumentException("The maximum amount must be greater than the minimum");
        }
        if (minShares < 1 || maxShares < minShares || maxShares > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Size must be between 1 and " + Integer.MAX_VALUE);
        }
    }

    public String sharesRangeMessage() {
        return DomainErrorCodes.INVALID_SIZE.message().formatted(minShares, maxShares);
    }
}
