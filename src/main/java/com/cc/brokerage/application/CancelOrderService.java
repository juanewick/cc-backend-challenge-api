package com.cc.brokerage.application;

import com.cc.brokerage.domain.OrderConflictException;
import com.cc.brokerage.domain.OrderNotFoundException;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.User;
import com.cc.brokerage.domain.port.in.CancelOrderUseCase;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelOrderService implements CancelOrderUseCase {

    private final UserRepository users;
    private final OrderRepository orders;

    public CancelOrderService(UserRepository users, OrderRepository orders) {
        this.users = users;
        this.orders = orders;
    }

    @Override
    @Transactional
    public Order cancel(String accountNumber, Long orderId) {
        String account = InputText.account(accountNumber);
        User user = users.lockByAccountNumber(account);
        Order order = orders.findOrderById(orderId).orElseThrow(OrderNotFoundException::new);
        if (!order.userId().equals(user.id())) {
            throw new OrderConflictException("La orden no pertenece a esa cuenta");
        }
        if (order.status() != OrderStatus.NEW) {
            throw new OrderConflictException("Solo se cancelan órdenes abiertas");
        }
        Order cancelled = order.withStatus(OrderStatus.CANCELLED);
        return orders.save(cancelled);
    }
}
