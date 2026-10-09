package com.mohan.mohanmart.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiChatProviderTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Test
    @DisplayName("Model resolution defaults to gemini-3.1-flash-lite and blocks deprecated gemini-2.5-*")
    void testModelResolution() {
        assertEquals("gemini-3.1-flash-lite", GeminiChatProvider.resolveModel(null));
        assertEquals("gemini-3.1-flash-lite", GeminiChatProvider.resolveModel(""));
        assertEquals("gemini-3.1-flash-lite", GeminiChatProvider.resolveModel("gemini-2.5-flash"));
        assertEquals("gemini-3.1-flash-lite", GeminiChatProvider.resolveModel("gemini-2.5-pro"));
        assertEquals("gemini-3.1-flash-lite", GeminiChatProvider.resolveModel("gemini-3.1-flash-lite"));
    }

    @Test
    @DisplayName("PII sanitizer strips email addresses and long digit sequences")
    void testSanitizePii() {
        String raw = "My email is alice@example.com and my card is 4111222233334444. Where is my order?";
        String sanitized = GeminiChatProvider.sanitizePii(raw);
        assertFalse(sanitized.contains("alice@example.com"));
        assertFalse(sanitized.contains("4111222233334444"));
        assertTrue(sanitized.contains("[REDACTED_EMAIL]"));
        assertTrue(sanitized.contains("[REDACTED_NUMBER]"));
    }

    @Test
    @DisplayName("Missing API key falls back cleanly to MockChatProvider")
    void testFallbackWhenApiKeyMissing() {
        GeminiChatProvider provider = new GeminiChatProvider("", "gemini-3.1-flash-lite", httpClient, new MockChatProvider());
        String reply = provider.getReply("What is your return policy?", null);
        assertTrue(reply.contains("7-day"));
        verifyNoInteractions(httpClient);
    }

    @Test
    @DisplayName("Successful HTTP 200 Gemini JSON response extracts candidate text")
    @SuppressWarnings("unchecked")
    void testSuccessResponseParsing() throws Exception {
        String geminiJson = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"MohanMart ships within 2-4 business days.\"}]}}]}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(geminiJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        GeminiChatProvider provider = new GeminiChatProvider("test-key", "gemini-3.1-flash-lite", httpClient, new MockChatProvider());
        String reply = provider.getReply("How fast is shipping?", "Keyboard (Electronics, $99.00)");
        assertEquals("MohanMart ships within 2-4 business days.", reply);
    }

    @Test
    @DisplayName("HTTP 429 or network timeout falls back to MockChatProvider without throwing")
    @SuppressWarnings("unchecked")
    void testFailoverOn429AndException() throws Exception {
        when(httpResponse.statusCode()).thenReturn(429);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        GeminiChatProvider provider = new GeminiChatProvider("test-key", "gemini-3.1-flash-lite", httpClient, new MockChatProvider());
        String reply429 = provider.getReply("How do reviews work?", null);
        assertTrue(reply429.contains("DELIVERED"));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection timed out"));
        String replyTimeout = provider.getReply("How to order?", null);
        assertTrue(replyTimeout.contains("/checkout"));
    }

    @Test
    @DisplayName("ChatProviderFactory creates MockChatProvider by default and GeminiChatProvider when configured")
    void testFactorySelection() {
        ChatProvider mock = ChatProviderFactory.create("mock", null, null);
        assertInstanceOf(MockChatProvider.class, mock);

        ChatProvider defaultProvider = ChatProviderFactory.create(null, null, null);
        assertInstanceOf(MockChatProvider.class, defaultProvider);

        ChatProvider gemini = ChatProviderFactory.create("gemini", "dummy-key", "gemini-3.1-flash-lite");
        assertInstanceOf(GeminiChatProvider.class, gemini);
    }
}
