package com.cc.brokerage.UnitTest;

import com.cc.brokerage.domain.service.PortfolioCalculator;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.OrderType;
import com.cc.brokerage.domain.model.Portfolio;
import com.cc.brokerage.domain.model.Position;
import com.cc.brokerage.support.Account10001Fixture;
import com.cc.brokerage.support.HoldingsOf;
import com.cc.brokerage.support.MoneyBounds;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioCalculatorTest {

    private final PortfolioCalculator calculator = new PortfolioCalculator(MoneyBounds.DEFAULT_MONEY);

    @Test
    void account10001MatchesReferenceOracle() {
        Portfolio portfolio = calculator.toPortfolio(
                Account10001Fixture.USER,
                HoldingsOf.orders(Account10001Fixture.orders()),
                Account10001Fixture.instruments(),
                Account10001Fixture.latestPrices()
        );

        assertThat(portfolio.settledCash()).isEqualByComparingTo("753000.00");
        assertThat(portfolio.reservedCash()).isEqualByComparingTo("125500.00");
        assertThat(portfolio.availableCash()).isEqualByComparingTo("627500.00");
        assertThat(portfolio.totalValue()).isEqualByComparingTo("889756.00");
        assertThat(portfolio.positions()).extracting(Position::ticker).containsExactly("BMA", "METR", "PAMP");

        Position bma = portfolio.positions().get(0);
        assertThat(bma.quantity()).isEqualTo(-10);
        assertThat(bma.marketValue()).isEqualByComparingTo("-15028.00");
        assertThat(bma.totalReturnPercent()).isEqualByComparingTo("1.78");
        assertThat(bma.dailyReturnPercent()).isEqualByComparingTo("-1.15");

        Position metr = portfolio.positions().get(1);
        assertThat(metr.quantity()).isEqualTo(500);
        assertThat(metr.marketValue()).isEqualByComparingTo("114750.00");
        assertThat(metr.totalReturnPercent()).isEqualByComparingTo("-8.20");
        assertThat(metr.dailyReturnPercent()).isEqualByComparingTo("-1.08");

        Position pamp = portfolio.positions().get(2);
        assertThat(pamp.quantity()).isEqualTo(40);
        assertThat(pamp.marketValue()).isEqualByComparingTo("37034.00");
        assertThat(pamp.totalReturnPercent()).isEqualByComparingTo("-0.45");
        assertThat(pamp.dailyReturnPercent()).isEqualByComparingTo("0.44");
    }

    @Test
    void partialSaleKeepsTheReturnOnAverageCost() {
        // Buy 10 at 100, sell 5 at 150: the 5 shares left cost 100 each and close at 925.85 (PAMP's last close).
        Portfolio portfolio = calculator.toPortfolio(
                Account10001Fixture.USER,
                HoldingsOf.orders(List.of(
                        Account10001Fixture.order(1L, 66L, 10_000, "1", OrderSide.CASH_IN, OrderStatus.FILLED, OrderType.MARKET, "2023-07-12T10:00:00"),
                        Account10001Fixture.order(2L, 47L, 10, "100", OrderSide.BUY, OrderStatus.FILLED, OrderType.MARKET, "2023-07-12T11:00:00"),
                        Account10001Fixture.order(3L, 47L, 5, "150", OrderSide.SELL, OrderStatus.FILLED, OrderType.MARKET, "2023-07-12T12:00:00"),
                        Account10001Fixture.order(4L, 47L, 1, "900", OrderSide.BUY, OrderStatus.CANCELLED, OrderType.LIMIT, "2023-07-12T13:00:00")
                )),
                Account10001Fixture.instruments(),
                Account10001Fixture.latestPrices()
        );

        Position pamp = portfolio.positions().get(0);
        assertThat(pamp.quantity()).isEqualTo(5);
        assertThat(pamp.totalReturnPercent()).isEqualByComparingTo("825.85");
    }

    @Test
    void sellsAboveBuysDoNotTurnTheReturnUpsideDown() {
        // Net capital would be 1000 - 1800 = -800; the average cost of the shares bought is still 100.
        Portfolio portfolio = calculator.toPortfolio(
                Account10001Fixture.USER,
                HoldingsOf.orders(List.of(
                        Account10001Fixture.order(1L, 47L, 10, "100", OrderSide.BUY, OrderStatus.FILLED, OrderType.MARKET, "2023-07-12T11:00:00"),
                        Account10001Fixture.order(2L, 47L, 9, "200", OrderSide.SELL, OrderStatus.FILLED, OrderType.MARKET, "2023-07-12T12:00:00")
                )),
                Account10001Fixture.instruments(),
                Account10001Fixture.latestPrices()
        );

        assertThat(portfolio.positions().get(0).totalReturnPercent()).isEqualByComparingTo("825.85");
    }

    @Test
    void shortPositionGainsWhenThePriceFalls() {
        // Sold 10 BMA at 1600 without holding them; BMA closes at 1502.80.
        Portfolio portfolio = calculator.toPortfolio(
                Account10001Fixture.USER,
                HoldingsOf.orders(List.of(
                        Account10001Fixture.order(1L, 31L, 10, "1600", OrderSide.SELL, OrderStatus.FILLED, OrderType.MARKET, "2023-07-12T11:00:00")
                )),
                Account10001Fixture.instruments(),
                Account10001Fixture.latestPrices()
        );

        Position bma = portfolio.positions().get(0);
        assertThat(bma.quantity()).isEqualTo(-10);
        assertThat(bma.totalReturnPercent()).isEqualByComparingTo("6.08");
    }

    @Test
    void emptyOrdersYieldZeroPortfolio() {
        Portfolio portfolio = calculator.toPortfolio(
                Account10001Fixture.USER,
                List.of(),
                Account10001Fixture.instruments(),
                Account10001Fixture.latestPrices()
        );

        assertThat(portfolio.availableCash()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(portfolio.totalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(portfolio.positions()).isEmpty();
    }
}
