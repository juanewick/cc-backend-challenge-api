package com.cc.brokerage.adapter.out.persistence;

import com.cc.brokerage.adapter.out.persistence.entity.MarketPriceEntity;
import com.cc.brokerage.adapter.out.persistence.repository.MarketPriceJpaRepository;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MarketPricePersistenceAdapter implements MarketPriceRepository {

    private final MarketPriceJpaRepository prices;

    public MarketPricePersistenceAdapter(MarketPriceJpaRepository prices) {
        this.prices = prices;
    }

    @Override
    public Map<Long, MarketPrice> findLatestByInstrumentIds(Collection<Long> instrumentIds) {
        if (instrumentIds.isEmpty()) {
            return Map.of();
        }
        return prices.findLatestByInstrumentIdIn(instrumentIds).stream()
                .map(MarketPricePersistenceAdapter::toDomain)
                .collect(Collectors.toUnmodifiableMap(MarketPrice::instrumentId, Function.identity()));
    }

    private static MarketPrice toDomain(MarketPriceEntity entity) {
        return new MarketPrice(
                entity.getId(),
                entity.getInstrumentId(),
                entity.getHigh(),
                entity.getLow(),
                entity.getOpen(),
                entity.getClose(),
                entity.getPreviousClose(),
                entity.getDate()
        );
    }
}
