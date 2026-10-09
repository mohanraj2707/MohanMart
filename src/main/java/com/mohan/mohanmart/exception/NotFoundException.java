package com.mohan.mohanmart.exception;

/**
 * Exception thrown when a requested entity or resource does not exist (HTTP 404).
 */
public class NotFoundException extends ResourceNotFoundException {
    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String resourceName, Object identifier) {
        super(resourceName + " not found: " + identifier, "NOT_FOUND");
    }
}
