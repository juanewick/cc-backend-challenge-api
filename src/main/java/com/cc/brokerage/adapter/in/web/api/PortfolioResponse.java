package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.domain.model.Portfolio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PortfolioResponse(
        String accountNumber,
        @Schema(description = "Total value with two decimals, as text.") String totalValue,
        @Schema(description = "Available pesos with two decimals, as text.") String availableCash,
        @Schema(description = "Computed settled cash, with two decimals. Not stored.") String settledCash,
        @Schema(description = "Cash reserved by open buys, with two decimals. Not stored.") String reservedCash,
        List<PositionResponse> positions,
        @Schema(description = "Tickers of the positions without a close. They are listed but left out of the total value. Empty when every position has a price.") List<String> unpricedTickers
) {
    public static PortfolioResponse from(Portfolio portfolio, MoneyFormatter money) {
        return new PortfolioResponse(
                portfolio.accountNumber(),
                money.format(portfolio.totalValue()),
                money.format(portfolio.availableCash()),
                money.format(portfolio.settledCash()),
                money.format(portfolio.reservedCash()),
                portfolio.positions().stream().map(position -> PositionResponse.from(position, money)).toList(),
                portfolio.unpricedTickers()
        );
    }
}
