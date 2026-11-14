package com.cc.brokerage.adapter.out.persistence.converter;

import com.cc.brokerage.domain.model.OrderStatus;
import jakarta.persistence.Converter;

@Converter
public class OrderStatusConverter extends TolerantEnumConverter<OrderStatus> {

    public OrderStatusConverter() {
        super(OrderStatus.class, OrderStatus.UNKNOWN);
    }
}
