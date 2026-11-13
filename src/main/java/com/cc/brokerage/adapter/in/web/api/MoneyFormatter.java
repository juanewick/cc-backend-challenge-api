package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.domain.model.MoneyMath;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Writes amounts as plain text at the configured scale, the way every money field of the API is published.
 */
@Component
public class MoneyFormatter {

    private final MoneyMath money;

    public MoneyFormatter(MoneyMath money) {
        this.money = money;
    }

    public String format(BigDecimal value) {
        return value == null ? null : money.round(value).toPlainString();
    }
}
