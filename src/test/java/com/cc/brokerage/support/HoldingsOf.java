package com.cc.brokerage.support;

import com.cc.brokerage.domain.model.InstrumentHoldings;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reference implementation, for unit tests, of what OrderRepository.summarizeHoldings adds up in SQL. The
 * integration tests check that the query returns the same account totals.
 */
public final class HoldingsOf {

    private HoldingsOf() {
    }

    public static List<InstrumentHoldings> orders(List<Order> orders) {
        Map<Long, Totals> byInstrument = new LinkedHashMap<>();
        for (Order order : orders) {
            if (order.status() != OrderStatus.FILLED && order.status() != OrderStatus.NEW) {
                continue;
            }
            Totals totals = byInstrument.computeIfAbsent(order.instrumentId(), id -> new Totals());
            BigDecimal notional = order.price().multiply(BigDecimal.valueOf(order.size()));
            boolean filled = order.status() == OrderStatus.FILLED;
            if (filled && order.side() == OrderSide.BUY) {
                totals.netQuantity += order.size();
                totals.netInvested = totals.netInvested.add(notional);
            } else if (filled && order.side() == OrderSide.SELL) {
                totals.netQuantity -= order.size();
                totals.netInvested = totals.netInvested.subtract(notional);
            } else if (filled && order.side() == OrderSide.CASH_IN) {
                totals.cashFlow = totals.cashFlow.add(notional);
            } else if (filled && order.side() == OrderSide.CASH_OUT) {
                totals.cashFlow = totals.cashFlow.subtract(notional);
            } else if (!filled && order.side() == OrderSide.BUY) {
                totals.reservedBuyAmount = totals.reservedBuyAmount.add(notional);
            } else if (!filled && order.side() == OrderSide.SELL) {
                totals.reservedSellQuantity += order.size();
            }
        }
        List<InstrumentHoldings> rows = new ArrayList<>();
        byInstrument.forEach((id, totals) -> rows.add(new InstrumentHoldings(
                id, totals.netQuantity, totals.netInvested, totals.reservedSellQuantity, totals.reservedBuyAmount, totals.cashFlow)));
        return rows;
    }

    private static final class Totals {
        private long netQuantity;
        private BigDecimal netInvested = BigDecimal.ZERO;
        private long reservedSellQuantity;
        private BigDecimal reservedBuyAmount = BigDecimal.ZERO;
        private BigDecimal cashFlow = BigDecimal.ZERO;
    }
}
