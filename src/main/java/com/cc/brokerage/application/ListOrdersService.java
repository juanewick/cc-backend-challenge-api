package com.cc.brokerage.application;

import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.ListedOrder;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.PageQuery;
import com.cc.brokerage.domain.model.PageResult;
import com.cc.brokerage.domain.model.User;
import com.cc.brokerage.domain.port.in.ListOrdersUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
public class ListOrdersService implements ListOrdersUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListOrdersService.class);

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
    public PageResult<ListedOrder> list(String accountNumber, OrderStatus status, PageQuery page) {
        String account = InputText.account(accountNumber);
        User user = users.findByAccountNumber(account).orElseThrow(() -> new DomainException(DomainErrorCodes.ACCOUNT_NOT_FOUND));
        PageResult<Order> found = orders.findPageByUserId(user.id(), status, page);
        Map<Long, Instrument> byId = instruments.findByIds(found.content().stream()
                .map(Order::instrumentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        log.debug("Orders of user {} listed: status {}, page {}, {} of {} returned",
                user.id(), status, page.page(), found.content().size(), found.totalElements());
        return found.map(order -> new ListedOrder(order, tickerOf(order, byId)));
    }

    private static String tickerOf(Order order, Map<Long, Instrument> byId) {
        Instrument instrument = order.instrumentId() == null ? null : byId.get(order.instrumentId());
        if (instrument == null) {
            log.warn("Order {} points to missing instrument {}", order.id(), order.instrumentId());
            throw new DomainException(DomainErrorCodes.INSTRUMENT_NOT_FOUND);
        }
        return instrument.ticker();
    }
}
