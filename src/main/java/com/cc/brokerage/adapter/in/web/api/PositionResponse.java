package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.domain.model.Position;
import io.swagger.v3.oas.annotations.media.Schema;

public record PositionResponse(
        String ticker,
        String name,
        int quantity,
        @Schema(description = "Importe a dos decimales, como texto.") String marketValue,
        @Schema(description = "Rendimiento total a dos decimales. 0.48 es 0,48 %, no 48 %.") String totalReturnPercent,
        @Schema(description = "Rendimiento diario a dos decimales. 0.48 es 0,48 %, no 48 %.") String dailyReturnPercent
) {
    static PositionResponse from(Position position) {
        return new PositionResponse(
                position.ticker(),
                position.name(),
                position.quantity(),
                MoneyFormats.money(position.marketValue()),
                MoneyFormats.money(position.totalReturnPercent()),
                MoneyFormats.money(position.dailyReturnPercent())
        );
    }
}
