package com.cc.brokerage.adapter.in.web.controller;

import com.cc.brokerage.adapter.in.web.api.ApiProblemModels;
import com.cc.brokerage.adapter.in.web.api.MoneyFormats;
import com.cc.brokerage.adapter.in.web.api.OrderResponse;
import com.cc.brokerage.adapter.in.web.api.PlaceOrderRequest;
import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.ListedOrder;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.PlaceOrderCommand;
import com.cc.brokerage.domain.model.PlaceOrderOutcome;
import com.cc.brokerage.domain.port.in.CancelOrderUseCase;
import com.cc.brokerage.domain.port.in.GetOrderUseCase;
import com.cc.brokerage.domain.port.in.ListOrdersUseCase;
import com.cc.brokerage.domain.port.in.PlaceOrderUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/v1/accounts/{accountNumber}/orders")
public class OrderController {

    private final PlaceOrderUseCase placeOrder;
    private final CancelOrderUseCase cancelOrder;
    private final ListOrdersUseCase listOrders;
    private final GetOrderUseCase getOrder;
    private final InstrumentRepository instruments;

    public OrderController(
            PlaceOrderUseCase placeOrder,
            CancelOrderUseCase cancelOrder,
            ListOrdersUseCase listOrders,
            GetOrderUseCase getOrder,
            InstrumentRepository instruments
    ) {
        this.placeOrder = placeOrder;
        this.cancelOrder = cancelOrder;
        this.listOrders = listOrders;
        this.getOrder = getOrder;
        this.instruments = instruments;
    }

    @GetMapping
    @Operation(summary = "Lista las órdenes de la cuenta, de la más antigua a la más nueva")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class)))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class)))
    })
    public List<OrderResponse> list(@PathVariable String accountNumber) {
        return listOrders.list(accountNumber).stream()
                .map(order -> OrderResponse.from(order.order(), accountNumber, order.ticker()))
                .toList();
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Consulta una orden de la cuenta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class))),
            @ApiResponse(responseCode = "409", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.Conflict.class)))
    })
    public OrderResponse get(@PathVariable String accountNumber, @PathVariable String orderId) {
        ListedOrder order = getOrder.get(accountNumber, InputText.orderId(orderId));
        return OrderResponse.from(order.order(), accountNumber, order.ticker());
    }

    @PostMapping
    @Operation(summary = "Crea una orden. Repetir el pedido crea otra orden. Location indica cómo consultarla.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class)))
    })
    public ResponseEntity<OrderResponse> place(@PathVariable String accountNumber, @RequestBody PlaceOrderRequest request) {
        request.requireLimitPrice();
        PlaceOrderCommand command = new PlaceOrderCommand(
                accountNumber,
                request.ticker(),
                request.side(),
                request.type(),
                request.size(),
                MoneyFormats.parse(request.amount(), DomainErrorCodes.AMOUNT_NOT_A_NUMBER),
                MoneyFormats.parse(request.price(), DomainErrorCodes.PRICE_NOT_A_NUMBER)
        );
        PlaceOrderOutcome outcome = placeOrder.place(command);
        Order order = outcome.order();
        String ticker = tickerOf(order);
        OrderResponse body = OrderResponse.placed(order, accountNumber, ticker, outcome.amount(), outcome.rejectionReason());
        URI location = URI.create(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{orderId}")
                .buildAndExpand(order.id())
                .getPath());
        return ResponseEntity.created(location).body(body);
    }

    @PostMapping("/{orderId}/cancellation")
    @Operation(summary = "Cancela una orden abierta de la cuenta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class))),
            @ApiResponse(responseCode = "409", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.Conflict.class)))
    })
    public OrderResponse cancel(@PathVariable String accountNumber, @PathVariable String orderId) {
        long id = InputText.orderId(orderId);
        Order order = cancelOrder.cancel(accountNumber, id);
        return OrderResponse.from(order, accountNumber, tickerOf(order));
    }

    private String tickerOf(Order order) {
        return instruments.findInstrumentById(order.instrumentId())
                .map(Instrument::ticker)
                .orElseThrow(() -> new DomainException(DomainErrorCodes.INSTRUMENT_NOT_FOUND));
    }
}
