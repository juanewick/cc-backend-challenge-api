package com.cc.brokerage.domain.model;

import com.cc.brokerage.domain.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyMathTest {

    @Test
    void eightIntegerDigitsStayAfterHalfUp() {
        assertThat(MoneyMath.scaled(new BigDecimal("99999999.994"))).isEqualByComparingTo("99999999.99");
    }

    @Test
    void roundingIntoANinthIntegerDigitIsRejected() {
        assertThatThrownBy(() -> MoneyMath.scaled(new BigDecimal("99999999.995")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shareCountAboveIntegerMaxIsNotTruncated() {
        assertThatThrownBy(() -> MoneyMath.sharesFromAmount(new BigDecimal("2147483648"), BigDecimal.ONE))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("La cantidad debe ser un entero de 1 a 2147483647");
    }

    @Test
    void zeroSharesStayZero() {
        assertThat(MoneyMath.sharesFromAmount(new BigDecimal("1"), new BigDecimal("10"))).isZero();
    }

    @Test
    void maxIntegerSharesAreAccepted() {
        assertThat(MoneyMath.sharesFromAmount(new BigDecimal("2147483647"), BigDecimal.ONE)).isEqualTo(2147483647);
    }
}
