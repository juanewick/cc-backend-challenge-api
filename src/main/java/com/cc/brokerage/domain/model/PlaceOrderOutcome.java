package com.cc.brokerage.domain.model;

import java.math.BigDecimal;

public record PlaceOrderOutcome(
        Order order,
        String ticker,
        BigDecimal amount,
        RejectionReason rejectionReason
) {
}
