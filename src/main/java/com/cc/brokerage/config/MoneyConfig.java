package com.cc.brokerage.config;

import com.cc.brokerage.domain.model.MoneyLimits;
import com.cc.brokerage.domain.model.MoneyMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MoneyProperties.class)
public class MoneyConfig {

    private static final Logger log = LoggerFactory.getLogger(MoneyConfig.class);

    @Bean
    MoneyLimits moneyLimits(MoneyProperties properties) {
        MoneyLimits limits = properties.toLimits();
        log.info("Money limits: scale {}, amounts above {} up to {}, shares from {} to {}",
                limits.scale(), limits.minAmount(), limits.maxAmount(), limits.minShares(), limits.maxShares());
        return limits;
    }

    @Bean
    MoneyMath moneyMath(MoneyLimits limits) {
        return new MoneyMath(limits);
    }
}
