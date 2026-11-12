package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.domain.model.Portfolio;

import java.util.List;

public record PortfolioResponse(
        String accountNumber,
        String totalValue,
        String availableCash,
        List<PositionResponse> positions
) {
    static PortfolioResponse from(Portfolio portfolio) {
        return new PortfolioResponse(
                portfolio.accountNumber(),
                MoneyFormats.money(portfolio.totalValue()),
                MoneyFormats.money(portfolio.availableCash()),
                portfolio.positions().stream().map(PositionResponse::from).toList()
        );
    }
}
