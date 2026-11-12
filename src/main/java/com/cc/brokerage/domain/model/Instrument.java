package com.cc.brokerage.domain.model;

public record Instrument(Long id, String ticker, String name, InstrumentType type) {
}
