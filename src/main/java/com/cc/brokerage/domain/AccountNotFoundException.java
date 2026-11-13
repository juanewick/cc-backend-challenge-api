package com.cc.brokerage.domain;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException() {
        super("La cuenta no existe");
    }
}
