package com.cc.brokerage.application;

import com.cc.brokerage.domain.InstrumentNotFoundException;
import com.cc.brokerage.domain.InvalidRequestException;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentQuote;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.port.in.GetInstrumentQuoteUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import com.cc.brokerage.domain.service.LatestPrices;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class GetInstrumentQuoteService implements GetInstrumentQuoteUseCase {

    private final InstrumentRepository instruments;
    private final MarketPriceRepository prices;

    public GetInstrumentQuoteService(InstrumentRepository instruments, MarketPriceRepository prices) {
        this.instruments = instruments;
        this.prices = prices;
    }

    @Override
    @Transactional(readOnly = true)
    public InstrumentQuote quote(String ticker) {
        String symbol = InputText.ticker(ticker);
        Instrument instrument = instruments.findByTicker(symbol).orElseThrow(InstrumentNotFoundException::new);
        if (instrument.type() == InstrumentType.MONEDA) {
            throw new InvalidRequestException("La moneda no es operable");
        }
        var latest = LatestPrices.one(prices.findByInstrumentIds(Set.of(instrument.id()))).orElse(null);
        return SearchInstrumentsService.toQuote(instrument, latest);
    }
}
