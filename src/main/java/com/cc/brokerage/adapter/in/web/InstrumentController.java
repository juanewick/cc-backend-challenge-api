package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.domain.port.in.SearchInstrumentsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiBase.PATH + "/v1/instruments")
public class InstrumentController {

    private final SearchInstrumentsUseCase searchInstruments;

    public InstrumentController(SearchInstrumentsUseCase searchInstruments) {
        this.searchInstruments = searchInstruments;
    }

    @GetMapping
    public List<InstrumentQuoteResponse> search(@RequestParam("q") String query) {
        return searchInstruments.search(query).stream().map(InstrumentQuoteResponse::from).toList();
    }
}
