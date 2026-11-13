package com.cc.brokerage.adapter.in.web.controller;

import com.cc.brokerage.adapter.in.web.api.ApiProblemModels;
import com.cc.brokerage.adapter.in.web.api.InstrumentQuoteResponse;
import com.cc.brokerage.domain.port.in.GetInstrumentQuoteUseCase;
import com.cc.brokerage.domain.port.in.SearchInstrumentsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/instruments")
public class InstrumentController {

    private final SearchInstrumentsUseCase searchInstruments;
    private final GetInstrumentQuoteUseCase quotes;

    public InstrumentController(SearchInstrumentsUseCase searchInstruments, GetInstrumentQuoteUseCase quotes) {
        this.searchInstruments = searchInstruments;
        this.quotes = quotes;
    }

    @GetMapping
    @Operation(summary = "Busca activos por ticker o nombre. No devuelve la moneda.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = InstrumentQuoteResponse.class)))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class})))
    })
    public List<InstrumentQuoteResponse> search(@RequestParam("q") String query) {
        return searchInstruments.search(query).stream().map(InstrumentQuoteResponse::from).toList();
    }

    @GetMapping("/{ticker}")
    @Operation(summary = "Cotización de un ticker. El rendimiento 0.48 es 0,48 %.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = InstrumentQuoteResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class)))
    })
    public InstrumentQuoteResponse quote(@PathVariable String ticker) {
        return InstrumentQuoteResponse.from(quotes.quote(ticker));
    }
}
