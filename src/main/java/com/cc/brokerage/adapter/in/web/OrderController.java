package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.PlaceOrderCommand;
import com.cc.brokerage.domain.port.in.CancelOrderUseCase;
import com.cc.brokerage.domain.port.in.PlaceOrderUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiBase.PATH + "/v1/accounts/{accountNumber}/orders")
public class OrderController {

    private final PlaceOrderUseCase placeOrder;
    private final CancelOrderUseCase cancelOrder;
    private final InstrumentRepository instruments;

    public OrderController(
            PlaceOrderUseCase placeOrder,
            CancelOrderUseCase cancelOrder,
            InstrumentRepository instruments
    ) {
        this.placeOrder = placeOrder;
        this.cancelOrder = cancelOrder;
        this.instruments = instruments;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@PathVariable String accountNumber, @RequestBody PlaceOrderRequest request) {
        request.requireLimitPrice();
        PlaceOrderCommand command = new PlaceOrderCommand(
                accountNumber,
                request.ticker(),
                request.side(),
                request.type(),
                request.size(),
                MoneyFormats.parse(request.amount(), "monto"),
                MoneyFormats.parse(request.price(), "precio")
        );
        Order order = placeOrder.place(command);
        String ticker = instruments.findInstrumentById(order.instrumentId()).map(instrument -> instrument.ticker()).orElse(request.ticker());
        return OrderResponse.from(order, accountNumber, ticker);
    }

    @PostMapping("/{orderId}/cancellation")
    public OrderResponse cancel(@PathVariable String accountNumber, @PathVariable String orderId) {
        long id = InputText.orderId(orderId);
        Order order = cancelOrder.cancel(accountNumber, id);
        String ticker = instruments.findInstrumentById(order.instrumentId()).map(instrument -> instrument.ticker()).orElse("");
        return OrderResponse.from(order, accountNumber, ticker);
    }
}
