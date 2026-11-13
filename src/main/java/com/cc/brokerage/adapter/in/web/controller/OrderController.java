package com.cc.brokerage.adapter.in.web.controller;

import com.cc.brokerage.adapter.in.web.api.ApiProblemModels;
import com.cc.brokerage.adapter.in.web.api.MoneyFormatter;
import com.cc.brokerage.adapter.in.web.api.OrderPageResponse;
import com.cc.brokerage.adapter.in.web.api.OrderResponse;
import com.cc.brokerage.adapter.in.web.api.PlaceOrderRequest;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.ListedOrder;
import com.cc.brokerage.domain.model.PageResult;
import com.cc.brokerage.domain.model.PlaceOrderOutcome;
import com.cc.brokerage.domain.port.in.CancelOrderUseCase;
import com.cc.brokerage.domain.port.in.GetOrderUseCase;
import com.cc.brokerage.domain.port.in.ListOrdersUseCase;
import com.cc.brokerage.domain.port.in.PlaceOrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/v1/accounts/{accountNumber}/orders")
public class OrderController {

    private final PlaceOrderUseCase placeOrder;
    private final CancelOrderUseCase cancelOrder;
    private final ListOrdersUseCase listOrders;
    private final GetOrderUseCase getOrder;
    private final MoneyFormatter money;

    public OrderController(
            PlaceOrderUseCase placeOrder,
            CancelOrderUseCase cancelOrder,
            ListOrdersUseCase listOrders,
            GetOrderUseCase getOrder,
            MoneyFormatter money
    ) {
        this.placeOrder = placeOrder;
        this.cancelOrder = cancelOrder;
        this.listOrders = listOrders;
        this.getOrder = getOrder;
        this.money = money;
    }

    @GetMapping
    @Operation(summary = "Lista las órdenes de la cuenta por páginas, de la más antigua a la más nueva")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OrderPageResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class)))
    })
    public OrderPageResponse list(
            @PathVariable String accountNumber,
            @Parameter(description = "Solo las órdenes en este estado: NEW, FILLED, REJECTED o CANCELLED.")
            @RequestParam(required = false) String status,
            @Parameter(description = "Número de página, desde 0. Por defecto 0.")
            @RequestParam(required = false) String page,
            @Parameter(description = "Órdenes por página, de 1 a 100. Por defecto 20.")
            @RequestParam(required = false) String pageSize
    ) {
        PageResult<ListedOrder> found = listOrders.list(accountNumber, InputText.orderStatus(status), InputText.page(page, pageSize));
        return new OrderPageResponse(
                found.content().stream().map(order -> toResponse(order, accountNumber)).toList(),
                found.page(),
                found.size(),
                found.totalElements(),
                found.totalPages()
        );
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Consulta una orden de la cuenta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class)))
    })
    public OrderResponse get(@PathVariable String accountNumber, @PathVariable String orderId) {
        return toResponse(getOrder.get(accountNumber, InputText.orderId(orderId)), accountNumber);
    }

    @PostMapping
    @Operation(summary = "Crea una orden. Repetir el pedido crea otra orden. Location indica cómo consultarla.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class)))
    })
    public ResponseEntity<OrderResponse> place(@PathVariable String accountNumber, @RequestBody PlaceOrderRequest request) {
        PlaceOrderOutcome outcome = placeOrder.place(request.toCommand(accountNumber));
        OrderResponse body = OrderResponse.placed(
                outcome.order(), accountNumber, outcome.ticker(), outcome.amount(), outcome.rejectionReason(), money);
        URI location = URI.create(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{orderId}")
                .buildAndExpand(outcome.order().id())
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
        return toResponse(cancelOrder.cancel(accountNumber, InputText.orderId(orderId)), accountNumber);
    }

    private OrderResponse toResponse(ListedOrder order, String accountNumber) {
        return OrderResponse.from(order.order(), accountNumber, order.ticker(), money);
    }
}
