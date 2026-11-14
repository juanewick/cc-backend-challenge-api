package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.domain.model.InstrumentQuote;
import io.swagger.v3.oas.annotations.media.Schema;

public record InstrumentQuoteResponse(
        String ticker,
        String name,
        String type,
        @Schema(description = "Latest price with two decimals, as text.") String lastPrice,
        @Schema(description = "Daily return with two decimals. 0.48 means 0.48 %, not 48 %.") String dailyReturnPercent
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
