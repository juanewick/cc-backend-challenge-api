package com.cc.brokerage.application;

import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.OrderType;
import com.cc.brokerage.domain.model.PlaceOrderCommand;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import com.cc.brokerage.domain.service.PortfolioCalculator;
import com.cc.brokerage.support.Account10001Fixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RejectOrderServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private InstrumentRepository instruments;
    @Mock
    private MarketPriceRepository prices;
    @Mock
    private OrderRepository orders;

    private final PortfolioCalculator calculator = new PortfolioCalculator();
    private PlaceOrderService service;

    @BeforeEach
    void setUp() {
        service = new PlaceOrderService(
                users,
                instruments,
                prices,
                orders,
                calculator,
                Clock.fixed(Instant.parse("2023-07-14T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void oversizedMarketBuyIsRejected() {
        stubAccountAndGgalWithClose();
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order placed = service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.MARKET, 1_000_000, null, null));

        assertThat(placed.status()).isEqualTo(OrderStatus.REJECTED);
        assertUnchangedHoldings(placed);
    }

    @Test
    void oversizedMarketSellIsRejected() {
        stubAccount();
        when(instruments.findByTicker("PAMP")).thenReturn(Optional.of(Account10001Fixture.PAMP));
        when(prices.findLatestByInstrumentId(47L)).thenReturn(Optional.of(Account10001Fixture.latestPrices().get(47L)));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order placed = service.place(new PlaceOrderCommand(
                "10001", "PAMP", OrderSide.SELL, OrderType.MARKET, 100, null, null));

        assertThat(placed.status()).isEqualTo(OrderStatus.REJECTED);
        assertUnchangedHoldings(placed);
        var after = calculator.summarize(withSaved(placed));
        assertThat(calculator.availableShares(after, Account10001Fixture.PAMP.id())).isEqualTo(40);
    }

    @Test
    void amountThatYieldsZeroSharesIsSavedRejected() {
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(instruments.findByTicker("GGAL")).thenReturn(Optional.of(Account10001Fixture.GGAL));
        when(prices.findLatestByInstrumentId(34L)).thenReturn(Optional.of(Account10001Fixture.latestPrices().get(34L)));
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.MARKET, null, new BigDecimal("100"), null));

        verify(orders).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.size()).isEqualTo(0);
        assertThat(saved.status()).isEqualTo(OrderStatus.REJECTED);
        assertUnchangedHoldings(saved);
    }

    @Test
    void reservedCashIsNotReusedAndOrderIsNotDownsized() {
        stubAccountAndGgalWithClose();
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order placed = service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.MARKET, 850, null, null));

        assertThat(placed.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(placed.size()).isEqualTo(850);
        assertThat(placed.notional()).isEqualByComparingTo("752930.00");
        assertUnchangedHoldings(placed);
    }

    @Test
    void secondOrderThatExceedsRemainingAvailableIsRejected() {
        stubAccountAndGgal();
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order first = service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.LIMIT, 400, null, new BigDecimal("800.00")));
        assertThat(first.status()).isEqualTo(OrderStatus.NEW);

        when(orders.findByUserId(1L)).thenReturn(withSaved(first));

        Order second = service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.LIMIT, 400, null, new BigDecimal("800.00")));
        assertThat(second.status()).isEqualTo(OrderStatus.REJECTED);

        List<Order> afterBoth = new ArrayList<>(Account10001Fixture.orders());
        afterBoth.add(first);
        afterBoth.add(second);
        assertThat(calculator.summarize(afterBoth).availableCash()).isEqualByComparingTo("307500.00");
    }

    private void stubAccount() {
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(orders.findByUserId(1L)).thenReturn(Account10001Fixture.orders());
    }

    private void stubAccountAndGgal() {
        stubAccount();
        when(instruments.findByTicker("GGAL")).thenReturn(Optional.of(Account10001Fixture.GGAL));
    }

    private void stubAccountAndGgalWithClose() {
        stubAccountAndGgal();
        when(prices.findLatestByInstrumentId(34L)).thenReturn(Optional.of(Account10001Fixture.latestPrices().get(34L)));
    }

    private void assertUnchangedHoldings(Order saved) {
        var before = calculator.summarize(Account10001Fixture.orders());
        var after = calculator.summarize(withSaved(saved));
        assertThat(after.settledCash()).isEqualByComparingTo(before.settledCash());
        assertThat(after.availableCash()).isEqualByComparingTo(before.availableCash());
    }

    private static List<Order> withSaved(Order saved) {
        List<Order> all = new ArrayList<>(Account10001Fixture.orders());
        all.add(saved);
        return all;
    }
}
