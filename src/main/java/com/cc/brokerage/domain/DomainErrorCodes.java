package com.cc.brokerage.domain;

public enum DomainErrorCodes {
    ORDER_NOT_OWNED("BRK-001", "ORDER_NOT_OWNED", 409, "The order does not belong to this account."),
    INSTRUMENT_NOT_FOUND("BRK-002", "INSTRUMENT_NOT_FOUND", 404, "Instrument not found."),
    ORDER_NOT_FOUND("BRK-003", "ORDER_NOT_FOUND", 404, "Order not found."),
    ACCOUNT_NOT_FOUND("BRK-004", "ACCOUNT_NOT_FOUND", 404, "Account not found."),
    ORDER_NOT_OPEN("BRK-005", "ORDER_NOT_OPEN", 409, "Only open orders can be cancelled."),
    INSTRUMENT_NOT_TRADABLE("BRK-006", "INSTRUMENT_NOT_TRADABLE", 400, "The currency is not tradable."),
    CASH_MOVEMENT("BRK-007", "CASH_MOVEMENT", 400, "New cash deposits and withdrawals are not accepted."),
    INVALID_ACCOUNT("BRK-008", "INVALID_ACCOUNT", 400, "The account is not valid."),
    INVALID_TICKER("BRK-009", "INVALID_TICKER", 400, "The ticker is not valid."),
    INVALID_ORDER_ID("BRK-010", "INVALID_ORDER_ID", 400, "The order is not valid."),
    SEARCH_EMPTY("BRK-011", "SEARCH_EMPTY", 400, "The search has no text."),
    SEARCH_TOO_LONG("BRK-012", "SEARCH_TOO_LONG", 400, "The search exceeds the maximum length."),
    SEARCH_INVALID("BRK-013", "SEARCH_INVALID", 400, "The search is not valid."),
    INVALID_SIDE("BRK-014", "INVALID_SIDE", 400, "Side must be BUY or SELL."),
    INVALID_TYPE("BRK-015", "INVALID_TYPE", 400, "Order type is required."),
    EXCLUSIVE_QUANTITY("BRK-016", "EXCLUSIVE_QUANTITY", 400, "Exactly one quantity is required: shares or amount."),
    INVALID_SIZE("BRK-017", "INVALID_SIZE", 400, "Size must be an integer from %d to %d."),
    AMOUNT_NOT_POSITIVE("BRK-018", "INVALID_AMOUNT", 400, "Amount must be greater than zero."),
    AMOUNT_ABOVE_MAXIMUM("BRK-019", "INVALID_AMOUNT", 400, "Amount exceeds the maximum allowed."),
    AMOUNT_NOT_A_NUMBER("BRK-020", "INVALID_AMOUNT", 400, "Amount is not a valid number."),
    PRICE_NOT_POSITIVE("BRK-021", "INVALID_PRICE", 400, "Price is required and must be greater than zero."),
    PRICE_ABOVE_MAXIMUM("BRK-022", "INVALID_PRICE", 400, "Price exceeds the maximum allowed."),
    PRICE_NOT_A_NUMBER("BRK-023", "INVALID_PRICE", 400, "Price is not a valid number.");

    private final String code;
    private final String description;
    private final int status;
    private final String message;

    DomainErrorCodes(String code, String description, int status, String message) {
        this.code = code;
        this.description = description;
        this.status = status;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }

    public int status() {
        return status;
    }

    public String message() {
        return message;
    }
}
