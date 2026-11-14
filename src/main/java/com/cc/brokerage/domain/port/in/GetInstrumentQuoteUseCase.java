package com.cc.brokerage.domain.port.in;

import com.cc.brokerage.domain.model.InstrumentQuote;

public interface GetInstrumentQuoteUseCase {

    InstrumentQuote quote(String ticker);
}
