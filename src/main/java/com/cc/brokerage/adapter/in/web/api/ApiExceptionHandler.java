package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.adapter.in.web.RequestLoggingFilter;
import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String INTERNAL = "The request could not be processed.";
    private static final String UNREADABLE = "The request cannot be read.";
    private static final String UNSUPPORTED_MEDIA_TYPE = "The request body must be sent as application/json.";
    private static final String UNKNOWN_FIELD = "The request contains a field that is not allowed.";
    private static final Set<String> FIELDS = Set.of("ticker", "side", "type", "size", "amount", "price");
    private static final Map<String, DomainErrorCodes> NOT_A_NUMBER = Map.of(
            "amount", DomainErrorCodes.AMOUNT_NOT_A_NUMBER,
            "price", DomainErrorCodes.PRICE_NOT_A_NUMBER
    );

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> domain(DomainException exception) {
        DomainErrorCodes error = exception.error();
        return problem(
                statusOf(error),
                error.code(),
                exception.getMessage(),
                error.description(),
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) throws Exception {
        if (exception instanceof ErrorResponse) {
            // Spring's own 4xx (405, 406, unknown path...): rethrowing hands it back to the default resolvers.
            throw exception;
        }
        // The route pattern, not the URI: the URI carries the account number.
        log.error("Unexpected failure on {} {}", request.getMethod(), RequestLoggingFilter.routeOf(request), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", INTERNAL, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> missingParameter() {
        return domain(new DomainException(DomainErrorCodes.SEARCH_EMPTY));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException exception) {
        if (unrecognized(exception)) {
            return problem(HttpStatus.BAD_REQUEST, "UNKNOWN_FIELD", UNKNOWN_FIELD,
                    List.of(new ApiError.InvalidParam(null, UNKNOWN_FIELD)));
        }
        String field = mismatchedField(exception);
        if (field == null) {
            return problem(HttpStatus.BAD_REQUEST, "UNREADABLE", UNREADABLE, List.of());
        }
        if (NOT_A_NUMBER.containsKey(field)) {
            return domain(new DomainException(NOT_A_NUMBER.get(field)));
        }
        String message = "The " + field + " field is not valid.";
        return problem(HttpStatus.BAD_REQUEST, "INVALID_FIELD", message, List.of(new ApiError.InvalidParam(field, message)));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> unsupportedMediaType() {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", UNSUPPORTED_MEDIA_TYPE, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> unreadableRequest() {
        return problem(HttpStatus.BAD_REQUEST, "UNREADABLE", UNREADABLE, List.of());
    }

    private static HttpStatus statusOf(DomainErrorCodes error) {
        return switch (error.category()) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
    }

    private static ResponseEntity<ApiError> problem(
            HttpStatus status,
            String code,
            String detail,
            List<ApiError.InvalidParam> params
    ) {
        return problem(status, code, detail, code, params);
    }

    private static ResponseEntity<ApiError> problem(
            HttpStatus status,
            String code,
            String detail,
            String paramKey,
            List<ApiError.InvalidParam> params
    ) {
        if (status.is4xxClientError()) {
            // Every rejection passes through here, so none goes unlogged. The detail never carries the submitted value.
            log.debug("Request rejected with {} {}: {}", status.value(), code, detail);
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ApiError.of(status, code, detail, paramKey, params));
    }

    private static boolean unrecognized(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof UnrecognizedPropertyException) {
                return true;
            }
        }
        return false;
    }

    /**
     * The request property whose value had the wrong type, when it is one of ours; null otherwise.
     */
    private static String mismatchedField(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof JacksonException jackson
                    && (current instanceof MismatchedInputException || current instanceof InputCoercionException)) {
                String field = ourField(jackson);
                if (field != null) {
                    return field;
                }
            }
        }
        return null;
    }

    private static String ourField(JacksonException exception) {
        if (exception.getPath() == null) {
            return null;
        }
        for (int index = exception.getPath().size() - 1; index >= 0; index--) {
            String name = exception.getPath().get(index).getPropertyName();
            if (name != null && FIELDS.contains(name)) {
                return name;
            }
        }
        return null;
    }
}
