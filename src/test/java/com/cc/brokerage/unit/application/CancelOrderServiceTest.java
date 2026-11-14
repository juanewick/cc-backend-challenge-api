package com.cc.brokerage.unit.application;

import com.cc.brokerage.application.CancelOrderService;
import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.ListedOrder;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.OrderType;
import com.cc.brokerage.domain.model.User;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import com.cc.brokerage.support.Account10001Fixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelOrderServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private OrderRepository orders;
    @Mock
    private InstrumentRepository instruments;
    @InjectMocks
    private CancelOrderService service;

    @Test
    void cancellingOpenLomaBuyReleasesReservation() {
        Order open = Account10001Fixture.orders().get(4);
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(orders.findOrderOfUser(5L, 1L)).thenReturn(Optional.of(open));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(instruments.findInstrumentById(45L)).thenReturn(Optional.of(Account10001Fixture.LOMA));

        ListedOrder result = service.cancel("10001", 5L);
        Order cancelled = result.order();

        assertThat(result.ticker()).isEqualTo("LOMA");

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.size()).isEqualTo(50);
        assertThat(cancelled.price()).isEqualByComparingTo("710");
    }

    @Test
    void filledRejectedOrCancelledCannotBeCancelled() {
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(orders.findOrderOfUser(2L, 1L)).thenReturn(Optional.of(Account10001Fixture.orders().get(1)));

        assertThatThrownBy(() -> service.cancel("10001", 2L))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).error())
                .isEqualTo(DomainErrorCodes.ORDER_NOT_OPEN);
        verify(orders, never()).save(any());
    }

    @Test
    void orderOfAnotherAccountIsNotFound() {
        when(users.lockByAccountNumber("10002")).thenReturn(new User(2L, "jose@test.com", "10002"));
        when(orders.findOrderOfUser(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel("10002", 5L))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).error())
                .isEqualTo(DomainErrorCodes.ORDER_NOT_FOUND);
        verify(orders, never()).save(any());
    }

    @Test
    void missingOrderThrows() {
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(orders.findOrderOfUser(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel("10001", 99L))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).error())
                .isEqualTo(DomainErrorCodes.ORDER_NOT_FOUND);
    }

    @Test
    void cancellingOpenSellDoesNotChangeCash() {
        Order openSell = Account10001Fixture.order(
                20L, 47L, 10, "1000", OrderSide.SELL, OrderStatus.NEW, OrderType.LIMIT, "2023-07-14T10:00:00");
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(orders.findOrderOfUser(20L, 1L)).thenReturn(Optional.of(openSell));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(instruments.findInstrumentById(47L)).thenReturn(Optional.of(Account10001Fixture.PAMP));

        Order cancelled = service.cancel("10001", 20L).order();

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.side()).isEqualTo(OrderSide.SELL);
    }
}
