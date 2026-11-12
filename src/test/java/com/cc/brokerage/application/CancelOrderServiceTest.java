package com.cc.brokerage.application;

import com.cc.brokerage.domain.OrderConflictException;
import com.cc.brokerage.domain.OrderNotFoundException;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.OrderType;
import com.cc.brokerage.domain.model.User;
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
    @InjectMocks
    private CancelOrderService service;

    @Test
    void cancellingOpenLomaBuyReleasesReservation() {
        Order open = Account10001Fixture.orders().get(4);
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(orders.findOrderById(5L)).thenReturn(Optional.of(open));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order cancelled = service.cancel("10001", 5L);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.size()).isEqualTo(50);
        assertThat(cancelled.price()).isEqualByComparingTo("710");
    }

    @Test
    void filledRejectedOrCancelledCannotBeCancelled() {
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(orders.findOrderById(2L)).thenReturn(Optional.of(Account10001Fixture.orders().get(1)));

        assertThatThrownBy(() -> service.cancel("10001", 2L))
                .isInstanceOf(OrderConflictException.class)
                .hasMessageContaining("abiertas");
        verify(orders, never()).save(any());
    }

    @Test
    void otherAccountCannotCancel() {
        when(users.lockByAccountNumber("10002")).thenReturn(new User(2L, "jose@test.com", "10002"));
        when(orders.findOrderById(5L)).thenReturn(Optional.of(Account10001Fixture.orders().get(4)));

        assertThatThrownBy(() -> service.cancel("10002", 5L))
                .isInstanceOf(OrderConflictException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void missingOrderThrows() {
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(orders.findOrderById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel("10001", 99L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cancellingOpenSellDoesNotChangeCash() {
        Order openSell = Account10001Fixture.order(
                20L, 47L, 10, "1000", OrderSide.SELL, OrderStatus.NEW, OrderType.LIMIT, "2023-07-14T10:00:00");
        when(users.lockByAccountNumber("10001")).thenReturn(Account10001Fixture.USER);
        when(orders.findOrderById(20L)).thenReturn(Optional.of(openSell));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order cancelled = service.cancel("10001", 20L);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.side()).isEqualTo(OrderSide.SELL);
    }
}
