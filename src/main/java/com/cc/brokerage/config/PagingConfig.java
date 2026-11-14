package com.cc.brokerage.config;

import com.cc.brokerage.domain.model.PageLimits;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PagingProperties.class)
public class PagingConfig {

    @Bean
    PageLimits pageLimits(PagingProperties properties) {
        return properties.toLimits();
    }
}
