package com.cc.brokerage.unit.config;

import com.cc.brokerage.config.MoneyConfig;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.MoneyMath;
import com.cc.brokerage.support.MoneyBounds;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MoneyConfig.class);

    @Test
    void yamlDefaultsKeepTheCurrentBounds() {
        runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    MoneyMath money = context.getBean(MoneyMath.class);
                    assertThat(money.scale()).isEqualTo(2);
                    assertThat(money.minAmount()).isEqualByComparingTo("0");
                    assertThat(money.maxAmount()).isEqualByComparingTo("99999999.99");
                    assertThat(money.minShares()).isEqualTo(1L);
                    assertThat(money.maxShares()).isEqualTo(2147483647L);
                    assertThat(money.scaled(new MoneyBounds(money).justWithinMax())).isEqualByComparingTo(money.maxAmount());
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
                    MoneyMath money = context.getBean(MoneyMath.class);
                    assertThat(money.maxAmount()).isEqualByComparingTo("10.00");
                    assertThat(money.minShares()).isEqualTo(2L);
                    assertThatThrownBy(() -> money.scaled(new BigDecimal("10.01")))
                            .isInstanceOf(DomainException.class);
                    assertThatThrownBy(() -> money.sharesFromAmount(new BigDecimal("6"), BigDecimal.ONE))
                            .isInstanceOf(DomainException.class)
                            .hasMessage("Size must be an integer from 2 to 5.");
                });
    }

    @Test
    void twoConfigurationsDoNotShareState() {
        runner.withPropertyValues("brokerage.money.max-amount=10.00", "brokerage.money.min-amount=0",
                        "brokerage.money.scale=2", "brokerage.money.min-shares=1", "brokerage.money.max-shares=5")
                .run(context -> assertThat(MoneyBounds.DEFAULT_MONEY.maxAmount()).isEqualByComparingTo("99999999.99"));
    }
}
