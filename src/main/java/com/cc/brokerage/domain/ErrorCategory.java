package com.cc.brokerage.domain;

/**
 * What kind of failure a domain error is. Adapters decide how to present it (an HTTP status, an exit code...).
 */
public enum ErrorCategory {
    INVALID_INPUT,
    NOT_FOUND,
    CONFLICT
}
