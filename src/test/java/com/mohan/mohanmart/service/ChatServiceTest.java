package com.mohan.mohanmart.service;

import com.mohan.mohanmart.ai.ChatProvider;
import com.mohan.mohanmart.dto.PaginatedResult;
import com.mohan.mohanmart.dto.ProductDTO;
import com.mohan.mohanmart.exception.AppException;
import com.mohan.mohanmart.exception.RateLimitException;
import com.mohan.mohanmart.exception.ValidationException;
import com.mohan.mohanmart.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private ChatProvider chatProvider;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(productService, chatProvider);
    }

    @Test
    @DisplayName("Rejects null, blank, or >500 character messages with ValidationException")
    void testInputValidation() {
        assertThrows(ValidationException.class, () -> chatService.processMessage(null, "sess-1"));
        assertThrows(ValidationException.class, () -> chatService.processMessage("   ", "sess-1"));

        String tooLong = "a".repeat(501);
        ValidationException ex = assertThrows(ValidationException.class,
                () -> chatService.processMessage(tooLong, "sess-1"));
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("MESSAGE_TOO_LONG") || ex.getField().contains("500"));
    }

    @Test
    @DisplayName("Caches repeated identical questions per session without re-invoking provider")
    void testSessionQuestionCache() throws AppException {
        when(chatProvider.getReply(eq("How do returns work?"), anyString()))
                .thenReturn("7-day hassle-free returns.");

        String first = chatService.processMessage("How do returns work?", "session-cache-test");
        String second = chatService.processMessage("  how do   returns work? ", "session-cache-test");

        assertEquals("7-day hassle-free returns.", first);
        assertEquals("7-day hassle-free returns.", second);
        verify(chatProvider, times(1)).getReply(eq("How do returns work?"), anyString());
    }

    @Test
    @DisplayName("Enforces per-session rate limit of 10 messages per minute")
    void testPerSessionRateLimit() throws AppException {
        when(chatProvider.getReply(anyString(), anyString())).thenReturn("OK");

        String sessionKey = "session-rate-limit";
        for (int i = 1; i <= 10; i++) {
            String reply = chatService.processMessage("Question #" + i, sessionKey);
            assertEquals("OK", reply);
        }

        // 11th message within the same minute must throw RateLimitException (HTTP 429)
        assertThrows(RateLimitException.class,
                () -> chatService.processMessage("Question #11", sessionKey));

        // Different session is unaffected
        assertEquals("OK", chatService.processMessage("Question #1", "other-session"));
    }

    @Test
    @DisplayName("Returns degraded static reply when ChatProvider throws an unexpected exception")
    void testProviderExceptionFallback() throws AppException {
        when(chatProvider.getReply(anyString(), anyString()))
                .thenThrow(new RuntimeException("Upstream LLM failure"));

        String reply = chatService.processMessage("Tell me about shipping", "session-fallback");
        assertEquals(ChatServiceImpl.DEGRADED_FALLBACK_REPLY, reply);
    }

    @Test
    @DisplayName("Injects matching product catalog context into ChatProvider")
    void testCatalogContextInjection() throws AppException {
        ProductDTO dto = new ProductDTO();
        dto.setName("Mechanical Keyboard");
        dto.setCategory("Electronics");
        dto.setPrice(new BigDecimal("129.99"));

        when(productService.searchProducts(eq("keyboard"), isNull(), eq(1), eq(3)))
                .thenReturn(List.of(dto));
        when(chatProvider.getReply(eq("Find keyboard"), contains("Mechanical Keyboard (Electronics, $129.99)")))
                .thenReturn("Found Mechanical Keyboard for $129.99");

        String reply = chatService.processMessage("Find keyboard", 42L);
        assertEquals("Found Mechanical Keyboard for $129.99", reply);
    }
}
