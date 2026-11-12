package com.cc.brokerage.domain.model;

import java.math.BigDecimal;

public record Position(
        String ticker,
        String name,
        int quantity,
        BigDecimal marketValue,
        BigDecimal totalReturnPercent,
        BigDecimal dailyReturnPercent
) {
}
