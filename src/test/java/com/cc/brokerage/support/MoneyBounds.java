package com.cc.brokerage.support;

import com.cc.brokerage.domain.model.MoneyLimits;
import com.cc.brokerage.domain.model.MoneyMath;

import java.math.BigDecimal;

/**
 * Values just inside and just outside the configured money limits.
 */
public record MoneyBounds(MoneyMath money) {

    public static final MoneyMath DEFAULT_MONEY = new MoneyMath(MoneyLimits.defaults());
    public static final MoneyBounds DEFAULTS = new MoneyBounds(DEFAULT_MONEY);

    public BigDecimal justWithinMax() {
        return money.maxAmount().add(fraction().multiply(BigDecimal.valueOf(4)));
    }

    public BigDecimal justPastMax() {
        return money.maxAmount().add(fraction().multiply(BigDecimal.valueOf(5)));
    }

    public BigDecimal oneSharePastMax() {
        return BigDecimal.valueOf(money.maxShares()).add(BigDecimal.ONE);
    }

    public BigDecimal belowMinAmount() {
        return money.minAmount().subtract(BigDecimal.ONE);
    }

    private BigDecimal fraction() {
        return BigDecimal.ONE.movePointLeft(money.scale() + 1);
    }
}
