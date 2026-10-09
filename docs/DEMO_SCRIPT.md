# MohanMart — Rehearsed 2–3 Minute Viva & Demo Script

**Target Duration**: 2 minutes 45 seconds  
**Pre-Demo Setup**:
1. Application running locally (`http://localhost:8080/mohanmart`) and deployed on Render (`<LIVE_URL>`).
2. Browser Tab 1: `/home` (Buyer view).
3. Browser Tab 2: `/api/v1/health` (JSON health endpoint).
4. Terminal: `mvn -B clean verify` completed with **176 tests passing, 0 failures**.

---

## 0:00 – 0:30 | Architecture, Health Check & Automated Verification
- **Show**: Terminal with `mvn -B clean verify` green build and Tab 2 (`/api/v1/health`).
- **Say**:
  > "Good morning. This is **MohanMart**, a multi-seller e-commerce marketplace built on Java 17, Servlet 4.0, JSP/JSTL, and raw JDBC with HikariCP. It follows a strict Controller → Service → DAO architecture with 176 JUnit 5 and Mockito tests. Here is `/api/v1/health` returning HTTP 200 with `db: UP` via our connection pool and `X-Request-Id` tracing."

---

## 0:30 – 1:15 | Buyer Journey: Faceted Search, Server Cart & Atomic Checkout (F1, F3, F4, F5)
- **Show**: Tab 1 (`/home` → `/products`). Filter by `Electronics`, search `"Keyboard"`, add item to cart, open `/cart`, proceed to `/checkout`, and click **Place Order**.
- **Say**:
  > "Signing in as a Buyer rotates the session ID to prevent session fixation and verifies BCrypt-hashed credentials. On the catalog, I can filter by category, price, and stock. Adding an item updates our server-side cart. When I submit checkout with a CSRF token, `OrderServiceImpl` executes a 10-step atomic JDBC transaction (`setAutoCommit(false)`): it re-reads authoritative database prices, verifies stock, inserts the order and line items, decrements product inventory, and clears the cart before committing."

---

## 1:15 – 1:55 | Seller Fulfillment Workflow (F2, O2) & Verified Buyer Review (F8)
- **Show**: Log in as Seller (`seller1@mohanmart.com`) → `/seller/dashboard` (show KPI cards: Active Listings, Gross Sales, Low Stock Warnings) → `/seller/orders`. Advance the new order from `CONFIRMED` to `SHIPPED` and then `DELIVERED`. Switch back to Buyer and submit a 5-star review on `/reviews`.
- **Say**:
  > "In the Seller Operations Hub, the merchant sees real-time revenue KPIs and assigned orders. Advancing the order follows our strict O2 state machine: `CONFIRMED → SHIPPED → DELIVERED`—any invalid backward transition is rejected with HTTP 409 Conflict. Once the order is `DELIVERED`, the buyer is eligible under F8 to submit a single verified product review, which is escaped with JSTL `<c:out>`."

---

## 1:55 – 2:45 | Admin Console (F7) & AI Marketplace Assistant Chatbot (O4)
- **Show**: Open `/admin/dashboard` (platform GMV, user governance, order moderation). Then click the floating **Ask MohanMart AI** button in the bottom-right corner, click `"How does mock payment work?"`, and ask `"Do you have headphones?"`.
- **Say**:
  > "Admins monitor platform-wide GMV, users, and listings. Finally, our floating AI Chatbot widget calls `POST /api/v1/chat`. On the server, `ChatServiceImpl` validates input length, enforces a 10-messages-per-minute session rate limit, caches repeated questions, injects live product catalog context, and delegates to `ChatProviderFactory`. When configured with `GeminiChatProvider` (`gemini-3.1-flash-lite`), it strips any PII and enforces an 8-second timeout, automatically failing over to `MockChatProvider` if rate-limited or offline so the user never sees an error page."
