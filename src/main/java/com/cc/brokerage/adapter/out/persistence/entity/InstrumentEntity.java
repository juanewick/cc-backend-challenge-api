package com.cc.brokerage.adapter.out.persistence.entity;

import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.adapter.out.persistence.converter.InstrumentTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "instruments")
public class InstrumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker", length = 10)
    private String ticker;

    @Column(name = "name")
    private String name;

    @Convert(converter = InstrumentTypeConverter.class)
    @Column(name = "type", length = 10)
    private InstrumentType type;

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public String getName() {
        return name;
    }

    public InstrumentType getType() {
        return type;
    }
}
