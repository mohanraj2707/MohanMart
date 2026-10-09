package com.mohan.mohanmart.service.impl;

import com.mohan.mohanmart.ai.ChatProvider;
import com.mohan.mohanmart.ai.ChatProviderFactory;
import com.mohan.mohanmart.dao.impl.ProductDAOImpl;
import com.mohan.mohanmart.dto.PaginatedResult;
import com.mohan.mohanmart.dto.ProductDTO;
import com.mohan.mohanmart.exception.AppException;
import com.mohan.mohanmart.exception.RateLimitException;
import com.mohan.mohanmart.exception.ValidationException;
import com.mohan.mohanmart.service.ChatService;
import com.mohan.mohanmart.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Server-side implementation of {@link ChatService}.
 *
 * Features:
 * - Input validation: non-empty and max 500 characters.
 * - Per-session rate limit: maximum 10 messages per 60-second sliding window.
 * - Per-session in-memory cache of repeated identical questions.
 * - Optional live catalog context injection from {@link ProductService} (product name, category, price only — no PII).
 * - Automatic failover to static degraded reply if the underlying {@link ChatProvider} throws an exception.
 */
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    public static final int MAX_MESSAGE_LENGTH = 500;
    public static final int MAX_MESSAGES_PER_MINUTE = 10;
    public static final long RATE_LIMIT_WINDOW_MS = 60_000L;
    public static final String DEGRADED_FALLBACK_REPLY =
            "Our assistant is unavailable right now. Please browse Products at /products or contact support@mohanmart.com.";

    private final ProductService productService;
    private final ChatProvider chatProvider;

    private final Map<String, Deque<Long>> rateLimitBuckets = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> sessionCache = new ConcurrentHashMap<>();

    public ChatServiceImpl() {
        this(new ProductServiceImpl(new ProductDAOImpl()), ChatProviderFactory.create());
    }

    public ChatServiceImpl(ProductService productService) {
        this(productService, ChatProviderFactory.create());
    }

    public ChatServiceImpl(ProductService productService, ChatProvider chatProvider) {
        this.productService = productService;
        this.chatProvider = chatProvider != null ? chatProvider : ChatProviderFactory.create();
    }

    @Override
    public String processMessage(String userMessage, Long userId) throws AppException {
        String sessionKey = (userId != null) ? "user:" + userId : "guest";
        return processMessage(userMessage, sessionKey);
    }

    @Override
    public String processMessage(String userMessage, String sessionKey) throws AppException {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new ValidationException("Chat message cannot be empty", "EMPTY_MESSAGE");
        }

        String trimmed = userMessage.trim();
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            throw new ValidationException(
                    "Chat message cannot exceed " + MAX_MESSAGE_LENGTH + " characters",
                    "MESSAGE_TOO_LONG");
        }

        String bucketKey = (sessionKey != null && !sessionKey.trim().isEmpty()) ? sessionKey.trim() : "guest";
        enforceRateLimit(bucketKey);

        String normalizedQuestion = trimmed.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        Map<String, String> cacheForSession = sessionCache.computeIfAbsent(bucketKey, k -> new ConcurrentHashMap<>());
        String cachedReply = cacheForSession.get(normalizedQuestion);
        if (cachedReply != null) {
            logger.debug("Returning cached chat reply for session key {}", bucketKey);
            return cachedReply;
        }

        String catalogContext = buildCatalogContext(trimmed);

        try {
            String reply = chatProvider.getReply(trimmed, catalogContext);
            if (reply == null || reply.trim().isEmpty()) {
                return DEGRADED_FALLBACK_REPLY;
            }
            String cleanReply = reply.trim();
            cacheForSession.put(normalizedQuestion, cleanReply);
            return cleanReply;
        } catch (Exception e) {
            logger.warn("ChatProvider failed unexpectedly; returning degraded static fallback: {}", e.getMessage());
            return DEGRADED_FALLBACK_REPLY;
        }
    }

    private void enforceRateLimit(String bucketKey) throws RateLimitException {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = rateLimitBuckets.computeIfAbsent(bucketKey, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) >= RATE_LIMIT_WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_MESSAGES_PER_MINUTE) {
                throw new RateLimitException("Rate limit exceeded: maximum " + MAX_MESSAGES_PER_MINUTE
                        + " chat messages per minute allowed. Please wait a moment and try again.");
            }
            timestamps.addLast(now);
        }
    }

    private String buildCatalogContext(String userMessage) {
        if (productService == null) {
            return "";
        }
        try {
            String keyword = extractKeyword(userMessage);
            PaginatedResult<ProductDTO> result = productService.searchProducts(
                    keyword, null, null, null, true, null, 1, 3);
            if (result == null || result.getData() == null || result.getData().isEmpty()) {
                return "";
            }
            List<ProductDTO> topMatches = result.getData();
            return topMatches.stream()
                    .limit(3)
                    .map(p -> String.format("%s (%s, $%s)", p.getName(), p.getCategory(), p.getPrice()))
                    .collect(Collectors.joining("; "));
        } catch (Exception e) {
            logger.debug("Could not fetch catalog context for chat query: {}", e.getMessage());
            return "";
        }
    }

    private String extractKeyword(String message) {
        String cleaned = message.replaceAll("[^a-zA-Z0-9\\s]", " ").trim();
        String[] words = cleaned.split("\\s+");
        for (String w : words) {
            String lower = w.toLowerCase(Locale.ROOT);
            if (w.length() >= 4
                    && !lower.equals("what") && !lower.equals("have") && !lower.equals("show")
                    && !lower.equals("find") && !lower.equals("tell") && !lower.equals("about")
                    && !lower.equals("does") && !lower.equals("mohanmart") && !lower.equals("product")
                    && !lower.equals("products")) {
                return w;
            }
        }
        return null;
    }

    public void clearSessionState() {
        rateLimitBuckets.clear();
        sessionCache.clear();
    }
}
