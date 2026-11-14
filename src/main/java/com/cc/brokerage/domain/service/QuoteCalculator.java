package com.cc.brokerage.domain.service;

import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentQuote;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.model.MoneyMath;

public class QuoteCalculator {

    private final MoneyMath money;

    public QuoteCalculator(MoneyMath money) {
        this.money = money;
    }

    public InstrumentQuote quote(Instrument instrument, MarketPrice price) {
        if (price == null || price.close() == null) {
            return new InstrumentQuote(instrument.ticker(), instrument.name(), instrument.type().name(), null, null);
        }
        var dailyReturn = price.previousClose() == null
                ? null
                : money.percent(price.close().subtract(price.previousClose()), price.previousClose());
        return new InstrumentQuote(
                instrument.ticker(),
                instrument.name(),
                instrument.type().name(),
                money.scaled(price.close()),
                dailyReturn
        );
    }
}
