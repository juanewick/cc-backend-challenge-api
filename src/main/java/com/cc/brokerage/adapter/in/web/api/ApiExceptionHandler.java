package com.cc.brokerage.adapter.in.web.api;

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
import java.util.Set;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String INTERNAL = "The request could not be processed.";
    private static final String UNREADABLE = "The request cannot be read.";
    private static final String UNKNOWN_FIELD = "The request contains a field that is not allowed.";
    private static final String DUPLICATE_PREFIX = "Duplicate Object property \"";
    private static final Set<String> FIELDS = Set.of("ticker", "side", "type", "size", "amount", "price");

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> domain(DomainException exception) {
        DomainErrorCodes error = exception.error();
        log.debug("Request rejected with {}: {}", error.code(), exception.getMessage());
        return problem(
                HttpStatus.valueOf(error.status()),
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
        log.error("Unexpected failure on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", INTERNAL, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> missingParameter() {
        return domain(new DomainException(DomainErrorCodes.SEARCH_EMPTY));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException exception) {
        String code = codeFor(exception);
        String message = messageFor(exception);
        String field = "UNKNOWN_FIELD".equals(code) ? null : fieldFor(exception);
        List<ApiError.InvalidParam> params = "UNKNOWN_FIELD".equals(code) || field != null
                ? List.of(new ApiError.InvalidParam(field, message))
                : List.of();
        return problem(HttpStatus.BAD_REQUEST, code, message, params);
    }

    @ExceptionHandler({HttpMediaTypeNotSupportedException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> unreadableRequest() {
        return problem(HttpStatus.BAD_REQUEST, "UNREADABLE", UNREADABLE, List.of());
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
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ApiError.of(status, code, detail, paramKey, params));
    }

    private static String codeFor(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof UnrecognizedPropertyException) {
                return "UNKNOWN_FIELD";
            }
        }
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (!(current instanceof JacksonException jackson)) {
                continue;
            }
            if (repeatedField(jackson) != null) {
                return "DUPLICATE_FIELD";
            }
            if (current instanceof MismatchedInputException || current instanceof InputCoercionException) {
                if (ourField(jackson) != null) {
                    return "INVALID_FIELD";
                }
            }
        }
        return "UNREADABLE";
    }

    private static String messageFor(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof UnrecognizedPropertyException) {
                return UNKNOWN_FIELD;
            }
        }
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (!(current instanceof JacksonException jackson)) {
                continue;
            }
            String repeated = repeatedField(jackson);
            if (repeated != null) {
                return "The " + repeated + " field is duplicated.";
            }
            if (current instanceof MismatchedInputException || current instanceof InputCoercionException) {
                String field = ourField(jackson);
                if (field != null) {
                    return "The " + field + " field is not valid.";
                }
            }
        }
        return UNREADABLE;
    }

    private static String repeatedField(JacksonException exception) {
        String original = exception.getOriginalMessage();
        if (original == null || !original.startsWith(DUPLICATE_PREFIX)) {
            return null;
        }
        int end = original.indexOf('"', DUPLICATE_PREFIX.length());
        if (end < 0) {
            return null;
        }
        String property = original.substring(DUPLICATE_PREFIX.length(), end);
        return FIELDS.contains(property) ? property : null;
    }

    private static String fieldFor(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof UnrecognizedPropertyException unrecognized && unrecognized.getPropertyName() != null) {
                return unrecognized.getPropertyName();
            }
        }
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (!(current instanceof JacksonException jackson)) {
                continue;
            }
            String repeated = repeatedField(jackson);
            if (repeated != null) {
                return repeated;
            }
            if (current instanceof MismatchedInputException || current instanceof InputCoercionException) {
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
