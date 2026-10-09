package com.mohan.mohanmart.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockChatProviderTest {

    private MockChatProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockChatProvider();
    }

    @Test
    @DisplayName("FAQ 1: How to order / place an order")
    void testHowToOrderFaq() {
        String reply = provider.getReply("How do I order a product?", null);
        assertTrue(reply.contains("/products"));
        assertTrue(reply.contains("/checkout"));
    }

    @Test
    @DisplayName("FAQ 2: Mock payment / escrow gateway explanation")
    void testPaymentFaq() {
        String reply = provider.getReply("How does payment work? Is it mock?", null);
        assertTrue(reply.toLowerCase().contains("escrow") || reply.toLowerCase().contains("mock"));
        assertTrue(reply.contains("CONFIRMED"));
    }

    @Test
    @DisplayName("FAQ 3: Returns, refunds, and order cancellation policy")
    void testReturnsAndRefundsFaq() {
        String reply = provider.getReply("What is your return and refund policy?", null);
        assertTrue(reply.contains("7-day"));
        assertTrue(reply.contains("/orders"));
    }

    @Test
    @DisplayName("FAQ 4: How sellers list products in Seller Hub")
    void testHowSellersListProductsFaq() {
        String reply = provider.getReply("How do sellers list products on MohanMart?", null);
        assertTrue(reply.contains("SELLER"));
        assertTrue(reply.contains("/seller/"));
    }

    @Test
    @DisplayName("FAQ 5: Categories available on the marketplace")
    void testCategoriesAvailableFaq() {
        String reply = provider.getReply("What categories are available?", "Laptop (Electronics, $999.00)");
        assertTrue(reply.contains("Electronics"));
        assertTrue(reply.contains("Books"));
        assertTrue(reply.contains("Clothing"));
        assertTrue(reply.contains("Home"));
        assertTrue(reply.contains("Laptop"));
    }

    @Test
    @DisplayName("FAQ 6: Verified reviews and ratings rules")
    void testReviewsFaq() {
        String reply = provider.getReply("How do product reviews and star ratings work?", null);
        assertTrue(reply.contains("DELIVERED"));
        assertTrue(reply.contains("1 to 5 stars"));
    }

    @Test
    @DisplayName("FAQ 7: Order status meanings and workflow")
    void testOrderStatusMeaningsFaq() {
        String reply = provider.getReply("What do the order status values mean?", null);
        assertTrue(reply.contains("PENDING"));
        assertTrue(reply.contains("CONFIRMED"));
        assertTrue(reply.contains("SHIPPED"));
        assertTrue(reply.contains("DELIVERED"));
        assertTrue(reply.contains("CANCELLED"));
    }

    @Test
    @DisplayName("FAQ 8: Customer support and contact information")
    void testContactSupportFaq() {
        String reply = provider.getReply("How can I contact customer support?", null);
        assertTrue(reply.contains("support@mohanmart.com"));
    }

    @Test
    @DisplayName("FAQ 9: Shipping and delivery timelines")
    void testShippingAndDeliveryFaq() {
        String reply = provider.getReply("How long does shipping and delivery take?", null);
        assertTrue(reply.contains("2-4 business days"));
    }

    @Test
    @DisplayName("FAQ 10: Account registration, login, and BCrypt security")
    void testAccountAndRegistrationFaq() {
        String reply = provider.getReply("How do I register an account or sign in?", null);
        assertTrue(reply.contains("/register"));
        assertTrue(reply.contains("BCrypt"));
    }

    @Test
    @DisplayName("FAQ 11: Off-topic questions are politely refused")
    void testOffTopicRefused() {
        String reply = provider.getReply("What is the weather forecast tomorrow?", null);
        assertTrue(reply.contains("only assist with MohanMart marketplace questions"));
    }

    @Test
    @DisplayName("FAQ 12: Catalog context is injected when matching products exist")
    void testCatalogContextReply() {
        String reply = provider.getReply("Do you have wireless headphones?", "Studio Headphones (Electronics, $149.99)");
        assertTrue(reply.contains("Studio Headphones"));
        assertTrue(reply.contains("$149.99"));
    }
}
