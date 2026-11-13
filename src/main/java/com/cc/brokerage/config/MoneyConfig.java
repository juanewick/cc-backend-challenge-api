package com.cc.brokerage.config;

import com.cc.brokerage.domain.model.MoneyLimits;
import com.cc.brokerage.domain.model.MoneyMath;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MoneyProperties.class)
public class MoneyConfig {

    @Bean
    MoneyLimits moneyLimits(MoneyProperties properties) {
        return properties.toLimits();
    }

    @Bean
    MoneyMath moneyMath(MoneyLimits limits) {
        return new MoneyMath(limits);
    }
}
