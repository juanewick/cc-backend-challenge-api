package com.cc.brokerage.adapter.out.persistence;

import com.cc.brokerage.domain.AccountNotFoundException;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.User;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class PersistenceAdapter implements UserRepository, InstrumentRepository, MarketPriceRepository, OrderRepository {

    private final UserJpaRepository users;
    private final InstrumentJpaRepository instruments;
    private final MarketPriceJpaRepository prices;
    private final OrderJpaRepository orders;

    public PersistenceAdapter(
            UserJpaRepository users,
            InstrumentJpaRepository instruments,
            MarketPriceJpaRepository prices,
            OrderJpaRepository orders
    ) {
        this.users = users;
        this.instruments = instruments;
        this.prices = prices;
        this.orders = orders;
    }

    @Override
    public Optional<User> findByAccountNumber(String accountNumber) {
        return users.findByAccountNumber(accountNumber).map(this::toDomain);
    }

    @Override
    public User lockByAccountNumber(String accountNumber) {
        return users.lockByAccountNumber(accountNumber)
                .map(this::toDomain)
                .orElseThrow(AccountNotFoundException::new);
    }

    @Override
    public Optional<Instrument> findByTicker(String ticker) {
        return instruments.findByTicker(ticker).map(this::toDomain);
    }

    @Override
    public Optional<Instrument> findInstrumentById(Long id) {
        return instruments.findById(id).map(this::toDomain);
    }

    @Override
    public List<Instrument> findAll() {
        return instruments.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<MarketPrice> findLatestByInstrumentId(Long instrumentId) {
        return prices.findFirstByInstrumentIdOrderByDateDescIdDesc(instrumentId).map(this::toDomain);
    }

    @Override
    public List<MarketPrice> findByInstrumentIds(Collection<Long> instrumentIds) {
        if (instrumentIds.isEmpty()) {
            return List.of();
        }
        return prices.findByInstrumentIdIn(instrumentIds).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return orders.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Order> findOrderById(Long id) {
        return orders.findById(id).map(this::toDomain);
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        return toDomain(orders.save(entity));
    }

    private User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getEmail(), entity.getAccountNumber());
    }

    private Instrument toDomain(InstrumentEntity entity) {
        return new Instrument(entity.getId(), entity.getTicker(), entity.getName(), entity.getType());
    }

    private MarketPrice toDomain(MarketPriceEntity entity) {
        return new MarketPrice(
                entity.getId(),
                entity.getInstrumentId(),
                entity.getHigh(),
                entity.getLow(),
                entity.getOpen(),
                entity.getClose(),
                entity.getPreviousClose(),
                entity.getDate()
        );
    }

    private Order toDomain(OrderEntity entity) {
        return new Order(
                entity.getId(),
                entity.getInstrumentId(),
                entity.getUserId(),
                entity.getSize(),
                entity.getPrice(),
                entity.getType(),
                entity.getSide(),
                entity.getStatus(),
                entity.getDatetime()
        );
    }

    private OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.id());
        entity.setInstrumentId(order.instrumentId());
        entity.setUserId(order.userId());
        entity.setSize(order.size());
        entity.setPrice(order.price());
        entity.setType(order.type());
        entity.setSide(order.side());
        entity.setStatus(order.status());
        entity.setDatetime(order.datetime());
        return entity;
    }
}
