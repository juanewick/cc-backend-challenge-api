package com.cc.brokerage.domain.port.out;

import com.cc.brokerage.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByAccountNumber(String accountNumber);

    User lockByAccountNumber(String accountNumber);
}
