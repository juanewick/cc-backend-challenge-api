package com.cc.brokerage.UnitTest;

import com.cc.brokerage.domain.service.PortfolioCalculator;
import com.cc.brokerage.domain.model.Portfolio;
import com.cc.brokerage.domain.model.Position;
import com.cc.brokerage.support.Account10001Fixture;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioCalculatorTest {

    private final PortfolioCalculator calculator = new PortfolioCalculator();

    @Test
    void account10001MatchesReferenceOracle() {
        Portfolio portfolio = calculator.toPortfolio(
                Account10001Fixture.USER,
                Account10001Fixture.orders(),
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
        assertThat(bma.totalReturnPercent()).isEqualByComparingTo("0.48");
        assertThat(bma.dailyReturnPercent()).isEqualByComparingTo("-1.15");

        Position metr = portfolio.positions().get(1);
        assertThat(metr.quantity()).isEqualTo(500);
        assertThat(metr.marketValue()).isEqualByComparingTo("114750.00");
        assertThat(metr.totalReturnPercent()).isEqualByComparingTo("-8.20");
        assertThat(metr.dailyReturnPercent()).isEqualByComparingTo("-1.08");

        Position pamp = portfolio.positions().get(2);
        assertThat(pamp.quantity()).isEqualTo(40);
        assertThat(pamp.marketValue()).isEqualByComparingTo("37034.00");
        assertThat(pamp.totalReturnPercent()).isEqualByComparingTo("-0.18");
        assertThat(pamp.dailyReturnPercent()).isEqualByComparingTo("0.44");
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
