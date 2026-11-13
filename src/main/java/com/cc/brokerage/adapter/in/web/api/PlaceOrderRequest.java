package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderType;

import java.math.BigDecimal;

public record PlaceOrderRequest(
        String ticker,
        OrderSide side,
        OrderType type,
        Integer size,
        String amount,
        String price
) {
    public void requireLimitPrice() {
        if (type != OrderType.LIMIT) {
            return;
        }
        BigDecimal parsed = MoneyFormats.parse(price, DomainErrorCodes.PRICE_NOT_A_NUMBER);
        if (parsed == null || parsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException(DomainErrorCodes.PRICE_NOT_POSITIVE);
        }
    }
}
