package com.cc.brokerage.application;

import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentQuote;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.port.in.GetInstrumentQuoteUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import com.cc.brokerage.domain.service.QuoteCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class GetInstrumentQuoteService implements GetInstrumentQuoteUseCase {

    private final InstrumentRepository instruments;
    private final MarketPriceRepository prices;
    private final QuoteCalculator quotes;

    public GetInstrumentQuoteService(InstrumentRepository instruments, MarketPriceRepository prices, QuoteCalculator quotes) {
        this.instruments = instruments;
        this.prices = prices;
        this.quotes = quotes;
    }

    @Override
    @Transactional(readOnly = true)
    public InstrumentQuote quote(String ticker) {
        String symbol = InputText.ticker(ticker);
        Instrument instrument = instruments.findByTicker(symbol).orElseThrow(() -> new DomainException(DomainErrorCodes.INSTRUMENT_NOT_FOUND));
        if (instrument.type() == InstrumentType.MONEDA) {
            throw new DomainException(DomainErrorCodes.INSTRUMENT_NOT_TRADABLE);
        }
        return quotes.quote(instrument, prices.findLatestByInstrumentIds(Set.of(instrument.id())).get(instrument.id()));
    }
}
