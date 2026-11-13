package com.cc.brokerage.domain.service;

import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentHoldings;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.model.MoneyMath;
import com.cc.brokerage.domain.model.Portfolio;
import com.cc.brokerage.domain.model.Position;
import com.cc.brokerage.domain.model.User;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the per-instrument totals of an account into cash and positions. The totals themselves are added up
 * by the order repository, so this never walks the order history.
 */
public class PortfolioCalculator {

    private final MoneyMath money;

    public PortfolioCalculator(MoneyMath money) {
        this.money = money;
    }

    public Holdings summarize(List<InstrumentHoldings> rows) {
        BigDecimal cashFlow = money.zero();
        BigDecimal invested = money.zero();
        BigDecimal reservedCash = money.zero();
        Map<Long, InstrumentHoldings> positions = new HashMap<>();

        for (InstrumentHoldings row : rows) {
            cashFlow = cashFlow.add(row.cashFlow());
            invested = invested.add(row.netInvested());
            reservedCash = reservedCash.add(row.reservedBuyAmount());
            if (row.instrumentId() != null) {
                positions.put(row.instrumentId(), row);
            }
        }

        BigDecimal settledCash = money.round(cashFlow.subtract(invested));
        reservedCash = money.round(reservedCash);
        return new Holdings(settledCash, reservedCash, settledCash.subtract(reservedCash), Map.copyOf(positions));
    }

    public Portfolio toPortfolio(
            User user,
            List<InstrumentHoldings> rows,
            Map<Long, Instrument> instruments,
            Map<Long, MarketPrice> latestPrices
    ) {
        Holdings holdings = summarize(rows);
        List<Position> positions = new ArrayList<>();
        BigDecimal marketTotal = money.zero();

        for (Map.Entry<Long, InstrumentHoldings> entry : holdings.positions().entrySet()) {
            Instrument instrument = instruments.get(entry.getKey());
            if (instrument == null || instrument.type() == InstrumentType.MONEDA) {
                continue;
            }
            InstrumentHoldings snapshot = entry.getValue();
            if (snapshot.netQuantity() == 0) {
                continue;
            }
            MarketPrice price = latestPrices.get(entry.getKey());
            BigDecimal marketValue = null;
            BigDecimal totalReturn = null;
            BigDecimal dailyReturn = null;
            if (price != null && price.close() != null) {
                marketValue = money.notional(snapshot.netQuantity(), price.close());
                marketTotal = marketTotal.add(marketValue);
                totalReturn = totalReturn(snapshot, price.close());
                dailyReturn = money.percent(price.close().subtract(nullToZero(price.previousClose())), price.previousClose());
            }
            positions.add(new Position(
                    instrument.ticker(),
                    instrument.name(),
                    snapshot.netQuantity(),
                    marketValue,
                    totalReturn,
                    dailyReturn
            ));
        }

        positions.sort(Comparator.comparing(Position::ticker));
        List<String> unpricedTickers = positions.stream()
                .filter(position -> position.marketValue() == null)
                .map(Position::ticker)
                .toList();
        return new Portfolio(
                user.accountNumber(),
                holdings.settledCash().add(marketTotal),
                holdings.availableCash(),
                holdings.settledCash(),
                holdings.reservedCash(),
                List.copyOf(positions),
                unpricedTickers
        );
    }

    public long availableShares(Holdings holdings, Long instrumentId) {
        InstrumentHoldings position = holdings.positions().get(instrumentId);
        if (position == null) {
            return 0;
        }
        return position.netQuantity() - position.reservedSellQuantity();
    }

    /**
     * Return of the shares still held against their average cost. A long position compares the close with the
     * average price of every FILLED buy; a short one compares the average price of every FILLED sell with the
     * close, so a falling price is a gain. Without such orders there is no cost and the return is null.
     */
    private BigDecimal totalReturn(InstrumentHoldings snapshot, BigDecimal close) {
        if (snapshot.netQuantity() > 0) {
            BigDecimal averageCost = average(snapshot.boughtAmount(), snapshot.boughtQuantity());
            return averageCost == null ? null : money.percent(close.subtract(averageCost), averageCost);
        }
        BigDecimal averageSale = average(snapshot.soldAmount(), snapshot.soldQuantity());
        return averageSale == null ? null : money.percent(averageSale.subtract(close), averageSale);
    }

    private static BigDecimal average(BigDecimal amount, long quantity) {
        if (quantity <= 0 || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return amount.divide(BigDecimal.valueOf(quantity), MathContext.DECIMAL128);
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record Holdings(
            BigDecimal settledCash,
            BigDecimal reservedCash,
            BigDecimal availableCash,
            Map<Long, InstrumentHoldings> positions
    ) {
    }
}
