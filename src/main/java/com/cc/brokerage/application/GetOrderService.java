package com.cc.brokerage.application;

import com.cc.brokerage.domain.AccountNotFoundException;
import com.cc.brokerage.domain.InstrumentNotFoundException;
import com.cc.brokerage.domain.OrderConflictException;
import com.cc.brokerage.domain.OrderNotFoundException;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.ListedOrder;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.User;
import com.cc.brokerage.domain.port.in.GetOrderUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOrderService implements GetOrderUseCase {

    private final UserRepository users;
    private final OrderRepository orders;
    private final InstrumentRepository instruments;

    public GetOrderService(UserRepository users, OrderRepository orders, InstrumentRepository instruments) {
        this.users = users;
        this.orders = orders;
        this.instruments = instruments;
    }

    @Override
    @Transactional(readOnly = true)
    public ListedOrder get(String accountNumber, long orderId) {
        String account = InputText.account(accountNumber);
        User user = users.findByAccountNumber(account).orElseThrow(AccountNotFoundException::new);
        Order order = orders.findOrderById(orderId).orElseThrow(OrderNotFoundException::new);
        if (!order.userId().equals(user.id())) {
            throw new OrderConflictException("La orden no pertenece a esa cuenta");
        }
        String ticker = instruments.findInstrumentById(order.instrumentId())
                .map(Instrument::ticker)
                .orElseThrow(InstrumentNotFoundException::new);
        return new ListedOrder(order, ticker);
    }
}
