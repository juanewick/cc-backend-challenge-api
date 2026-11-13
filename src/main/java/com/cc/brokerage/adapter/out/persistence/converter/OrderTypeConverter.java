package com.cc.brokerage.adapter.out.persistence.converter;

import com.cc.brokerage.domain.model.OrderType;
import jakarta.persistence.Converter;

@Converter
public class OrderTypeConverter extends TolerantEnumConverter<OrderType> {

    public OrderTypeConverter() {
        super(OrderType.class, OrderType.UNKNOWN);
    }
}
