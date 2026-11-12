package com.cc.brokerage.config;

import com.cc.brokerage.domain.service.PortfolioCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    PortfolioCalculator portfolioCalculator() {
        return new PortfolioCalculator();
    }
}
