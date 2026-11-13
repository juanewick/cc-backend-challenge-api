package com.cc.brokerage.domain.service;

import com.cc.brokerage.domain.model.MarketPrice;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class LatestPrices {

    private LatestPrices() {
    }

    public static Map<Long, MarketPrice> byInstrument(Collection<MarketPrice> prices) {
        Map<Long, MarketPrice> latest = new HashMap<>();
        if (prices == null || prices.isEmpty()) {
            return latest;
        }
        for (MarketPrice price : prices) {
            MarketPrice current = latest.get(price.instrumentId());
            if (current == null || newer(price, current)) {
                latest.put(price.instrumentId(), price);
            }
        }
        return latest;
    }

    public static Optional<MarketPrice> one(Collection<MarketPrice> prices) {
        return byInstrument(prices).values().stream().findFirst();
    }

    private static boolean newer(MarketPrice candidate, MarketPrice current) {
        if (candidate.date() == null) {
            return false;
        }
        if (current.date() == null || candidate.date().isAfter(current.date())) {
            return true;
        }
        return candidate.date().equals(current.date()) && candidate.id() > current.id();
    }
}
