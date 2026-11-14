package com.cc.brokerage.adapter.in.web.controller;

import com.cc.brokerage.adapter.in.web.api.ApiProblemModels;
import com.cc.brokerage.adapter.in.web.api.InstrumentQuoteResponse;
import com.cc.brokerage.adapter.in.web.api.MoneyFormatter;
import com.cc.brokerage.adapter.in.web.api.PageResponse;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.PageQuery;
import com.cc.brokerage.domain.model.PageSizeLimits;
import com.cc.brokerage.domain.port.in.GetInstrumentQuoteUseCase;
import com.cc.brokerage.domain.port.in.SearchInstrumentsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/instruments")
public class InstrumentController {

    private final SearchInstrumentsUseCase searchInstruments;
    private final GetInstrumentQuoteUseCase quotes;
    private final MoneyFormatter money;
    private final PageSizeLimits pageSizes;

    public InstrumentController(SearchInstrumentsUseCase searchInstruments, GetInstrumentQuoteUseCase quotes, MoneyFormatter money, PageSizeLimits pageSizes) {
        this.searchInstruments = searchInstruments;
        this.quotes = quotes;
        this.money = money;
        this.pageSizes = pageSizes;
    }

    @GetMapping
    @Operation(summary = "Searches instruments by ticker or name, by page, sorted by ticker. Never returns the currency.")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class})))
    })
    public PageResponse<InstrumentQuoteResponse> search(
            @RequestParam("q") String query,
            @Parameter(description = "Page number, from 0. Defaults to 0.")
            @RequestParam(required = false) String page,
            @Parameter(description = "Instruments per page, from 1 to the configured maximum (100 unless changed). Defaults to 20.")
            @RequestParam(required = false) String pageSize
    ) {
        // The search text is checked before the page, so a blank search reports SEARCH_EMPTY whatever the page says.
        String text = InputText.search(query);
        PageQuery requested = InputText.page(page, pageSize, pageSizes.instrumentSearch());
        return PageResponse.from(searchInstruments.search(text, requested), quote -> InstrumentQuoteResponse.from(quote, money));
    }

    @GetMapping("/{ticker}")
    @Operation(summary = "Quote of one ticker. A return of 0.48 means 0.48 %.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = InstrumentQuoteResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(oneOf = {ApiProblemModels.InvalidParameters.class, ApiProblemModels.BadRequest.class}))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemModels.NotFound.class)))
    })
    public InstrumentQuoteResponse quote(@PathVariable String ticker) {
        return InstrumentQuoteResponse.from(quotes.quote(ticker), money);
    }
}
