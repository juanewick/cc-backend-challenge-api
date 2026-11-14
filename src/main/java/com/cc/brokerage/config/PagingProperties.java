package com.cc.brokerage.config;

import com.cc.brokerage.domain.model.PageLimit;
import com.cc.brokerage.domain.model.PageLimits;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brokerage.paging")
public record PagingProperties(
        Endpoint instrumentSearch,
        Endpoint orderList
) {

    public PageLimits toLimits() {
        return new PageLimits(instrumentSearch.toLimit(), orderList.toLimit());
    }

    public record Endpoint(int maxPage, int maxSize) {

        PageLimit toLimit() {
            return new PageLimit(maxPage, maxSize);
        }
    }
}
