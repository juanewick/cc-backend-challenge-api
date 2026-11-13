package com.cc.brokerage.support;

import com.cc.brokerage.domain.model.MoneyMath;

import java.math.BigDecimal;

public final class MoneyBounds {

    private MoneyBounds() {
    }

    public static BigDecimal justWithinMax() {
        return MoneyMath.maxAmount().add(fraction().multiply(BigDecimal.valueOf(4)));
    }

    public static BigDecimal justPastMax() {
        return MoneyMath.maxAmount().add(fraction().multiply(BigDecimal.valueOf(5)));
    }

    public static BigDecimal oneSharePastMax() {
        return BigDecimal.valueOf(MoneyMath.maxShares()).add(BigDecimal.ONE);
    }

    public static BigDecimal belowMinAmount() {
        return MoneyMath.minAmount().subtract(BigDecimal.ONE);
    }

    private static BigDecimal fraction() {
        return BigDecimal.ONE.movePointLeft(MoneyMath.scale() + 1);
    }
}
