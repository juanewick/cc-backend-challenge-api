package com.cc.brokerage.domain.model;

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
            throw new IllegalArgumentException("La escala del importe no puede ser negativa");
        }
        if (minAmount == null || maxAmount == null) {
            throw new IllegalArgumentException("Los límites de importe son obligatorios");
        }
        if (maxAmount.compareTo(minAmount) <= 0) {
            throw new IllegalArgumentException("El importe máximo debe ser mayor que el mínimo");
        }
        if (minShares < 1 || maxShares < minShares || maxShares > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("La cantidad debe entrar entre 1 y " + Integer.MAX_VALUE);
        }
    }

    public String sharesRangeMessage() {
        return "La cantidad debe ser un entero de " + minShares + " a " + maxShares;
    }
}
