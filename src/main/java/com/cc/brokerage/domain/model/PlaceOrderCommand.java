package com.cc.brokerage.domain.model;

import java.math.BigDecimal;

public record PlaceOrderCommand(
        String accountNumber,
        String ticker,
        OrderSide side,
        OrderType type,
        Integer size,
        BigDecimal amount,
        BigDecimal price
) {
}
