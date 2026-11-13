package com.cc.brokerage.adapter.in.web;

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
    static PortfolioResponse from(Portfolio portfolio) {
        return new PortfolioResponse(
                portfolio.accountNumber(),
                MoneyFormats.money(portfolio.totalValue()),
                MoneyFormats.money(portfolio.availableCash()),
                MoneyFormats.money(portfolio.settledCash()),
                MoneyFormats.money(portfolio.reservedCash()),
                portfolio.positions().stream().map(PositionResponse::from).toList()
        );
    }
}
