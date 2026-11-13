package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.domain.model.InstrumentQuote;
import io.swagger.v3.oas.annotations.media.Schema;

public record InstrumentQuoteResponse(
        String ticker,
        String name,
        String type,
        @Schema(description = "Último precio a dos decimales, como texto.") String lastPrice,
        @Schema(description = "Rendimiento diario a dos decimales. 0.48 es 0,48 %, no 48 %.") String dailyReturnPercent
) {
    public static InstrumentQuoteResponse from(InstrumentQuote quote, MoneyFormatter money) {
        return new InstrumentQuoteResponse(
                quote.ticker(),
                quote.name(),
                quote.type(),
                money.format(quote.lastPrice()),
                money.format(quote.dailyReturnPercent())
        );
    }
}
