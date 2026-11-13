package com.cc.brokerage.domain.port.out;

import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface InstrumentRepository {

    Optional<Instrument> findByTicker(String ticker);

    Optional<Instrument> findInstrumentById(Long id);

    /**
     * Instruments by id in one round trip. Ids that do not exist are absent from the map.
     */
    Map<Long, Instrument> findByIds(Collection<Long> ids);

    /**
     * Instruments whose ticker or name contain the text, ignoring case, ordered by ticker. The text is matched
     * literally: wildcard characters in it have no special meaning.
     */
    List<Instrument> search(String text, InstrumentType excludedType, int limit);
}
