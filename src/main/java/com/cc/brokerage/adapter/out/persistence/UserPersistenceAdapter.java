package com.cc.brokerage.adapter.out.persistence;

import com.cc.brokerage.adapter.out.persistence.entity.UserEntity;
import com.cc.brokerage.adapter.out.persistence.repository.UserJpaRepository;
import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.User;
import com.cc.brokerage.domain.port.out.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository users;

    public UserPersistenceAdapter(UserJpaRepository users) {
        this.users = users;
    }

    @Override
    public Optional<User> findByAccountNumber(String accountNumber) {
        return users.findByAccountNumber(accountNumber).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public User lockByAccountNumber(String accountNumber) {
        return users.lockByAccountNumber(accountNumber)
                .map(UserPersistenceAdapter::toDomain)
                .orElseThrow(() -> new DomainException(DomainErrorCodes.ACCOUNT_NOT_FOUND));
    }

    private static User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getEmail(), entity.getAccountNumber());
    }
}
