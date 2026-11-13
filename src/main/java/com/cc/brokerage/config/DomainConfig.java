package com.cc.brokerage.config;

import com.cc.brokerage.domain.model.MoneyMath;
import com.cc.brokerage.domain.service.PortfolioCalculator;
import com.cc.brokerage.domain.service.QuoteCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    PortfolioCalculator portfolioCalculator(MoneyMath money) {
        return new PortfolioCalculator(money);
    }

    @Bean
    QuoteCalculator quoteCalculator(MoneyMath money) {
        return new QuoteCalculator(money);
    }
}
