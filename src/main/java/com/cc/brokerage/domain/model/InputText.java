package com.cc.brokerage.domain.model;

import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;

import java.math.BigInteger;

public final class InputText {

    private static final int MAX_ACCOUNT_LENGTH = 20;
    private static final int MAX_TICKER_LENGTH = 10;
    private static final int MAX_SEARCH_LENGTH = 100;
    private static final BigInteger MAX_ORDER_ID = BigInteger.valueOf(Integer.MAX_VALUE);

    private InputText() {
    }

    public static String account(String value) {
        if (value == null || !value.matches("[0-9]{1," + MAX_ACCOUNT_LENGTH + "}")) {
            throw new DomainException(DomainErrorCodes.INVALID_ACCOUNT);
        }
        return value;
    }

    public static String ticker(String value) {
        if (value == null || !value.matches("[A-Za-z0-9]{1," + MAX_TICKER_LENGTH + "}")) {
            throw new DomainException(DomainErrorCodes.INVALID_TICKER);
        }
        return value;
    }

    public static String search(String value) {
        if (value == null) {
            throw new DomainException(DomainErrorCodes.SEARCH_EMPTY);
        }
        String text = trimEnds(value);
        if (text.isEmpty()) {
            throw new DomainException(DomainErrorCodes.SEARCH_EMPTY);
        }
        if (text.codePointCount(0, text.length()) > MAX_SEARCH_LENGTH) {
            throw new DomainException(DomainErrorCodes.SEARCH_TOO_LONG);
        }
        if (text.codePoints().anyMatch(Character::isISOControl)) {
            throw new DomainException(DomainErrorCodes.SEARCH_INVALID);
        }
        return text;
    }

    public static long orderId(String value) {
        if (value == null || !value.matches("[0-9]+")) {
            throw new DomainException(DomainErrorCodes.INVALID_ORDER_ID);
        }
        BigInteger id = new BigInteger(value);
        if (id.compareTo(BigInteger.ONE) < 0 || id.compareTo(MAX_ORDER_ID) > 0) {
            throw new DomainException(DomainErrorCodes.INVALID_ORDER_ID);
        }
        return id.longValue();
    }

    private static String trimEnds(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isTrimmedEnd(value.charAt(start))) {
            start++;
        }
        while (end > start && isTrimmedEnd(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean isTrimmedEnd(char character) {
        return Character.isWhitespace(character) && !Character.isISOControl(character);
    }
}
