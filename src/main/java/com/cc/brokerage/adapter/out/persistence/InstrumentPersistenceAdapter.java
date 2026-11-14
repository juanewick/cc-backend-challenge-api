package com.cc.brokerage.adapter.out.persistence;

import com.cc.brokerage.adapter.out.persistence.entity.InstrumentEntity;
import com.cc.brokerage.adapter.out.persistence.repository.InstrumentJpaRepository;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.model.PageQuery;
import com.cc.brokerage.domain.model.PageResult;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InstrumentPersistenceAdapter implements InstrumentRepository {

    private final InstrumentJpaRepository instruments;

    public InstrumentPersistenceAdapter(InstrumentJpaRepository instruments) {
        this.instruments = instruments;
    }

    @Override
    public Optional<Instrument> findByTicker(String ticker) {
        return instruments.findByTickerIgnoreCase(ticker).map(InstrumentPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Instrument> findInstrumentById(Long id) {
        return id == null ? Optional.empty() : instruments.findById(id).map(InstrumentPersistenceAdapter::toDomain);
    }

    @Override
    public Map<Long, Instrument> findByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return instruments.findAllById(ids).stream()
                .map(InstrumentPersistenceAdapter::toDomain)
                .collect(Collectors.toUnmodifiableMap(Instrument::id, Function.identity()));
    }

    @Override
    public PageResult<Instrument> search(String text, InstrumentType excludedType, PageQuery page) {
        Page<InstrumentEntity> found = instruments.search(containsPattern(text), excludedType, PageMapping.toRequest(page));
        return PageMapping.toResult(found, page, InstrumentPersistenceAdapter::toDomain);
    }

    /**
     * A LIKE pattern that matches the text anywhere, with %, _ and the escape character taken literally.
     */
    public static String containsPattern(String text) {
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    private static Instrument toDomain(InstrumentEntity entity) {
        return new Instrument(entity.getId(), entity.getTicker(), entity.getName(), entity.getType());
    }
}
