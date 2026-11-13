package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.MoneyMath;

import java.math.BigDecimal;

public final class MoneyFormats {

    private MoneyFormats() {
    }

    public static String money(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(MoneyMath.scale(), MoneyMath.ROUNDING).toPlainString();
    }

    public static BigDecimal parse(String value, DomainErrorCodes invalidNumber) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new DomainException(invalidNumber);
        }
    }
}
