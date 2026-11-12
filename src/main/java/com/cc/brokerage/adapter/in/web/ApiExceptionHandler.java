package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.domain.AccountNotFoundException;
import com.cc.brokerage.domain.InstrumentNotFoundException;
import com.cc.brokerage.domain.InvalidRequestException;
import com.cc.brokerage.domain.OrderConflictException;
import com.cc.brokerage.domain.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({AccountNotFoundException.class, InstrumentNotFoundException.class, OrderNotFoundException.class})
    ResponseEntity<ApiError> notFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler({
            InvalidRequestException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class
    })
    ResponseEntity<ApiError> badRequest(Exception exception) {
        String message = exception instanceof MissingServletRequestParameterException
                ? "La búsqueda no tiene texto"
                : exception.getMessage();
        return ResponseEntity.badRequest().body(new ApiError(message));
    }

    @ExceptionHandler(OrderConflictException.class)
    ResponseEntity<ApiError> conflict(OrderConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(exception.getMessage()));
    }
}
