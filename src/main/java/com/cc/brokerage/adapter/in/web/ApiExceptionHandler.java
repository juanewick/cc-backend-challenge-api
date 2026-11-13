package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.domain.AccountNotFoundException;
import com.cc.brokerage.domain.InstrumentNotFoundException;
import com.cc.brokerage.domain.InvalidRequestException;
import com.cc.brokerage.domain.OrderConflictException;
import com.cc.brokerage.domain.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String UNREADABLE = "El pedido no se puede leer";
    private static final String UNKNOWN_FIELD = "El pedido tiene un campo que no corresponde";
    private static final String DUPLICATE_PREFIX = "Duplicate Object property \"";
    private static final Map<String, String> FIELDS = Map.of(
            "ticker", "ticker",
            "side", "lado",
            "type", "tipo",
            "size", "cantidad",
            "amount", "monto",
            "price", "precio"
    );

    @ExceptionHandler({AccountNotFoundException.class, InstrumentNotFoundException.class, OrderNotFoundException.class})
    ResponseEntity<ApiError> notFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler({InvalidRequestException.class, IllegalArgumentException.class})
    ResponseEntity<ApiError> badRequest(Exception exception) {
        return ResponseEntity.badRequest().body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> missingParameter() {
        return ResponseEntity.badRequest().body(new ApiError("La búsqueda no tiene texto"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ApiError(messageFor(exception)));
    }

    @ExceptionHandler({HttpMediaTypeNotSupportedException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> unreadableRequest() {
        return ResponseEntity.badRequest().body(new ApiError(UNREADABLE));
    }

    @ExceptionHandler(OrderConflictException.class)
    ResponseEntity<ApiError> conflict(OrderConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(exception.getMessage()));
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
                return "El campo " + FIELDS.get(repeated) + " está repetido";
            }
            if (current instanceof MismatchedInputException || current instanceof InputCoercionException) {
                String field = ourField(jackson);
                if (field != null) {
                    return "El campo " + FIELDS.get(field) + " no es válido";
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
        return FIELDS.containsKey(property) ? property : null;
    }

    private static String ourField(JacksonException exception) {
        if (exception.getPath() == null) {
            return null;
        }
        for (int index = exception.getPath().size() - 1; index >= 0; index--) {
            String name = exception.getPath().get(index).getPropertyName();
            if (name != null && FIELDS.containsKey(name)) {
                return name;
            }
        }
        return null;
    }
}
