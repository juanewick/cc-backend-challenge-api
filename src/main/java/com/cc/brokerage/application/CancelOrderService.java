package com.cc.brokerage.application;

import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.ListedOrder;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.User;
import com.cc.brokerage.domain.port.in.CancelOrderUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelOrderService implements CancelOrderUseCase {

    private final UserRepository users;
    private final OrderRepository orders;
    private final InstrumentRepository instruments;

    public CancelOrderService(UserRepository users, OrderRepository orders, InstrumentRepository instruments) {
        this.users = users;
        this.orders = orders;
        this.instruments = instruments;
    }

    @Override
    @Transactional
    public ListedOrder cancel(String accountNumber, long orderId) {
        String account = InputText.account(accountNumber);
        User user = users.lockByAccountNumber(account);
        Order order = orders.findOrderOfUser(orderId, user.id())
                .orElseThrow(() -> new DomainException(DomainErrorCodes.ORDER_NOT_FOUND));
        if (order.status() != OrderStatus.NEW) {
            throw new DomainException(DomainErrorCodes.ORDER_NOT_OPEN);
        }
        String ticker = instruments.findInstrumentById(order.instrumentId())
                .map(Instrument::ticker)
                .orElseThrow(() -> new DomainException(DomainErrorCodes.INSTRUMENT_NOT_FOUND));
        return new ListedOrder(orders.save(order.withStatus(OrderStatus.CANCELLED)), ticker);
    }
}
