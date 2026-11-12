package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.domain.InvalidRequestException;
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
    void requireLimitPrice() {
        if (type != OrderType.LIMIT) {
            return;
        }
        BigDecimal parsed = MoneyFormats.parse(price, "precio");
        if (parsed == null || parsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("El precio es obligatorio y debe ser mayor que cero");
        }
    }
}
