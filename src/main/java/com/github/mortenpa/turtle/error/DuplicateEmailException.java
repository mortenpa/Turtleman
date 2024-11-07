package com.github.mortenpa.turtle.error;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String errorMessage) {
        super(errorMessage);
    }
}
