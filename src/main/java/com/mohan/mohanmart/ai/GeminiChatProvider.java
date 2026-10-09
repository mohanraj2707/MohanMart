package com.mohan.mohanmart.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Server-side Gemini AI ChatProvider using {@link java.net.http.HttpClient}.
 *
 * Security & Privacy Guarantees:
 * - API key is read strictly from environment variable {@code GEMINI_API_KEY} (never logged, never exposed to JS/JSP).
 * - Model defaults to {@code gemini-3.1-flash-lite} and is configurable via {@code GEMINI_MODEL} (never uses deprecated gemini-2.5-*).
 * - 8-second connection and request timeout.
 * - Fixed system prompt restricts replies strictly to MohanMart marketplace and product topics.
 * - Sanitizes user input to strip PII (emails, phone/card digit sequences) before outbound call.
 * - Automatically fails over to {@link MockChatProvider} on timeout, HTTP 429, network error, or missing key.
 */
public class GeminiChatProvider implements ChatProvider {

    private static final Logger logger = LoggerFactory.getLogger(GeminiChatProvider.class);

    public static final String DEFAULT_MODEL = "gemini-3.1-flash-lite";
    private static final String ENDPOINT_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private static final String SYSTEM_PROMPT =
            "You are the official customer support assistant for MohanMart, a multi-seller e-commerce marketplace. "
            + "Answer ONLY questions related to MohanMart products, categories (Electronics, Books, Clothing, Home), "
            + "ordering, simulated escrow mock payments, shipping (2-4 business days), 7-day returns, order statuses "
            + "(PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED), verified product reviews (allowed only on DELIVERED orders), "
            + "and how sellers list products. "
            + "Politely refuse any off-topic questions unrelated to MohanMart. "
            + "Keep answers concise (under 80 words), helpful, and plain text only.";

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ChatProvider fallbackProvider;

    public GeminiChatProvider() {
        this(System.getenv("GEMINI_API_KEY"), resolveModel(System.getenv("GEMINI_MODEL")),
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), new MockChatProvider());
    }

    public GeminiChatProvider(String apiKey, String model, HttpClient httpClient, ChatProvider fallbackProvider) {
        this.apiKey = apiKey;
        this.model = resolveModel(model);
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.fallbackProvider = fallbackProvider != null ? fallbackProvider : new MockChatProvider();
    }

    public static String resolveModel(String configuredModel) {
        if (configuredModel == null || configuredModel.trim().isEmpty()) {
            return DEFAULT_MODEL;
        }
        String clean = configuredModel.trim();
        // Never allow deprecated gemini-2.5-* models (scheduled for shutdown Oct 16, 2026)
        if (clean.startsWith("gemini-2.5")) {
            return DEFAULT_MODEL;
        }
        return clean;
    }

    /**
     * Strips potential user PII (email addresses, phone/card digit strings) from user text
     * before sending to external free-tier LLM endpoint.
     */
    public static String sanitizePii(String input) {
        if (input == null) {
            return "";
        }
        String sanitized = input.replaceAll("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}", "[REDACTED_EMAIL]");
        sanitized = sanitized.replaceAll("\\b\\d{10,16}\\b", "[REDACTED_NUMBER]");
        return sanitized.trim();
    }

    @Override
    public String getReply(String userMessage, String context) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.debug("GEMINI_API_KEY is not configured; falling back to MockChatProvider");
            return fallbackProvider.getReply(userMessage, context);
        }

        try {
            String sanitizedQuestion = sanitizePii(userMessage);
            StringBuilder promptBuilder = new StringBuilder(SYSTEM_PROMPT);
            if (context != null && !context.trim().isEmpty()) {
                promptBuilder.append("\n\nAvailable MohanMart catalog context:\n").append(sanitizePii(context));
            }
            promptBuilder.append("\n\nCustomer question: ").append(sanitizedQuestion);

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", promptBuilder.toString());

            JsonArray partsArray = new JsonArray();
            partsArray.add(textPart);

            JsonObject contentObj = new JsonObject();
            contentObj.add("parts", partsArray);

            JsonArray contentsArray = new JsonArray();
            contentsArray.add(contentObj);

            JsonObject payload = new JsonObject();
            payload.add("contents", contentsArray);

            String endpoint = String.format(ENDPOINT_TEMPLATE, model);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("x-goog-api-key", apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();

            if (status == 429) {
                logger.warn("Gemini API rate limit reached (HTTP 429); falling back to MockChatProvider");
                return fallbackProvider.getReply(userMessage, context);
            }
            if (status < 200 || status >= 300) {
                logger.warn("Gemini API returned HTTP {}; falling back to MockChatProvider", status);
                return fallbackProvider.getReply(userMessage, context);
            }

            String reply = extractTextFromGeminiJson(response.body());
            if (reply != null && !reply.trim().isEmpty()) {
                return reply.trim();
            }

            logger.warn("Gemini API response did not contain candidate text; falling back to MockChatProvider");
            return fallbackProvider.getReply(userMessage, context);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.warn("Gemini API request interrupted; falling back to MockChatProvider");
            return fallbackProvider.getReply(userMessage, context);
        } catch (Exception e) {
            logger.warn("Gemini API invocation failed ({}); falling back to MockChatProvider", e.getClass().getSimpleName());
            return fallbackProvider.getReply(userMessage, context);
        }
    }

    static String extractTextFromGeminiJson(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return null;
        }
        JsonElement rootEl = JsonParser.parseString(rawJson);
        if (!rootEl.isJsonObject()) {
            return null;
        }
        JsonObject root = rootEl.getAsJsonObject();
        if (!root.has("candidates") || !root.get("candidates").isJsonArray()) {
            return null;
        }
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates.isEmpty()) {
            return null;
        }
        JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
        if (!firstCandidate.has("content") || !firstCandidate.get("content").isJsonObject()) {
            return null;
        }
        JsonObject content = firstCandidate.getAsJsonObject("content");
        if (!content.has("parts") || !content.get("parts").isJsonArray()) {
            return null;
        }
        JsonArray parts = content.getAsJsonArray("parts");
        if (parts.isEmpty()) {
            return null;
        }
        JsonObject firstPart = parts.get(0).getAsJsonObject();
        if (firstPart.has("text") && !firstPart.get("text").isJsonNull()) {
            return firstPart.get("text").getAsString();
        }
        return null;
    }

    public String getModel() {
        return model;
    }
}
