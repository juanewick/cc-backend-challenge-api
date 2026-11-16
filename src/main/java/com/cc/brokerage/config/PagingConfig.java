package com.cc.brokerage.config;

import com.cc.brokerage.domain.model.PageLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PagingProperties.class)
public class PagingConfig {

    private static final Logger log = LoggerFactory.getLogger(PagingConfig.class);

    @Bean
    PageLimits pageLimits(PagingProperties properties) {
        PageLimits limits = properties.toLimits();
        log.info("Paging limits: instrument search up to page {} of {} items, order list up to page {} of {} items",
                limits.instrumentSearch().maxPage(), limits.instrumentSearch().maxSize(),
                limits.orderList().maxPage(), limits.orderList().maxSize());
        return limits;
    }
}
