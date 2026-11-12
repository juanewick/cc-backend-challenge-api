package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.domain.model.InstrumentQuote;

public record InstrumentQuoteResponse(
        String ticker,
        String name,
        String type,
        String lastPrice,
        String dailyReturnPercent
) {
    static InstrumentQuoteResponse from(InstrumentQuote quote) {
        return new InstrumentQuoteResponse(
                quote.ticker(),
                quote.name(),
                quote.type(),
                MoneyFormats.money(quote.lastPrice()),
                MoneyFormats.money(quote.dailyReturnPercent())
        );
    }
}
