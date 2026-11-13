package com.cc.brokerage.application;

import com.cc.brokerage.domain.AccountNotFoundException;
import com.cc.brokerage.domain.InstrumentNotFoundException;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.ListedOrder;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.User;
import com.cc.brokerage.domain.port.in.ListOrdersUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ListOrdersService implements ListOrdersUseCase {

    private final UserRepository users;
    private final OrderRepository orders;
    private final InstrumentRepository instruments;

    public ListOrdersService(UserRepository users, OrderRepository orders, InstrumentRepository instruments) {
        this.users = users;
        this.orders = orders;
        this.instruments = instruments;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListedOrder> list(String accountNumber) {
        String account = InputText.account(accountNumber);
        User user = users.findByAccountNumber(account).orElseThrow(AccountNotFoundException::new);
        return orders.findByUserId(user.id()).stream()
                .sorted(Comparator.comparing(Order::datetime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Order::id, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(order -> new ListedOrder(order, tickerOf(order)))
                .toList();
    }

    private String tickerOf(Order order) {
        return instruments.findInstrumentById(order.instrumentId())
                .map(Instrument::ticker)
                .orElseThrow(InstrumentNotFoundException::new);
    }
}
