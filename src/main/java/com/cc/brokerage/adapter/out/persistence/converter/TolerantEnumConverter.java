package com.cc.brokerage.adapter.out.persistence.converter;

import jakarta.persistence.AttributeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The database is owned by another system, so a column can hold a value this service does not know yet.
 * Reading maps it to the fallback instead of failing the whole query.
 */
public abstract class TolerantEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private static final Logger log = LoggerFactory.getLogger(TolerantEnumConverter.class);

    private final Class<E> type;
    private final E fallback;

    protected TolerantEnumConverter(Class<E> type, E fallback) {
        this.type = type;
        this.fallback = fallback;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        // Hibernate maps every constant at startup, the fallback included, so this cannot refuse it.
        // PersistenceAdapter.save is what keeps the fallback out of the table.
        return value == null ? null : value.name();
    }

    @Override
    public E convertToEntityAttribute(String column) {
        if (column == null) {
            return null;
        }
        if (!column.equals(fallback.name())) {
            try {
                return Enum.valueOf(type, column);
            } catch (IllegalArgumentException unknown) {
                // falls through to the warning below
            }
        }
        log.warn("Unknown {} value read from the database: '{}'", type.getSimpleName(), column);
        return fallback;
    }
}
