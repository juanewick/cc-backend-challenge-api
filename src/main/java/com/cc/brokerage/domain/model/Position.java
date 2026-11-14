package com.cc.brokerage.domain.model;

import java.math.BigDecimal;

public record Position(
        String ticker,
        String name,
        long quantity,
        BigDecimal marketValue,
        BigDecimal totalReturnPercent,
        BigDecimal dailyReturnPercent
) {
}
