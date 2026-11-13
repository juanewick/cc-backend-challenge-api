package com.cc.brokerage.adapter.out.persistence.repository;

import com.cc.brokerage.adapter.out.persistence.entity.InstrumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstrumentJpaRepository extends JpaRepository<InstrumentEntity, Long> {

    Optional<InstrumentEntity> findByTickerIgnoreCase(String ticker);
}
