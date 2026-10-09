package com.mohan.mohanmart.ai;

/**
 * Strategy interface for MohanMart AI Chatbot providers.
 * Implementations generate marketplace-scoped replies given a sanitized user message
 * and optional non-PII catalog context (product names, categories, and prices).
 */
public interface ChatProvider {

    /**
     * Generates a reply to a customer's marketplace or product question.
     *
     * @param userMessage Sanitized user message (non-empty, max 500 characters, no PII)
     * @param context     Optional generic product catalog context (names, categories, prices)
     * @return Assistant reply text
     */
    String getReply(String userMessage, String context);
}
