package com.cc.brokerage.adapter.in.web.api;

/**
 * A JSON request body repeats a property inside the same object.
 */
public class DuplicateFieldException extends RuntimeException {

    private final String field;

    public DuplicateFieldException(String field) {
        super("Duplicate property in request body");
        this.field = field;
    }

    public String field() {
        return field;
    }
}
