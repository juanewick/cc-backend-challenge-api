package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.domain.model.Position;
import io.swagger.v3.oas.annotations.media.Schema;

public record PositionResponse(
        String ticker,
        String name,
        long quantity,
        @Schema(description = "Amount with two decimals, as text.") String marketValue,
        @Schema(description = "Total return with two decimals, on average cost. 0.48 means 0.48 %, not 48 %.") String totalReturnPercent,
        @Schema(description = "Daily return with two decimals. 0.48 means 0.48 %, not 48 %.") String dailyReturnPercent
) {
    static PositionResponse from(Position position, MoneyFormatter money) {
        return new PositionResponse(
                position.ticker(),
                position.name(),
                position.quantity(),
                money.format(position.marketValue()),
                money.format(position.totalReturnPercent()),
                money.format(position.dailyReturnPercent())
        );
    }
}
