package com.cc.brokerage.domain.model;

/**
 * The deepest page and the largest page size one paged endpoint serves. A deep page still makes the database
 * walk every earlier row, so the page number is bounded too.
 */
public record PageLimit(int maxPage, int maxSize) {

    public PageLimit {
        if (maxPage < 0) {
            throw new IllegalArgumentException("The deepest page cannot be negative");
        }
        if (maxSize < 1) {
            throw new IllegalArgumentException("The largest page size must be at least 1");
        }
    }
}
