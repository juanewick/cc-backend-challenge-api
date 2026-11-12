package com.cc.brokerage.support;

import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.OrderType;
import com.cc.brokerage.domain.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class Account10001Fixture {

    public static final User USER = new User(1L, "emiliano@test.com", "10001");

    public static final Instrument PAMP = new Instrument(47L, "PAMP", "Pampa Holding S.A.", InstrumentType.ACCIONES);
    public static final Instrument BMA = new Instrument(31L, "BMA", "Banco Macro S.A.", InstrumentType.ACCIONES);
    public static final Instrument METR = new Instrument(54L, "METR", "MetroGAS S.A.", InstrumentType.ACCIONES);
    public static final Instrument LOMA = new Instrument(45L, "LOMA", "Loma Negra S.A.", InstrumentType.ACCIONES);
    public static final Instrument GGAL = new Instrument(34L, "GGAL", "Grupo Financiero Galicia", InstrumentType.ACCIONES);
    public static final Instrument ARS = new Instrument(66L, "ARS", "PESOS", InstrumentType.MONEDA);

    private Account10001Fixture() {
    }

    public static List<Order> orders() {
        return List.of(
                order(1L, 66L, 1_000_000, "1", OrderSide.CASH_IN, OrderStatus.FILLED, OrderType.MARKET, "2023-07-12T12:11:20"),
                order(2L, 47L, 50, "930", OrderSide.BUY, OrderStatus.FILLED, OrderType.MARKET, "2023-07-12T12:31:20"),
                order(3L, 47L, 50, "920", OrderSide.BUY, OrderStatus.CANCELLED, OrderType.LIMIT, "2023-07-12T14:21:20"),
                order(4L, 47L, 10, "940", OrderSide.SELL, OrderStatus.FILLED, OrderType.MARKET, "2023-07-12T14:51:20"),
                order(5L, 45L, 50, "710", OrderSide.BUY, OrderStatus.NEW, OrderType.LIMIT, "2023-07-12T15:14:20"),
                order(6L, 47L, 100, "950", OrderSide.SELL, OrderStatus.REJECTED, OrderType.MARKET, "2023-07-12T16:11:20"),
                order(7L, 31L, 60, "1500", OrderSide.BUY, OrderStatus.NEW, OrderType.LIMIT, "2023-07-13T11:13:20"),
                order(8L, 66L, 100_000, "1", OrderSide.CASH_OUT, OrderStatus.FILLED, OrderType.MARKET, "2023-07-13T12:31:20"),
                order(9L, 31L, 20, "1540", OrderSide.BUY, OrderStatus.FILLED, OrderType.LIMIT, "2023-07-13T12:51:20"),
                order(10L, 54L, 500, "250", OrderSide.BUY, OrderStatus.FILLED, OrderType.MARKET, "2023-07-13T14:11:20"),
                order(11L, 31L, 30, "1530", OrderSide.SELL, OrderStatus.FILLED, OrderType.MARKET, "2023-07-13T15:13:20")
        );
    }

    public static Map<Long, Instrument> instruments() {
        return Map.of(
                PAMP.id(), PAMP,
                BMA.id(), BMA,
                METR.id(), METR,
                LOMA.id(), LOMA,
                ARS.id(), ARS
        );
    }

    public static Map<Long, MarketPrice> latestPrices() {
        return Map.of(
                BMA.id(), price(31L, "1502.80", "1520.25"),
                METR.id(), price(54L, "229.50", "232.00"),
                PAMP.id(), price(47L, "925.85", "921.80"),
                LOMA.id(), price(45L, "734.65", "734.35"),
                GGAL.id(), price(34L, "885.80", "917.75")
        );
    }

    public static Order order(
            Long id,
            Long instrumentId,
            int size,
            String price,
            OrderSide side,
            OrderStatus status,
            OrderType type,
            String datetime
    ) {
        return new Order(
                id,
                instrumentId,
                1L,
                size,
                new BigDecimal(price),
                type,
                side,
                status,
                LocalDateTime.parse(datetime)
        );
    }

    private static MarketPrice price(Long instrumentId, String close, String previousClose) {
        return new MarketPrice(
                instrumentId,
                instrumentId,
                null,
                null,
                null,
                new BigDecimal(close),
                new BigDecimal(previousClose),
                LocalDate.of(2023, 7, 14)
        );
    }
}
