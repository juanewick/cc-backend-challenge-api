package com.cc.brokerage.config;

import com.cc.brokerage.domain.model.PageSizeLimits;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brokerage.paging")
public record PagingProperties(
        int instrumentSearchMaxSize,
        int orderListMaxSize
) {

    public PageSizeLimits toLimits() {
        return new PageSizeLimits(instrumentSearchMaxSize, orderListMaxSize);
    }
}
