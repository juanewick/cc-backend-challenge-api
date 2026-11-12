package com.cc.brokerage.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Order(
        Long id,
        Long instrumentId,
        Long userId,
        int size,
        BigDecimal price,
        OrderType type,
        OrderSide side,
        OrderStatus status,
        LocalDateTime datetime
) {
    public Order withId(Long newId) {
        return new Order(newId, instrumentId, userId, size, price, type, side, status, datetime);
    }

    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, instrumentId, userId, size, price, type, side, newStatus, datetime);
    }

    public BigDecimal notional() {
        return MoneyMath.notional(size, price);
    }
}
