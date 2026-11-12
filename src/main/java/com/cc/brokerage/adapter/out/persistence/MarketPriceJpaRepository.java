package com.cc.brokerage.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MarketPriceJpaRepository extends JpaRepository<MarketPriceEntity, Long> {

    Optional<MarketPriceEntity> findFirstByInstrumentIdOrderByDateDescIdDesc(Long instrumentId);

    List<MarketPriceEntity> findByInstrumentIdIn(Collection<Long> instrumentIds);
}
