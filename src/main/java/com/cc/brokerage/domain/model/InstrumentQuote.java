package com.cc.brokerage.domain.model;

import java.math.BigDecimal;

public record InstrumentQuote(
        String ticker,
        String name,
        String type,
        BigDecimal lastPrice,
        BigDecimal dailyReturnPercent
) {
}
