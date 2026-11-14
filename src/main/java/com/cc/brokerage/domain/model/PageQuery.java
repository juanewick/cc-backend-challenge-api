package com.cc.brokerage.domain.model;

public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_PAGE = 100_000;

    public PageQuery {
        if (page < 0 || page > MAX_PAGE) {
            throw new IllegalArgumentException("Page out of range: " + page);
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size out of range: " + size);
        }
    }

    public static PageQuery first() {
        return new PageQuery(0, DEFAULT_SIZE);
    }
}
