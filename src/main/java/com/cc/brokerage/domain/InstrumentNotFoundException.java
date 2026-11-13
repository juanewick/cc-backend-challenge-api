package com.cc.brokerage.domain;

public class InstrumentNotFoundException extends RuntimeException {

    public InstrumentNotFoundException() {
        super("El activo no existe");
    }
}
