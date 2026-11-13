package com.cc.brokerage.domain;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException() {
        super("La orden no existe");
    }
}
