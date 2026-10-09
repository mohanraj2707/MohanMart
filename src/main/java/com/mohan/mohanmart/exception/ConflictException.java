package com.mohan.mohanmart.exception;

/**
 * Exception thrown when an operation conflicts with the current state of a resource,
 * such as an invalid order status transition or duplicate resource (HTTP 409).
 */
public class ConflictException extends OrderException {
    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message, "CONFLICT");
    }

    public ConflictException(String message, String errorCode) {
        super(message, errorCode);
    }
}
