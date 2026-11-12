package com.cc.brokerage.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record Portfolio(
        String accountNumber,
        BigDecimal totalValue,
        BigDecimal availableCash,
        List<Position> positions
) {
}
