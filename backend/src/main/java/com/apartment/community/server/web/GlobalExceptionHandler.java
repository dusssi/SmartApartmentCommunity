package com.apartment.community.server.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> api(ApiException error, HttpServletRequest request) {
        return response(error.status, error.code, error.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException error, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError field : error.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(field.getField(), field.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed.", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> constraints(ConstraintViolationException error, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        error.getConstraintViolations().forEach(v -> fields.put(v.getPropertyPath().toString(), v.getMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed.", request, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> malformed(Exception error, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request is malformed.", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> missing(NoResourceFoundException error, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found.", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> method(HttpRequestMethodNotSupportedException error, HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP method not supported.", request, null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> optimistic(ObjectOptimisticLockingFailureException error, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "STALE_VERSION", "The record changed. Reload it before retrying.", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException error, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data.", request, null);
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    ResponseEntity<ErrorResponse> database(Exception error, HttpServletRequest request) {
        log.error("Database unavailable requestId={}", MDC.get("requestId"));
        return response(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_UNAVAILABLE",
                "Persistence is temporarily unavailable. No changes were confirmed.", request, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception error, HttpServletRequest request) {
        log.error("Unhandled API error requestId=" + MDC.get("requestId"), error);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The request could not be completed. No success is implied.", request, null);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message,
                                                    HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(ErrorResponse.of(request.getRequestURI(), code, message,
                MDC.get("requestId"), fields));
    }
}
