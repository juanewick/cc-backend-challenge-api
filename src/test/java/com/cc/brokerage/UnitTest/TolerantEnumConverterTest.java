package com.cc.brokerage.UnitTest;

import com.cc.brokerage.adapter.out.persistence.converter.InstrumentTypeConverter;
import com.cc.brokerage.adapter.out.persistence.converter.OrderSideConverter;
import com.cc.brokerage.adapter.out.persistence.converter.OrderStatusConverter;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TolerantEnumConverterTest {

    @Test
    void knownValuesRoundTrip() {
        OrderStatusConverter converter = new OrderStatusConverter();

        assertThat(converter.convertToEntityAttribute("FILLED")).isEqualTo(OrderStatus.FILLED);
        assertThat(converter.convertToDatabaseColumn(OrderStatus.FILLED)).isEqualTo("FILLED");
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void valueThisServiceDoesNotKnowIsReadAsUnknown() {
        assertThat(new InstrumentTypeConverter().convertToEntityAttribute("BONOS")).isEqualTo(InstrumentType.UNKNOWN);
        assertThat(new OrderSideConverter().convertToEntityAttribute("DIVIDEND")).isEqualTo(OrderSide.UNKNOWN);
    }
}
