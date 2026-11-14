package com.cc.brokerage.domain.model;

/**
 * The page limits of each paged endpoint.
 */
public record PageLimits(PageLimit instrumentSearch, PageLimit orderList) {

    public static PageLimits defaults() {
        return new PageLimits(new PageLimit(50, 100), new PageLimit(1000, 100));
    }

    public PageLimits {
        if (instrumentSearch == null || orderList == null) {
            throw new IllegalArgumentException("Every paged endpoint needs its page limits");
        }
    }
}
