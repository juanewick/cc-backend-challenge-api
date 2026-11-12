package com.cc.brokerage.domain.port.out;

import com.cc.brokerage.domain.model.Instrument;

import java.util.List;
import java.util.Optional;

public interface InstrumentRepository {

    Optional<Instrument> findByTicker(String ticker);

    Optional<Instrument> findInstrumentById(Long id);

    List<Instrument> findAll();
}
