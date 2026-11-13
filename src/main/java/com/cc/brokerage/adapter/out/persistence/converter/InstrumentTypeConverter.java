package com.cc.brokerage.adapter.out.persistence.converter;

import com.cc.brokerage.domain.model.InstrumentType;
import jakarta.persistence.Converter;

@Converter
public class InstrumentTypeConverter extends TolerantEnumConverter<InstrumentType> {

    public InstrumentTypeConverter() {
        super(InstrumentType.class, InstrumentType.UNKNOWN);
    }
}
