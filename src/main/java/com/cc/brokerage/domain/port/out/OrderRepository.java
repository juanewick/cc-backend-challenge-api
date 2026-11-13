package com.cc.brokerage.domain.port.out;

import com.cc.brokerage.domain.model.InstrumentHoldings;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.PageQuery;
import com.cc.brokerage.domain.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    /**
     * Orders of a user, oldest first (orders without a date last, then by id), optionally with one status.
     */
    PageResult<Order> findPageByUserId(Long userId, OrderStatus status, PageQuery page);

    /**
     * One row per instrument with the FILLED and NEW orders of the user already added up. Orders in any
     * other status, or with a side this service does not know, are left out.
     */
    List<InstrumentHoldings> summarizeHoldings(Long userId);

    Optional<Order> findOrderById(Long id);

    Order save(Order order);
}
