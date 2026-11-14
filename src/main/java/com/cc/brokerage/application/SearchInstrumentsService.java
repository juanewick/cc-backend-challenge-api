package com.cc.brokerage.application;

import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentQuote;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.model.PageQuery;
import com.cc.brokerage.domain.model.PageResult;
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
    public PageResult<InstrumentQuote> search(String query, PageQuery page) {
        String text = InputText.search(query);
        PageResult<Instrument> matches = instruments.search(text, InstrumentType.MONEDA, page);
        List<Long> ids = matches.content().stream().map(Instrument::id).toList();
        Map<Long, MarketPrice> latest = ids.isEmpty() ? Map.of() : prices.findLatestByInstrumentIds(ids);
        return matches.map(instrument -> quotes.quote(instrument, latest.get(instrument.id())));
    }
}
