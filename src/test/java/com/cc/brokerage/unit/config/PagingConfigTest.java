package com.cc.brokerage.unit.config;

import com.cc.brokerage.config.PagingConfig;
import com.cc.brokerage.domain.model.PageSizeLimits;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PagingConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PagingConfig.class);

    @Test
    void yamlDefaultsAllowOneHundredPerPage() {
        runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(PageSizeLimits.class)).isEqualTo(PageSizeLimits.defaults());
                });
    }

    @Test
    void eachEndpointTakesItsOwnMaximum() {
        runner.withPropertyValues(
                        "brokerage.paging.instrument-search-max-size=25",
                        "brokerage.paging.order-list-max-size=60"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(PageSizeLimits.class)).isEqualTo(new PageSizeLimits(25, 60));
                });
    }

    @Test
    void aMaximumBelowOneStopsTheStartup() {
        runner.withPropertyValues(
                        "brokerage.paging.instrument-search-max-size=0",
                        "brokerage.paging.order-list-max-size=100"
                )
                .run(context -> assertThat(context).hasFailed());
    }
}
