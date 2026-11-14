package com.cc.brokerage.adapter.in.web.controller;

import com.cc.brokerage.adapter.in.web.api.ApiProblemModels;
import com.cc.brokerage.adapter.in.web.api.MoneyFormatter;
import com.cc.brokerage.adapter.in.web.api.OrderResponse;
import com.cc.brokerage.adapter.in.web.api.PageResponse;
import com.cc.brokerage.adapter.in.web.api.PlaceOrderRequest;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.ListedOrder;
import com.cc.brokerage.domain.model.PageResult;
import com.cc.brokerage.domain.model.PageSizeLimits;
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
    private final PageSizeLimits pageSizes;

    public OrderController(
            PlaceOrderUseCase placeOrder,
            CancelOrderUseCase cancelOrder,
            ListOrdersUseCase listOrders,
            GetOrderUseCase getOrder,
            MoneyFormatter money,
            PageSizeLimits pageSizes
    ) {
        this.placeOrder = placeOrder;
        this.cancelOrder = cancelOrder;
        this.listOrders = listOrders;
        this.getOrder = getOrder;
        this.money = money;
        this.pageSizes = pageSizes;
    }

    @GetMapping
    @Operation(summary = "Lists the orders of the account by page, oldest first")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class)))
    })
    public PageResponse<OrderResponse> list(
            @PathVariable String accountNumber,
            @Parameter(description = "Only the orders in this status: NEW, FILLED, REJECTED or CANCELLED.")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number, from 0. Defaults to 0.")
            @RequestParam(required = false) String page,
            @Parameter(description = "Orders per page, from 1 to the configured maximum (100 unless changed). Defaults to 20.")
            @RequestParam(required = false) String pageSize
    ) {
        PageResult<ListedOrder> found = listOrders.list(accountNumber, InputText.orderStatus(status), InputText.page(page, pageSize, pageSizes.orderList()));
        return PageResponse.from(found, order -> toResponse(order, accountNumber));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Gets one order of the account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class)))
    })
    public OrderResponse get(@PathVariable String accountNumber, @PathVariable String orderId) {
        return toResponse(getOrder.get(accountNumber, InputText.orderId(orderId)), accountNumber);
    }

    @PostMapping
    @Operation(summary = "Places an order. Repeating the request places another order. Location tells how to get it.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class))),
            @ApiResponse(responseCode = "415", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.UnsupportedMediaType.class)))
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
    @Operation(summary = "Cancels an open order of the account")
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
