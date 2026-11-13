package com.cc.brokerage.adapter.out.persistence;

import com.cc.brokerage.adapter.out.persistence.entity.OrderEntity;
import com.cc.brokerage.adapter.out.persistence.repository.OrderJpaRepository;
import com.cc.brokerage.adapter.out.persistence.repository.OrderJpaRepository.HoldingsRow;
import com.cc.brokerage.domain.model.InstrumentHoldings;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.OrderType;
import com.cc.brokerage.domain.model.PageQuery;
import com.cc.brokerage.domain.model.PageResult;
import com.cc.brokerage.domain.port.out.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OrderPersistenceAdapter implements OrderRepository {

    private final OrderJpaRepository orders;

    public OrderPersistenceAdapter(OrderJpaRepository orders) {
        this.orders = orders;
    }

    @Override
    public PageResult<Order> findPageByUserId(Long userId, OrderStatus status, PageQuery page) {
        PageRequest request = PageRequest.of(page.page(), page.size());
        Page<OrderEntity> found = status == null
                ? orders.findPageByUserId(userId, request)
                : orders.findPageByUserIdAndStatus(userId, status, request);
        return new PageResult<>(
                found.getContent().stream().map(OrderPersistenceAdapter::toDomain).toList(),
                page.page(),
                page.size(),
                found.getTotalElements()
        );
    }

    @Override
    public List<InstrumentHoldings> summarizeHoldings(Long userId) {
        return orders.summarizeHoldings(userId).stream()
                .map(row -> toHoldings(userId, row))
                .toList();
    }

    @Override
    public Optional<Order> findOrderOfUser(Long id, Long userId) {
        return orders.findByIdAndUserId(id, userId).map(OrderPersistenceAdapter::toDomain);
    }

    @Override
    public Order save(Order order) {
        if (order.side() == OrderSide.UNKNOWN || order.type() == OrderType.UNKNOWN || order.status() == OrderStatus.UNKNOWN) {
            // The placeholder stands for a value this service could not read; storing it would overwrite the original.
            throw new IllegalStateException("Refusing to store order " + order.id() + " with a placeholder side, type or status");
        }
        return toDomain(orders.save(toEntity(order)));
    }

    private static InstrumentHoldings toHoldings(Long userId, HoldingsRow row) {
        if (row.getUnpricedOrders() > 0) {
            throw new IllegalStateException("User " + userId + " has " + row.getUnpricedOrders()
                    + " filled or open orders without price on instrument " + row.getInstrumentId());
        }
        return new InstrumentHoldings(
                row.getInstrumentId(),
                row.getNetQuantity(),
                row.getNetInvested(),
                row.getReservedSellQuantity(),
                row.getReservedBuyAmount(),
                row.getCashFlow(),
                row.getBoughtQuantity(),
                row.getBoughtAmount(),
                row.getSoldQuantity(),
                row.getSoldAmount()
        );
    }

    private static Order toDomain(OrderEntity entity) {
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

    private static OrderEntity toEntity(Order order) {
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
