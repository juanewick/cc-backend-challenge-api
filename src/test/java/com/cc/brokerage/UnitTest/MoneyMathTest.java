package com.cc.brokerage.UnitTest;

import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.MoneyMath;
import com.cc.brokerage.support.MoneyBounds;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyMathTest {

    @Test
    void amountAtTheMaximumStaysAfterHalfUp() {
        assertThat(MoneyMath.scaled(MoneyBounds.justWithinMax())).isEqualByComparingTo(MoneyMath.maxAmount());
    }

    @Test
    void roundingPastTheMaximumIsRejected() {
        assertThatThrownBy(() -> MoneyMath.scaled(MoneyBounds.justPastMax()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shareCountAboveTheMaximumIsNotTruncated() {
        assertThatThrownBy(() -> MoneyMath.sharesFromAmount(MoneyBounds.oneSharePastMax(), BigDecimal.ONE))
                .isInstanceOf(DomainException.class)
                .hasMessage(MoneyMath.sharesRangeMessage());
    }

    @Test
    void zeroSharesStayZero() {
        assertThat(MoneyMath.sharesFromAmount(new BigDecimal("1"), new BigDecimal("10"))).isZero();
    }

    @Test
    void maximumSharesAreAccepted() {
        assertThat(MoneyMath.sharesFromAmount(BigDecimal.valueOf(MoneyMath.maxShares()), BigDecimal.ONE))
                .isEqualTo(Math.toIntExact(MoneyMath.maxShares()));
    }
}
