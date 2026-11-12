package com.cc.brokerage.domain;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super("La orden no existe: " + orderId);
    }
}
