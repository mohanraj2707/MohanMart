package com.mohan.mohanmart.service.impl;

import com.mohan.mohanmart.dao.impl.ProductDAOImpl;
import com.mohan.mohanmart.exception.AppException;
import com.mohan.mohanmart.exception.ValidationException;
import com.mohan.mohanmart.service.ChatService;
import com.mohan.mohanmart.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side implementation of ChatService.
 * Keeps AI provider abstraction clean, secure, and fully server-side without exposing API keys.
 */
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);
    private final ProductService productService;

    public ChatServiceImpl() {
        this(new ProductServiceImpl(new ProductDAOImpl()));
    }

    public ChatServiceImpl(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public String processMessage(String userMessage, Long userId) throws AppException {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new ValidationException("message", "Chat message cannot be blank");
        }

        String query = userMessage.trim().toLowerCase();
        logger.info("Processing chat query from user {}: {}", userId != null ? userId : "guest", query);

        if (query.contains("return") || query.contains("refund")) {
            return "MohanMart offers a hassle-free 7-day return policy on all eligible items. Visit your Orders page to initiate a return.";
        } else if (query.contains("shipping") || query.contains("delivery")) {
            return "Standard delivery takes 2-4 business days. Track your active orders in real time under the Orders tab.";
        } else if (query.contains("order") || query.contains("status")) {
            return "You can check order history, live status, and tracking updates directly in your Buyer Dashboard under Orders.";
        } else if (query.contains("product") || query.contains("search") || query.contains("buy")) {
            return "You can browse our catalog by category, search by keywords, and filter by price range on the Products page.";
        } else if (query.contains("contact") || query.contains("support")) {
            return "Our support team is available 24/7 at support@mohanmart.com.";
        }

        return "Hello! I am your MohanMart AI Assistant. How can I help you today with products, orders, shipping, or returns?";
    }
}
