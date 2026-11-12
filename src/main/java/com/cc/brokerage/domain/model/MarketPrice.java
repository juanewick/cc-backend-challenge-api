package com.cc.brokerage.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketPrice(
        Long id,
        Long instrumentId,
        BigDecimal high,
        BigDecimal low,
        BigDecimal open,
        BigDecimal close,
        BigDecimal previousClose,
        LocalDate date
) {
}
