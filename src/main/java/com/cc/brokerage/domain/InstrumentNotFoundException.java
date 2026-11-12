package com.cc.brokerage.domain;

public class InstrumentNotFoundException extends RuntimeException {

    public InstrumentNotFoundException(String ticker) {
        super("El activo no existe: " + ticker);
    }
}
