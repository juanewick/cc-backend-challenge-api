package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.domain.model.Position;

public record PositionResponse(
        String ticker,
        String name,
        int quantity,
        String marketValue,
        String totalReturnPercent,
        String dailyReturnPercent
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
