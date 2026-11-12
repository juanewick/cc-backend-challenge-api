package com.cc.brokerage.application;

import com.cc.brokerage.domain.InvalidRequestException;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentQuote;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.model.MoneyMath;
import com.cc.brokerage.domain.port.in.SearchInstrumentsUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SearchInstrumentsService implements SearchInstrumentsUseCase {

    private final InstrumentRepository instruments;
    private final MarketPriceRepository prices;

    public SearchInstrumentsService(InstrumentRepository instruments, MarketPriceRepository prices) {
        this.instruments = instruments;
        this.prices = prices;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstrumentQuote> search(String query) {
        if (query == null || query.isBlank()) {
            throw new InvalidRequestException("La búsqueda no tiene texto");
        }
        String needle = query.trim().toLowerCase();
        List<Instrument> matches = instruments.findAll().stream()
                .filter(instrument -> contains(instrument.ticker(), needle) || contains(instrument.name(), needle))
                .sorted(Comparator.comparing(Instrument::ticker))
                .toList();
        Set<Long> ids = matches.stream().map(Instrument::id).collect(Collectors.toSet());
        Map<Long, MarketPrice> latest = latestPrices(ids);
        return matches.stream()
                .map(instrument -> toQuote(instrument, latest.get(instrument.id())))
                .toList();
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private Map<Long, MarketPrice> latestPrices(Set<Long> instrumentIds) {
        Map<Long, MarketPrice> latest = new HashMap<>();
        if (instrumentIds.isEmpty()) {
            return latest;
        }
        for (MarketPrice price : prices.findByInstrumentIds(instrumentIds)) {
            MarketPrice current = latest.get(price.instrumentId());
            if (current == null
                    || price.date().isAfter(current.date())
                    || (price.date().equals(current.date()) && price.id() > current.id())) {
                latest.put(price.instrumentId(), price);
            }
        }
        return latest;
    }

    private static InstrumentQuote toQuote(Instrument instrument, MarketPrice price) {
        if (price == null || price.close() == null) {
            return new InstrumentQuote(instrument.ticker(), instrument.name(), instrument.type().name(), null, null);
        }
        var dailyReturn = price.previousClose() == null
                ? null
                : MoneyMath.percent(price.close().subtract(price.previousClose()), price.previousClose());
        return new InstrumentQuote(
                instrument.ticker(),
                instrument.name(),
                instrument.type().name(),
                MoneyMath.scaled(price.close()),
                dailyReturn
        );
    }
}
