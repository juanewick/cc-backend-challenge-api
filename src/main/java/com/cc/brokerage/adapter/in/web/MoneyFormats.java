package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.domain.model.MoneyMath;

import java.math.BigDecimal;

public final class MoneyFormats {

    private MoneyFormats() {
    }

    public static String money(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return MoneyMath.scaled(value).toPlainString();
    }

    public static BigDecimal parse(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El " + field + " no es un número válido");
        }
    }
}
