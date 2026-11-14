package com.cc.brokerage.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param totalValue      settled cash plus the market value of the positions that have a close
 * @param unpricedTickers positions without a close, by ticker: they are listed but left out of totalValue
 */
public record Portfolio(
        String accountNumber,
        BigDecimal totalValue,
        BigDecimal availableCash,
        BigDecimal settledCash,
        BigDecimal reservedCash,
        List<Position> positions,
        List<String> unpricedTickers
) {
}
