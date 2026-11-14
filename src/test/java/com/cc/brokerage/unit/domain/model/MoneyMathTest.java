package com.cc.brokerage.unit.domain.model;

import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.MoneyMath;
import com.cc.brokerage.support.MoneyBounds;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyMathTest {

    private final MoneyMath money = MoneyBounds.DEFAULT_MONEY;
    private final MoneyBounds bounds = MoneyBounds.DEFAULTS;

    @Test
    void amountAtTheMaximumStaysAfterHalfUp() {
        assertThat(money.scaled(bounds.justWithinMax())).isEqualByComparingTo(money.maxAmount());
    }

    @Test
    void roundingPastTheMaximumIsRejected() {
        assertThatThrownBy(() -> money.scaled(bounds.justPastMax()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shareCountAboveTheMaximumIsNotTruncated() {
        assertThatThrownBy(() -> money.sharesFromAmount(bounds.oneSharePastMax(), BigDecimal.ONE))
                .isInstanceOf(DomainException.class)
                .hasMessage(money.sharesRangeMessage());
    }

    @Test
    void zeroSharesStayZero() {
        assertThat(money.sharesFromAmount(new BigDecimal("1"), new BigDecimal("10"))).isZero();
    }

    @Test
    void maximumSharesAreAccepted() {
        assertThat(money.sharesFromAmount(BigDecimal.valueOf(money.maxShares()), BigDecimal.ONE))
                .isEqualTo(Math.toIntExact(money.maxShares()));
    }
}
