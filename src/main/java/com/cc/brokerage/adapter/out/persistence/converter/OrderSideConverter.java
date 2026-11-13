package com.cc.brokerage.adapter.out.persistence.converter;

import com.cc.brokerage.domain.model.OrderSide;
import jakarta.persistence.Converter;

@Converter
public class OrderSideConverter extends TolerantEnumConverter<OrderSide> {

    public OrderSideConverter() {
        super(OrderSide.class, OrderSide.UNKNOWN);
    }
}
