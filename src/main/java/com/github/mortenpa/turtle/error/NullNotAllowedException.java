package com.github.mortenpa.turtle.error;

public class NullNotAllowedException extends RuntimeException {
    public NullNotAllowedException(String errorMessage) {
        super(errorMessage);
    }
}
