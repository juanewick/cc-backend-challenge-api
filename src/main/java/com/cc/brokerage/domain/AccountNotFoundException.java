package com.cc.brokerage.domain;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountNumber) {
        super("La cuenta no existe: " + accountNumber);
    }
}
