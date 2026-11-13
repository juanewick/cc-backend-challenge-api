package com.cc.brokerage.application;

import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentQuote;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.port.in.SearchInstrumentsUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import com.cc.brokerage.domain.service.QuoteCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SearchInstrumentsService implements SearchInstrumentsUseCase {

    /**
     * Upper bound on the instruments one search returns, so a one-letter search cannot pull the whole table.
     */
    public static final int MAX_RESULTS = 50;

    private final InstrumentRepository instruments;
    private final MarketPriceRepository prices;
    private final QuoteCalculator quotes;

    public SearchInstrumentsService(InstrumentRepository instruments, MarketPriceRepository prices, QuoteCalculator quotes) {
        this.instruments = instruments;
        this.prices = prices;
        this.quotes = quotes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstrumentQuote> search(String query) {
        String text = InputText.search(query);
        List<Instrument> matches = instruments.search(text, InstrumentType.MONEDA, MAX_RESULTS);
        if (matches.isEmpty()) {
            return List.of();
        }
        Map<Long, MarketPrice> latest = prices.findLatestByInstrumentIds(matches.stream().map(Instrument::id).toList());
        return matches.stream()
                .map(instrument -> quotes.quote(instrument, latest.get(instrument.id())))
                .toList();
    }
}
