package com.mohan.mohanmart.service;

import com.mohan.mohanmart.exception.AppException;

/**
 * Service interface for AI chatbot support and customer assistance.
 * Enforces input validation, per-session rate limiting (10 messages/minute),
 * per-session repeated-question caching, catalog context lookup, and graceful failover.
 */
public interface ChatService {

    /**
     * Processes a customer inquiry scoped to a user ID.
     *
     * @param userMessage Message query from buyer/visitor
     * @param userId      Optional authenticated user ID
     * @return AI assistant reply
     * @throws AppException if validation or rate limit fails
     */
    String processMessage(String userMessage, Long userId) throws AppException;

    /**
     * Processes a customer inquiry scoped to a session identifier.
     *
     * @param userMessage Message query from buyer/visitor (1-500 characters)
     * @param sessionKey  Session ID or rate-limit key
     * @return AI assistant reply
     * @throws AppException if validation or rate limit fails
     */
    default String processMessage(String userMessage, String sessionKey) throws AppException {
        return processMessage(userMessage, (Long) null);
    }
}
