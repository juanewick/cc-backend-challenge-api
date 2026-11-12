package com.cc.brokerage.domain.port.in;

import com.cc.brokerage.domain.model.InstrumentQuote;

import java.util.List;

public interface SearchInstrumentsUseCase {

    List<InstrumentQuote> search(String query);
}
