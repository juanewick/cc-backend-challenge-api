package com.cc.brokerage.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstrumentJpaRepository extends JpaRepository<InstrumentEntity, Long> {

    Optional<InstrumentEntity> findByTicker(String ticker);
}
