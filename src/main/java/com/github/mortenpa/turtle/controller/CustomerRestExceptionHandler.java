package com.github.mortenpa.turtle.controller;

import com.github.mortenpa.turtle.error.DuplicateEmailException;
import com.github.mortenpa.turtle.error.NullNotAllowedException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.mortenpa.turtle.controller.util.ApiResponseHandler.buildApiResponse;

@ControllerAdvice(assignableTypes = CustomerController.class)
public class CustomerRestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomerRestExceptionHandler.class);



    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<CustomerApiResponse> handleDataDuplicateEmailException(DuplicateEmailException exception) {
        log.error("Duplicate email violation: {}", exception.getMessage(), exception);

        return buildApiResponse(
                false,
                "Failed due to email already being in use",
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(NullNotAllowedException.class)
    public ResponseEntity<CustomerApiResponse> handleNullNotAllowedException(NullNotAllowedException exception) {
        log.error("Null property value violation: {}", exception.getMessage(), exception);

        return buildApiResponse(
                false,
                "Failed due to null values",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CustomerApiResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        log.error("Constraint violation: {}", exception.getMessage(), exception);

        return buildApiResponse(
                false,
                "Failed due to property validations, check for missing or badly formatted properties",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomerApiResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        log.error("MethodArgumentNotValidException: {}", exception.getMessage(), exception);

        return buildApiResponse(
                false,
                "Failed due to invalid input, check for missing or badly formatted properties",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomerApiResponse> handleAnyException(Exception exception) {
        log.error("Uncaught error: {}", exception.getMessage(), exception);

        return buildApiResponse(
                false,
                "Failed due to an unknown error",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
