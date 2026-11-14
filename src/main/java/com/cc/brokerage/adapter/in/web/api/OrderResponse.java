package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.OrderType;
import com.cc.brokerage.domain.model.RejectionReason;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public record OrderResponse(
        Long id,
        String accountNumber,
        String ticker,
        OrderSide side,
        OrderType type,
        int size,
        @Schema(description = "Price with two decimals, as text.") String price,
        OrderStatus status,
        String datetime,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "Computed amount, size times price, with two decimals. Only when placing the order. Not stored.")
        String amount,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "Rejection reason. Only when placing the order and the status is REJECTED. Not stored.")
        RejectionReason rejectionReason
) {
    public static OrderResponse from(Order order, String accountNumber, String ticker, MoneyFormatter money) {
        return new OrderResponse(
                order.id(),
                accountNumber,
                ticker,
                order.side(),
                order.type(),
                order.size(),
                money.format(order.price()),
                order.status(),
                order.datetime() == null ? null : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(order.datetime()),
                null,
                null
        );
    }

    public static OrderResponse placed(Order order, String accountNumber, String ticker, BigDecimal amount, RejectionReason reason, MoneyFormatter money) {
        OrderResponse response = from(order, accountNumber, ticker, money);
        return new OrderResponse(
                response.id(),
                response.accountNumber(),
                response.ticker(),
                response.side(),
                response.type(),
                response.size(),
                response.price(),
                response.status(),
                response.datetime(),
                money.format(amount),
                reason
        );
    }
}
