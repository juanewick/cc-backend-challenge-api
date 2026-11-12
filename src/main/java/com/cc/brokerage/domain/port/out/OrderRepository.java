package com.cc.brokerage.domain.port.out;

import com.cc.brokerage.domain.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    List<Order> findByUserId(Long userId);

    Optional<Order> findOrderById(Long id);

    Order save(Order order);
}
