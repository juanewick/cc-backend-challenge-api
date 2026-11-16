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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelOrderService implements CancelOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelOrderService.class);

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
            log.info("Order {} of user {} not cancelled: status is {}", orderId, user.id(), order.status());
            throw new DomainException(DomainErrorCodes.ORDER_NOT_OPEN);
        }
        String ticker = instruments.findInstrumentById(order.instrumentId())
                .map(Instrument::ticker)
                .orElseThrow(() -> {
                    log.warn("Order {} points to missing instrument {}", order.id(), order.instrumentId());
                    return new DomainException(DomainErrorCodes.INSTRUMENT_NOT_FOUND);
                });
        Order cancelled = orders.save(order.withStatus(OrderStatus.CANCELLED));
        log.info("Order {} of user {} cancelled ({})", orderId, user.id(), ticker);
        return new ListedOrder(cancelled, ticker);
    }
}
