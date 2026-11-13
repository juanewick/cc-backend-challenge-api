package com.cc.brokerage.domain;

public class DomainException extends RuntimeException {

    private final DomainErrorCodes error;

    public DomainException(DomainErrorCodes error, Object... args) {
        super(render(error, args));
        this.error = error;
    }

    public DomainErrorCodes error() {
        return error;
    }

    private static String render(DomainErrorCodes error, Object... args) {
        if (args == null || args.length == 0) {
            return error.message();
        }
        return error.message().formatted(args);
    }
}
