package com.cc.brokerage.domain;

public enum DomainErrorCodes {
    INSTRUMENT_NOT_FOUND("BRK-002", "INSTRUMENT_NOT_FOUND", ErrorCategory.NOT_FOUND, "Instrument not found."),
    ORDER_NOT_FOUND("BRK-003", "ORDER_NOT_FOUND", ErrorCategory.NOT_FOUND, "Order not found."),
    ACCOUNT_NOT_FOUND("BRK-004", "ACCOUNT_NOT_FOUND", ErrorCategory.NOT_FOUND, "Account not found."),
    ORDER_NOT_OPEN("BRK-005", "ORDER_NOT_OPEN", ErrorCategory.CONFLICT, "Only open orders can be cancelled."),
    INSTRUMENT_NOT_TRADABLE("BRK-006", "INSTRUMENT_NOT_TRADABLE", ErrorCategory.INVALID_INPUT, "The currency is not tradable."),
    CASH_MOVEMENT("BRK-007", "CASH_MOVEMENT", ErrorCategory.INVALID_INPUT, "New cash deposits and withdrawals are not accepted."),
    INVALID_ACCOUNT("BRK-008", "INVALID_ACCOUNT", ErrorCategory.INVALID_INPUT, "The account is not valid."),
    INVALID_TICKER("BRK-009", "INVALID_TICKER", ErrorCategory.INVALID_INPUT, "The ticker is not valid."),
    INVALID_ORDER_ID("BRK-010", "INVALID_ORDER_ID", ErrorCategory.INVALID_INPUT, "The order is not valid."),
    SEARCH_EMPTY("BRK-011", "SEARCH_EMPTY", ErrorCategory.INVALID_INPUT, "The search has no text."),
    SEARCH_TOO_LONG("BRK-012", "SEARCH_TOO_LONG", ErrorCategory.INVALID_INPUT, "The search exceeds the maximum length."),
    SEARCH_INVALID("BRK-013", "SEARCH_INVALID", ErrorCategory.INVALID_INPUT, "The search is not valid."),
    INVALID_SIDE("BRK-014", "INVALID_SIDE", ErrorCategory.INVALID_INPUT, "Side must be BUY or SELL."),
    INVALID_TYPE("BRK-015", "INVALID_TYPE", ErrorCategory.INVALID_INPUT, "Order type is required."),
    EXCLUSIVE_QUANTITY("BRK-016", "EXCLUSIVE_QUANTITY", ErrorCategory.INVALID_INPUT, "Exactly one quantity is required: shares or amount."),
    INVALID_SIZE("BRK-017", "INVALID_SIZE", ErrorCategory.INVALID_INPUT, "Size must be an integer from %d to %d."),
    AMOUNT_NOT_POSITIVE("BRK-018", "INVALID_AMOUNT", ErrorCategory.INVALID_INPUT, "Amount must be greater than zero."),
    AMOUNT_ABOVE_MAXIMUM("BRK-019", "INVALID_AMOUNT", ErrorCategory.INVALID_INPUT, "Amount exceeds the maximum allowed."),
    AMOUNT_NOT_A_NUMBER("BRK-020", "INVALID_AMOUNT", ErrorCategory.INVALID_INPUT, "Amount is not a valid number."),
    PRICE_NOT_POSITIVE("BRK-021", "INVALID_PRICE", ErrorCategory.INVALID_INPUT, "Price is required and must be greater than zero."),
    PRICE_ABOVE_MAXIMUM("BRK-022", "INVALID_PRICE", ErrorCategory.INVALID_INPUT, "Price exceeds the maximum allowed."),
    PRICE_NOT_A_NUMBER("BRK-023", "INVALID_PRICE", ErrorCategory.INVALID_INPUT, "Price is not a valid number."),
    INVALID_STATUS("BRK-024", "INVALID_STATUS", ErrorCategory.INVALID_INPUT, "Status must be NEW, FILLED, REJECTED or CANCELLED."),
    INVALID_PAGE("BRK-025", "INVALID_PAGE", ErrorCategory.INVALID_INPUT, "Page must be an integer from 0 to %d."),
    INVALID_PAGE_SIZE("BRK-026", "INVALID_PAGE_SIZE", ErrorCategory.INVALID_INPUT, "Page size must be an integer from 1 to %d.");

    private final String code;
    private final String description;
    private final ErrorCategory category;
    private final String message;

    DomainErrorCodes(String code, String description, ErrorCategory category, String message) {
        this.code = code;
        this.description = description;
        this.category = category;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }

    public ErrorCategory category() {
        return category;
    }

    public String message() {
        return message;
    }
}
