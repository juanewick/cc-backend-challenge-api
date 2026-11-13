package com.cc.brokerage.UnitTest;

import com.cc.brokerage.config.MoneyConfig;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.MoneyLimits;
import com.cc.brokerage.domain.model.MoneyMath;
import com.cc.brokerage.support.MoneyBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MoneyConfig.class);

    @AfterEach
    void restoreDefaults() {
        MoneyMath.configure(MoneyLimits.defaults());
    }

    @Test
    void yamlDefaultsKeepTheCurrentBounds() {
        runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(MoneyMath.scale()).isEqualTo(2);
                    assertThat(MoneyMath.minAmount()).isEqualByComparingTo("0");
                    assertThat(MoneyMath.maxAmount()).isEqualByComparingTo("99999999.99");
                    assertThat(MoneyMath.minShares()).isEqualTo(1L);
                    assertThat(MoneyMath.maxShares()).isEqualTo(2147483647L);
                    assertThat(MoneyMath.scaled(MoneyBounds.justWithinMax())).isEqualByComparingTo(MoneyMath.maxAmount());
                });
    }

    @Test
    void environmentPropertiesReplaceTheBounds() {
        runner.withPropertyValues(
                        "brokerage.money.scale=2",
                        "brokerage.money.min-amount=0",
                        "brokerage.money.max-amount=10.00",
                        "brokerage.money.min-shares=2",
                        "brokerage.money.max-shares=5"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(MoneyMath.maxAmount()).isEqualByComparingTo("10.00");
                    assertThat(MoneyMath.minShares()).isEqualTo(2L);
                    assertThatThrownBy(() -> MoneyMath.scaled(new BigDecimal("10.01")))
                            .isInstanceOf(DomainException.class);
                    assertThatThrownBy(() -> MoneyMath.sharesFromAmount(new BigDecimal("6"), BigDecimal.ONE))
                            .isInstanceOf(DomainException.class)
                            .hasMessage("Size must be an integer from 2 to 5.");
                });
    }
}
