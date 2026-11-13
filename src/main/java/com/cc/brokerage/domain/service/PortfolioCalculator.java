package com.cc.brokerage.domain.service;

import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.model.MoneyMath;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.Portfolio;
import com.cc.brokerage.domain.model.Position;
import com.cc.brokerage.domain.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortfolioCalculator {

    public Holdings summarize(List<Order> orders) {
        BigDecimal cash = BigDecimal.ZERO.setScale(MoneyMath.SCALE, RoundingMode.HALF_UP);
        BigDecimal reservedCash = BigDecimal.ZERO.setScale(MoneyMath.SCALE, RoundingMode.HALF_UP);
        Map<Long, MutablePosition> positions = new HashMap<>();

        for (Order order : orders) {
            BigDecimal notional = order.notional();
            if (order.status() == OrderStatus.FILLED) {
                switch (order.side()) {
                    case CASH_IN -> cash = cash.add(notional);
                    case CASH_OUT -> cash = cash.subtract(notional);
                    case BUY -> {
                        cash = cash.subtract(notional);
                        positions.computeIfAbsent(order.instrumentId(), id -> new MutablePosition()).buy(order.size(), notional);
                    }
                    case SELL -> {
                        cash = cash.add(notional);
                        positions.computeIfAbsent(order.instrumentId(), id -> new MutablePosition()).sell(order.size(), notional);
                    }
                }
            } else if (order.status() == OrderStatus.NEW) {
                if (order.side() == OrderSide.BUY) {
                    reservedCash = reservedCash.add(notional);
                } else if (order.side() == OrderSide.SELL) {
                    positions.computeIfAbsent(order.instrumentId(), id -> new MutablePosition()).reserveSell(order.size());
                }
            }
        }

        return new Holdings(cash, reservedCash, cash.subtract(reservedCash), Map.copyOf(positions));
    }

    public Portfolio toPortfolio(
            User user,
            List<Order> orders,
            Map<Long, Instrument> instruments,
            Map<Long, MarketPrice> latestPrices
    ) {
        Holdings holdings = summarize(orders);
        List<Position> positions = new ArrayList<>();
        BigDecimal marketTotal = BigDecimal.ZERO.setScale(MoneyMath.SCALE, RoundingMode.HALF_UP);

        for (Map.Entry<Long, MutablePosition> entry : holdings.positions().entrySet()) {
            Instrument instrument = instruments.get(entry.getKey());
            if (instrument == null || instrument.type() == InstrumentType.MONEDA) {
                continue;
            }
            MutablePosition snapshot = entry.getValue();
            if (snapshot.netQuantity == 0) {
                continue;
            }
            MarketPrice price = latestPrices.get(entry.getKey());
            BigDecimal marketValue = null;
            BigDecimal totalReturn = null;
            BigDecimal dailyReturn = null;
            if (price != null && price.close() != null) {
                marketValue = MoneyMath.notional(snapshot.netQuantity, price.close());
                marketTotal = marketTotal.add(marketValue);
                if (snapshot.netInvested.compareTo(BigDecimal.ZERO) != 0) {
                    totalReturn = MoneyMath.percent(marketValue.subtract(snapshot.netInvested), snapshot.netInvested.abs());
                }
                dailyReturn = MoneyMath.percent(price.close().subtract(nullToZero(price.previousClose())), price.previousClose());
            }
            positions.add(new Position(
                    instrument.ticker(),
                    instrument.name(),
                    snapshot.netQuantity,
                    marketValue,
                    totalReturn,
                    dailyReturn
            ));
        }

        positions.sort(Comparator.comparing(Position::ticker));
        return new Portfolio(
                user.accountNumber(),
                holdings.settledCash().add(marketTotal),
                holdings.availableCash(),
                holdings.settledCash(),
                holdings.reservedCash(),
                List.copyOf(positions)
        );
    }

    public int availableShares(Holdings holdings, Long instrumentId) {
        MutablePosition position = holdings.positions().get(instrumentId);
        if (position == null) {
            return 0;
        }
        return position.netQuantity - position.reservedSellQuantity;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record Holdings(
            BigDecimal settledCash,
            BigDecimal reservedCash,
            BigDecimal availableCash,
            Map<Long, MutablePosition> positions
    ) {
    }

    public static final class MutablePosition {
        private int netQuantity;
        private int reservedSellQuantity;
        private BigDecimal netInvested = BigDecimal.ZERO.setScale(MoneyMath.SCALE, RoundingMode.HALF_UP);

        private void buy(int size, BigDecimal notional) {
            netQuantity += size;
            netInvested = netInvested.add(notional);
        }

        private void sell(int size, BigDecimal notional) {
            netQuantity -= size;
            netInvested = netInvested.subtract(notional);
        }

        private void reserveSell(int size) {
            reservedSellQuantity += size;
        }

        public int netQuantity() {
            return netQuantity;
        }

        public BigDecimal netInvested() {
            return netInvested;
        }
    }
}
