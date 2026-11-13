package com.cc.brokerage.config;

import com.cc.brokerage.domain.model.MoneyLimits;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "brokerage.money")
public record MoneyProperties(
        int scale,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        long minShares,
        long maxShares
) {

    public MoneyLimits toLimits() {
        return new MoneyLimits(scale, minAmount, maxAmount, minShares, maxShares);
    }
}
