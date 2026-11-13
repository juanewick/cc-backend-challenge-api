package com.cc.brokerage.UnitTest;

import com.cc.brokerage.application.PlaceOrderService;
import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.OrderType;
import com.cc.brokerage.domain.model.PlaceOrderCommand;
import com.cc.brokerage.support.MoneyBounds;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import com.cc.brokerage.domain.service.PortfolioCalculator;
import com.cc.brokerage.support.Account10001Fixture;
import com.cc.brokerage.support.HoldingsOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceMarketOrderServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private InstrumentRepository instruments;
    @Mock
    private MarketPriceRepository prices;
    @Mock
    private OrderRepository orders;

    private PlaceOrderService service;

    @BeforeEach
    void setUp() {
        service = new PlaceOrderService(
                users,
                instruments,
                prices,
                orders,
                new PortfolioCalculator(MoneyBounds.DEFAULT_MONEY),
                MoneyBounds.DEFAULT_MONEY
        );
    }

    @Test
    void marketBuyOfOneGgalIsFilledAtClose() {
        stubAccountAndGgal();
        when(orders.save(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            return order.withId(12L);
        });

        Order placed = service.place(new PlaceOrderCommand("10001", "GGAL", OrderSide.BUY, OrderType.MARKET, 1, null, null)).order();

        assertThat(placed.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(placed.size()).isEqualTo(1);
        assertThat(placed.price()).isEqualByComparingTo("885.80");
    }

    @Test
    void amountIsConvertedWithFloorAndDebitsShareNotional() {
        stubAccountAndGgal();
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.place(new PlaceOrderCommand("10001", "GGAL", OrderSide.BUY, OrderType.MARKET, null, new BigDecimal("2000"), null));

        verify(orders).save(captor.capture());
        assertThat(captor.getValue().size()).isEqualTo(2);
        assertThat(captor.getValue().price()).isEqualByComparingTo("885.80");
        assertThat(MoneyBounds.DEFAULT_MONEY.notional(captor.getValue().size(), captor.getValue().price())).isEqualByComparingTo("1771.60");
        assertThat(captor.getValue().status()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void marketOrderWithAPriceIsRejectedWithoutSaving() {
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "PAMP", OrderSide.SELL, OrderType.MARKET, 1, null, new BigDecimal("1.00"))))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).error())
                .isEqualTo(DomainErrorCodes.PRICE_NOT_ALLOWED);
        verify(orders, never()).save(any());
    }

    @Test
    void bothSizeAndAmountAreRejectedWithoutSaving() {
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.MARKET, 1, new BigDecimal("2000"), null)))
                .isInstanceOf(DomainException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void missingSizeAndAmountAreRejectedWithoutSaving() {
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.MARKET, null, null, null)))
                .isInstanceOf(DomainException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void unknownAccountDoesNotSave() {
        when(users.lockByAccountNumber("99999")).thenThrow(new DomainException(DomainErrorCodes.ACCOUNT_NOT_FOUND));
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "99999", "GGAL", OrderSide.BUY, OrderType.MARKET, 1, null, null)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).error())
                .isEqualTo(DomainErrorCodes.ACCOUNT_NOT_FOUND);
        verify(orders, never()).save(any());
    }

    @Test
    void unknownTickerDoesNotSave() {
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(instruments.findByTicker("ZZZZ")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "ZZZZ", OrderSide.BUY, OrderType.MARKET, 1, null, null)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).error())
                .isEqualTo(DomainErrorCodes.INSTRUMENT_NOT_FOUND);
        verify(orders, never()).save(any());
    }

    @Test
    void amountWhoseShareCountOverflowsDoesNotSave() {
        stubAccountAndGgalLenient();
        BigDecimal close = Account10001Fixture.latestPrices().get(Account10001Fixture.GGAL.id()).close();
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.MARKET, null, close.multiply(MoneyBounds.DEFAULTS.oneSharePastMax()), null)))
                .isInstanceOf(DomainException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void amountThatRoundsPastTheMaximumDoesNotSave() {
        stubAccountAndGgalLenient();
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.MARKET, null, MoneyBounds.DEFAULTS.justPastMax(), null)))
                .isInstanceOf(DomainException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void nonPositiveAmountDoesNotSave() {
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.MARKET, null, MoneyBounds.DEFAULT_MONEY.minAmount(), null)))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.MARKET, null, MoneyBounds.DEFAULTS.belowMinAmount(), null)))
                .isInstanceOf(DomainException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void amountThatBuysNoSharesIsSavedRejected() {
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(instruments.findByTicker("GGAL")).thenReturn(Optional.of(Account10001Fixture.GGAL));
        when(prices.findLatestByInstrumentIds(java.util.Set.of(34L))).thenReturn(java.util.Map.of(34L, Account10001Fixture.latestPrices().get(34L)));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order placed = service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.MARKET, null, BigDecimal.ONE, null)).order();

        assertThat(placed.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(placed.size()).isZero();
        verify(orders).save(any());
    }

    @Test
    void marketWithoutCloseIsSavedRejected() {
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(instruments.findByTicker("GGAL")).thenReturn(Optional.of(Account10001Fixture.GGAL));
        when(prices.findLatestByInstrumentIds(java.util.Set.of(34L))).thenReturn(java.util.Map.of());
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order placed = service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.MARKET, null, new BigDecimal("100"), null)).order();

        assertThat(placed.status()).isEqualTo(OrderStatus.REJECTED);
        verify(orders).save(any());
    }

    @Test
    void cashMovementsAreRejectedWithoutSaving() {
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "ARS", OrderSide.CASH_IN, OrderType.MARKET, 500, null, new BigDecimal("1"))))
                .isInstanceOf(DomainException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void placeholderTypeIsRejectedWithoutSaving() {
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.UNKNOWN, 5, null, null)))
                .isInstanceOf(DomainException.class)
                .extracting(thrown -> ((DomainException) thrown).error())
                .isEqualTo(DomainErrorCodes.INVALID_TYPE);
        verify(orders, never()).save(any());
    }

    private void stubAccount() {
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(orders.summarizeHoldings(1L)).thenReturn(HoldingsOf.orders(Account10001Fixture.orders()));
    }

    private void stubAccountAndGgal() {
        stubAccount();
        when(instruments.findByTicker("GGAL")).thenReturn(Optional.of(Account10001Fixture.GGAL));
        when(prices.findLatestByInstrumentIds(java.util.Set.of(34L))).thenReturn(java.util.Map.of(34L, Account10001Fixture.latestPrices().get(34L)));
    }

    private void stubAccountAndGgalLenient() {
        lenient().when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        lenient().when(orders.summarizeHoldings(1L)).thenReturn(HoldingsOf.orders(Account10001Fixture.orders()));
        lenient().when(instruments.findByTicker("GGAL")).thenReturn(Optional.of(Account10001Fixture.GGAL));
        lenient().when(prices.findLatestByInstrumentIds(java.util.Set.of(34L))).thenReturn(java.util.Map.of(34L, Account10001Fixture.latestPrices().get(34L)));
    }
}
