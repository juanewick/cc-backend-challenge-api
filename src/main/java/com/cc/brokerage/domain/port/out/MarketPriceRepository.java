package com.cc.brokerage.domain.port.out;

import com.cc.brokerage.domain.model.MarketPrice;

import java.util.Collection;
import java.util.Map;

public interface MarketPriceRepository {

    /**
     * The most recent price row of each instrument: latest date first, then highest id. Instruments without
     * prices are absent from the map.
     */
    Map<Long, MarketPrice> findLatestByInstrumentIds(Collection<Long> instrumentIds);
}
