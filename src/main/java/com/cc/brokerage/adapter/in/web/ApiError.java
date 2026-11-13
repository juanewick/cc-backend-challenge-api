package com.cc.brokerage.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;

import java.util.List;

public record ApiError(
        String type,
        String title,
        int status,
        String detail,
        String code,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonProperty("invalid_params")
        List<InvalidParam> invalidParams
) {
    record InvalidParam(
            @JsonInclude(JsonInclude.Include.NON_NULL) String field,
            String message
    ) {
    }

    static ApiError of(HttpStatus httpStatus, String code, String detail, String paramKey, List<InvalidParam> params) {
        List<InvalidParam> published = params == null || params.isEmpty() ? paramsFor(paramKey, detail) : params;
        boolean validation = !published.isEmpty();
        Kind kind = kind(httpStatus, validation);
        return new ApiError(
                kind.type(),
                kind.title(),
                httpStatus.value(),
                detail,
                code,
                validation ? List.copyOf(published) : null
        );
    }

    private static List<InvalidParam> paramsFor(String code, String detail) {
        return switch (code) {
            case "INVALID_ACCOUNT" -> one("accountNumber", detail);
            case "INVALID_TICKER" -> one("ticker", detail);
            case "INVALID_ORDER_ID" -> one("orderId", detail);
            case "SEARCH_EMPTY", "SEARCH_TOO_LONG", "SEARCH_INVALID" -> one("q", detail);
            case "INVALID_SIDE" -> one("side", detail);
            case "INVALID_TYPE" -> one("type", detail);
            case "INVALID_SIZE" -> one("size", detail);
            case "INVALID_AMOUNT" -> one("amount", detail);
            case "INVALID_PRICE" -> one("price", detail);
            case "EXCLUSIVE_QUANTITY" -> List.of(new InvalidParam("size", detail), new InvalidParam("amount", detail));
            default -> List.of();
        };
    }

    private static List<InvalidParam> one(String field, String detail) {
        return List.of(new InvalidParam(field, detail));
    }

    private static Kind kind(HttpStatus httpStatus, boolean validation) {
        if (httpStatus == HttpStatus.NOT_FOUND) {
            return new Kind(ProblemTypes.NOT_FOUND, "Not found");
        }
        if (httpStatus == HttpStatus.CONFLICT) {
            return new Kind(ProblemTypes.CONFLICT, "Conflict");
        }
        if (validation) {
            return new Kind(ProblemTypes.INVALID_PARAMETERS, "Invalid parameters");
        }
        return new Kind(ProblemTypes.BAD_REQUEST, "Bad request");
    }

    private record Kind(String type, String title) {
    }
}
