package com.cc.brokerage.domain.model;

/**
 * The largest page each paged endpoint serves.
 */
public record PageSizeLimits(int instrumentSearch, int orderList) {

    public static PageSizeLimits defaults() {
        return new PageSizeLimits(100, 100);
    }

    public PageSizeLimits {
        if (instrumentSearch < 1 || orderList < 1) {
            throw new IllegalArgumentException("The largest page size must be at least 1");
        }
    }
}
