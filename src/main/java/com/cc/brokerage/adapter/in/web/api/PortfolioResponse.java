package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.domain.model.Portfolio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PortfolioResponse(
        String accountNumber,
        @Schema(description = "Valor total a dos decimales, como texto.") String totalValue,
        @Schema(description = "Pesos disponibles a dos decimales, como texto.") String availableCash,
        @Schema(description = "Efectivo liquidado calculado, a dos decimales. No se guarda.") String settledCash,
        @Schema(description = "Efectivo reservado por compras abiertas, a dos decimales. No se guarda.") String reservedCash,
        List<PositionResponse> positions
) {
    public static PortfolioResponse from(Portfolio portfolio, MoneyFormatter money) {
        return new PortfolioResponse(
                portfolio.accountNumber(),
                money.format(portfolio.totalValue()),
                money.format(portfolio.availableCash()),
                money.format(portfolio.settledCash()),
                money.format(portfolio.reservedCash()),
                portfolio.positions().stream().map(position -> PositionResponse.from(position, money)).toList()
        );
    }
}
