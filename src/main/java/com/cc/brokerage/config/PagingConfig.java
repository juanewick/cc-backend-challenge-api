package com.cc.brokerage.config;

import com.cc.brokerage.domain.model.PageSizeLimits;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PagingProperties.class)
public class PagingConfig {

    @Bean
    PageSizeLimits pageSizeLimits(PagingProperties properties) {
        return properties.toLimits();
    }
}
