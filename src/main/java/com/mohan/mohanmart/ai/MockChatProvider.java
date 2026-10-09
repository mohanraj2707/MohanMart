package com.mohan.mohanmart.ai;

import java.util.Locale;

/**
 * Deterministic, zero-latency fallback and local development ChatProvider.
 * Provides comprehensive canned answers for at least 10 FAQ-style product and marketplace questions,
 * enriches replies with live catalog context when available, and refuses off-topic queries.
 */
public class MockChatProvider implements ChatProvider {

    @Override
    public String getReply(String userMessage, String context) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Hello! I am the MohanMart Assistant. Ask me about products, categories, ordering, mock payments, shipping, returns, order statuses, reviews, or selling on MohanMart.";
        }

        String q = userMessage.trim().toLowerCase(Locale.ROOT);

        // 1. Off-topic guard
        if (q.contains("weather") || q.contains("politics") || q.contains("movie")
                || q.contains("cricket") || q.contains("stock market") || q.contains("write code")) {
            return "I can only assist with MohanMart marketplace questions, such as browsing products, placing orders, shipping, returns, reviews, and seller listings.";
        }

        // 2. Payment / Mock / Escrow FAQ
        if (q.contains("payment") || q.contains("pay") || q.contains("mock") || q.contains("escrow")
                || q.contains("card") || q.contains("upi") || q.contains("checkout")) {
            if (q.contains("how to order") || q.contains("place order") || q.contains("how do i buy")) {
                return "To place an order on MohanMart: (1) Browse /products and click 'Add to Cart', (2) Open /cart to review quantities, and (3) Proceed to /checkout, enter your delivery address, and confirm via our Simulated Escrow Gateway.";
            }
            return "MohanMart uses a Simulated Escrow Payment Gateway (mock payment) for checkout. No real credit card or bank account is charged; once confirmed, your order moves to CONFIRMED status and stock is reserved atomically.";
        }

        // 3. How to Order / Buy FAQ
        if (q.contains("how to order") || q.contains("how do i order") || q.contains("place an order")
                || q.contains("place order") || q.contains("how to buy") || q.contains("buying")) {
            return "To order on MohanMart: (1) Browse or search the catalog at /products, (2) Add items to your cart, (3) Review your cart at /cart, and (4) Complete checkout at /checkout using our simulated escrow payment.";
        }

        // 4. Returns / Refunds / Cancellation FAQ
        if (q.contains("return") || q.contains("refund") || q.contains("cancel") || q.contains("exchange")) {
            return "MohanMart offers a hassle-free 7-day return policy on eligible items. You can also cancel any PENDING or CONFIRMED order directly from your Orders page (/orders), which immediately restores product inventory.";
        }

        // 5. Seller Listing / How Sellers List Products FAQ
        if (q.contains("seller") || q.contains("sell") || q.contains("list product")
                || q.contains("listing") || q.contains("merchant") || q.contains("vendor")) {
            return "Sellers can list products by registering with the SELLER role, visiting the Seller Operations Hub (/seller/dashboard or /seller/products), and submitting the product name, category, price, stock quantity, description, and image URL.";
        }

        // 6. Categories Available FAQ
        if (q.contains("categor") || q.contains("what do you sell") || q.contains("types of products")
                || q.contains("departments")) {
            String base = "MohanMart features curated products across four main categories: Electronics, Books, Clothing, and Home. You can filter by category, price range, and in-stock status on the /products page.";
            return appendContext(base, context);
        }

        // 7. Reviews & Ratings FAQ
        if (q.contains("review") || q.contains("rating") || q.contains("star") || q.contains("feedback")) {
            return "Only verified buyers who have received a product in a DELIVERED order can submit a review (1 to 5 stars with a comment). Each buyer may submit one review per product to ensure authentic marketplace ratings.";
        }

        // 8. Order Status Meanings / Workflow FAQ
        if (q.contains("status") || q.contains("pending") || q.contains("confirmed")
                || q.contains("shipped") || q.contains("delivered") || q.contains("track")) {
            return "MohanMart order statuses follow a strict workflow: PENDING (initial creation) -> CONFIRMED (mock payment verified) -> SHIPPED (dispatched by the seller) -> DELIVERED (completed; eligible for product reviews), or CANCELLED (cancelled before shipment with stock restored).";
        }

        // 9. Shipping & Delivery FAQ
        if (q.contains("ship") || q.contains("deliver") || q.contains("transit") || q.contains("how long")) {
            return "Standard shipping on MohanMart takes 2-4 business days. Sellers update dispatch status from CONFIRMED to SHIPPED and DELIVERED, which you can track live on your /orders page.";
        }

        // 10. Contact & Customer Support FAQ
        if (q.contains("contact") || q.contains("support") || q.contains("help") || q.contains("email")
                || q.contains("phone") || q.contains("customer service")) {
            return "You can reach MohanMart Customer Support 24/7 at support@mohanmart.com or use this assistant anytime for help with orders, products, and account questions.";
        }

        // 11. Account / Registration / Login / Password FAQ
        if (q.contains("register") || q.contains("sign up") || q.contains("signup") || q.contains("login")
                || q.contains("sign in") || q.contains("account") || q.contains("password")) {
            return "You can create a BUYER or SELLER account at /register and sign in at /login. All passwords are encrypted with salted BCrypt (cost 12), and sessions are protected with session-ID rotation and a 30-minute timeout.";
        }

        // 12. Cart / Stock / Price / Product Catalog Inquiry
        if (q.contains("cart") || q.contains("bag") || q.contains("wishlist")) {
            return "Your shopping cart (/cart) persists your selected items on the server and validates live stock and authoritative prices during checkout.";
        }

        if (context != null && !context.trim().isEmpty()) {
            return "Here is what I found in the MohanMart catalog matching your query: " + context.trim()
                    + " You can view full specifications and add items to your cart on the /products page.";
        }

        if (q.contains("product") || q.contains("search") || q.contains("price") || q.contains("available")
                || q.contains("stock") || q.contains("laptop") || q.contains("book") || q.contains("headphone")
                || q.contains("keyboard") || q.contains("watch")) {
            return "You can explore our catalog at /products using keyword search, category filters (Electronics, Books, Clothing, Home), price range filters, and sorting.";
        }

        return "Hello! I am your MohanMart AI Assistant. I can help you with: (1) how to order, (2) mock escrow payment, (3) shipping & delivery, (4) returns & cancellations, (5) order status meanings, (6) verified reviews, (7) available categories, (8) how sellers list products, (9) account registration, or (10) contacting support@mohanmart.com.";
    }

    private String appendContext(String baseReply, String context) {
        if (context != null && !context.trim().isEmpty()) {
            return baseReply + " Matching catalog highlights: " + context.trim();
        }
        return baseReply;
    }
}
