package com.cc.brokerage.domain.port.in;

import com.cc.brokerage.domain.model.InstrumentQuote;
import com.cc.brokerage.domain.model.PageQuery;
import com.cc.brokerage.domain.model.PageResult;


public interface SearchInstrumentsUseCase {

    PageResult<InstrumentQuote> search(String query, PageQuery page);
}
