package com.mohan.mohanmart.exception;

/**
 * Exception thrown when a client or session exceeds the allowed request rate limit (HTTP 429).
 */
public class RateLimitException extends AppException {
    private static final long serialVersionUID = 1L;

    public RateLimitException(String message) {
        super(message, "RATE_LIMIT_EXCEEDED");
    }

    public RateLimitException(String message, String errorCode) {
        super(message, errorCode);
    }
}
