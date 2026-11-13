package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderType;
import com.cc.brokerage.domain.model.PlaceOrderCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * The JSON body of a new order. It only carries what was sent: the rules live in the use case.
 */
public record PlaceOrderRequest(
        String ticker,
        @Schema(allowableValues = {"BUY", "SELL"}) OrderSide side,
        @Schema(allowableValues = {"MARKET", "LIMIT"}) OrderType type,
        @Schema(description = "Cantidad de acciones. Se envía esto o amount.") Integer size,
        @Schema(type = "string", example = "2500.00", description = "Monto en pesos, como texto. Se envía esto o size.") BigDecimal amount,
        @Schema(type = "string", example = "800.00", description = "Precio límite, como texto. Obligatorio en LIMIT. En MARKET no se envía: la orden se rechaza con BRK-027.") BigDecimal price
) {
    public PlaceOrderCommand toCommand(String accountNumber) {
        return new PlaceOrderCommand(accountNumber, ticker, side, type, size, amount, price);
    }
}
