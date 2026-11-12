package com.cc.brokerage.domain.port.out;

import com.cc.brokerage.domain.model.MarketPrice;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MarketPriceRepository {

    Optional<MarketPrice> findLatestByInstrumentId(Long instrumentId);

    List<MarketPrice> findByInstrumentIds(Collection<Long> instrumentIds);
}
