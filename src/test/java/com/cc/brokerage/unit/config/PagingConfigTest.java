package com.cc.brokerage.unit.config;

import com.cc.brokerage.config.PagingConfig;
import com.cc.brokerage.domain.model.PageLimit;
import com.cc.brokerage.domain.model.PageLimits;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PagingConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PagingConfig.class);

    @Test
    void yamlDefaultsMatchTheDomainDefaults() {
        runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(PageLimits.class)).isEqualTo(PageLimits.defaults());
                });
    }

    @Test
    void eachEndpointTakesItsOwnMaximum() {
        runner.withPropertyValues(
                        "brokerage.paging.instrument-search.max-page=5",
                        "brokerage.paging.instrument-search.max-size=25",
                        "brokerage.paging.order-list.max-page=200",
                        "brokerage.paging.order-list.max-size=60"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(PageLimits.class))
                            .isEqualTo(new PageLimits(new PageLimit(5, 25), new PageLimit(200, 60)));
                });
    }

    @Test
    void aPageSizeBelowOneStopsTheStartup() {
        runner.withPropertyValues(
                        "brokerage.paging.instrument-search.max-page=50",
                        "brokerage.paging.instrument-search.max-size=0",
                        "brokerage.paging.order-list.max-page=1000",
                        "brokerage.paging.order-list.max-size=100"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void aNegativeDeepestPageStopsTheStartup() {
        runner.withPropertyValues(
                        "brokerage.paging.instrument-search.max-page=50",
                        "brokerage.paging.instrument-search.max-size=100",
                        "brokerage.paging.order-list.max-page=-1",
                        "brokerage.paging.order-list.max-size=100"
                )
                .run(context -> assertThat(context).hasFailed());
    }
}
