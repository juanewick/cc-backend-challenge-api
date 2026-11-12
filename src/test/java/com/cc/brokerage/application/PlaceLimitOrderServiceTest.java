package com.cc.brokerage.application;

import com.cc.brokerage.domain.InvalidRequestException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceLimitOrderServiceTest {

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
    void limitBuyOfTenGgalStaysNewAndReservesNotional() {
        stubAccountAndGgal();
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.LIMIT, 10, null, new BigDecimal("800.00")));

        verify(orders).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.status()).isEqualTo(OrderStatus.NEW);
        assertThat(saved.size()).isEqualTo(10);
        assertThat(saved.price()).isEqualByComparingTo("800.00");
        assertThat(saved.notional()).isEqualByComparingTo("8000.00");

        var before = calculator.summarize(Account10001Fixture.orders());
        var after = calculator.summarize(withSaved(saved));
        assertThat(after.settledCash()).isEqualByComparingTo(before.settledCash());
        assertThat(after.availableCash()).isEqualByComparingTo(before.availableCash().subtract(new BigDecimal("8000.00")));
    }

    @Test
    void missingLimitPriceDoesNotSave() {
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.LIMIT, 10, null, null)))
                .isInstanceOf(InvalidRequestException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void nonPositiveLimitPriceDoesNotSave() {
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.LIMIT, 10, null, BigDecimal.ZERO)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.LIMIT, 10, null, new BigDecimal("-1.00"))))
                .isInstanceOf(InvalidRequestException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void limitAmountUsesFloorAndReservesShareNotional() {
        stubAccountAndGgal();
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.LIMIT, null, new BigDecimal("2500"), new BigDecimal("800.00")));

        verify(orders).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.status()).isEqualTo(OrderStatus.NEW);
        assertThat(saved.size()).isEqualTo(3);
        assertThat(saved.notional()).isEqualByComparingTo("2400.00");

        var before = calculator.summarize(Account10001Fixture.orders());
        var after = calculator.summarize(withSaved(saved));
        assertThat(after.availableCash()).isEqualByComparingTo(before.availableCash().subtract(new BigDecimal("2400.00")));
    }

    @Test
    void limitStaysNewAtSubmittedPriceEvenWhenCloseDiffers() {
        stubAccountAndGgal();
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order placed = service.place(new PlaceOrderCommand(
                "10001", "GGAL", OrderSide.BUY, OrderType.LIMIT, 10, null, new BigDecimal("800.00")));

        assertThat(placed.status()).isEqualTo(OrderStatus.NEW);
        assertThat(placed.price()).isEqualByComparingTo("800.00");
        assertThat(placed.type()).isEqualTo(OrderType.LIMIT);
    }

    @Test
    void limitSellReservesSharesAndDoesNotChangeCash() {
        stubAccount();
        when(instruments.findByTicker("PAMP")).thenReturn(Optional.of(Account10001Fixture.PAMP));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order placed = service.place(new PlaceOrderCommand(
                "10001", "PAMP", OrderSide.SELL, OrderType.LIMIT, 10, null, new BigDecimal("1000.00")));

        assertThat(placed.status()).isEqualTo(OrderStatus.NEW);
        assertThat(placed.size()).isEqualTo(10);
        assertThat(placed.price()).isEqualByComparingTo("1000.00");

        var before = calculator.summarize(Account10001Fixture.orders());
        var after = calculator.summarize(withSaved(placed));
        assertThat(after.settledCash()).isEqualByComparingTo(before.settledCash());
        assertThat(after.availableCash()).isEqualByComparingTo(before.availableCash());
        assertThat(calculator.availableShares(after, Account10001Fixture.PAMP.id()))
                .isEqualTo(calculator.availableShares(before, Account10001Fixture.PAMP.id()) - 10);
    }

    private void stubAccount() {
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(orders.findByUserId(1L)).thenReturn(Account10001Fixture.orders());
    }

    private void stubAccountAndGgal() {
        stubAccount();
        when(instruments.findByTicker("GGAL")).thenReturn(Optional.of(Account10001Fixture.GGAL));
    }

    private static List<Order> withSaved(Order saved) {
        List<Order> orders = new ArrayList<>(Account10001Fixture.orders());
        orders.add(saved);
        return orders;
    }
}
