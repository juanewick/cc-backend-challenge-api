package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.OrderType;

import java.time.format.DateTimeFormatter;

public record OrderResponse(
        Long id,
        String accountNumber,
        String ticker,
        OrderSide side,
        OrderType type,
        int size,
        String price,
        OrderStatus status,
        String datetime
) {
    static OrderResponse from(Order order, String accountNumber, String ticker) {
        return new OrderResponse(
                order.id(),
                accountNumber,
                ticker,
                order.side(),
                order.type(),
                order.size(),
                MoneyFormats.money(order.price()),
                order.status(),
                order.datetime() == null ? null : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(order.datetime())
        );
    }
}
