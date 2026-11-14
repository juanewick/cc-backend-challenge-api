package com.cc.brokerage.adapter.in.web.controller;

import com.cc.brokerage.adapter.in.web.api.ApiProblemModels;
import com.cc.brokerage.adapter.in.web.api.MoneyFormatter;
import com.cc.brokerage.adapter.in.web.api.PortfolioResponse;
import com.cc.brokerage.domain.port.in.GetPortfolioUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts/{accountNumber}/portfolio")
public class PortfolioController {

    private final GetPortfolioUseCase getPortfolio;
    private final MoneyFormatter money;

    public PortfolioController(GetPortfolioUseCase getPortfolio, MoneyFormatter money) {
        this.getPortfolio = getPortfolio;
        this.money = money;
    }

    @GetMapping
    @Operation(summary = "Portfolio of the account. Settled and reserved cash are computed, not stored.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PortfolioResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class)))
    })
    public PortfolioResponse get(@PathVariable String accountNumber) {
        return PortfolioResponse.from(getPortfolio.getPortfolio(accountNumber), money);
    }
}
