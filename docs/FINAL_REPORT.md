# MohanMart — Final Capstone Project Report

**Course**: Anna University R2025 Semester 3 — Object-Oriented Programming & Java Enterprise Capstone  
**Project**: MohanMart (`com.mohan.mohanmart`)  
**Date**: October 2026  

---

## 1. Executive Summary & Problem Statement

Small independent sellers and artisans need a reliable multi-seller marketplace where they can list products, manage stock, and fulfill customer orders without building custom e-commerce infrastructure. Buyers require faceted discovery, authoritative pricing, atomic stock reservation at checkout, transparent order status tracking (`PENDING → CONFIRMED → SHIPPED → DELIVERED`), authentic reviews restricted to verified purchasers, and instant customer support via an AI assistant.

**MohanMart** solves this using pure Java 17, Servlet 4.0 (`javax.servlet.*`), JSP 2.3 / JSTL 1.2, raw JDBC with HikariCP connection pooling, and H2 (local/testing) + PostgreSQL (Render cloud deployment).

---

## 2. Layered System Architecture

MohanMart enforces strict separation of concerns across five layers:

1. **Security & Observability Filter Pipeline (`com.mohan.mohanmart.filter`)**:
   - `EncodingFilter`: Enforces UTF-8 character encoding on every request and response.
   - `LoggingFilter` / `RequestIdFilter`: Generates a UUID `requestId` per request, binds it to SLF4J MDC (`%X{requestId}`), and emits the `X-Request-Id` header.
   - `CsrfFilter`: Generates per-session cryptographic CSRF tokens, validates `X-CSRF-Token` / `_csrf` on state-changing requests (`POST`, `PUT`, `DELETE`), and sets `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, and `X-XSS-Protection: 1; mode=block`.
   - `AuthFilter`: Enforces session authentication and Role-Based Access Control (`BUYER`, `SELLER`, `ADMIN`).
2. **Thin Controller / Servlet Layer (`com.mohan.mohanmart.controller`)**:
   - `BaseServlet`, `AuthServlet`, `ProductServlet`, `CartServlet`, `OrderServlet`, `ReviewServlet`, `SellerServlet`, `AdminServlet`, `ChatServlet`, `HealthServlet`.
   - Parses HTTP/JSON inputs, delegates business rules to services, and renders JSP views or standard `ApiResponse<T>` JSON envelopes.
3. **Service Layer (`com.mohan.mohanmart.service` & `service.impl`)**:
   - `AuthServiceImpl`, `UserServiceImpl`, `ProductServiceImpl`, `CartServiceImpl`, `OrderServiceImpl`, `ReviewServiceImpl`, `ChatServiceImpl`.
   - Validates all inputs at the top of each method, coordinates multi-DAO ACID transactions, enforces order status transitions, and manages chatbot rate-limiting/caching.
4. **AI Provider Sub-Layer (`com.mohan.mohanmart.ai`)**:
   - `ChatProvider` interface, `ChatProviderFactory`, `GeminiChatProvider` (`gemini-3.1-flash-lite` over `HttpClient` with 8s timeout and PII redaction), and `MockChatProvider`.
5. **DAO & Persistence Layer (`com.mohan.mohanmart.dao` & `dao.impl`)**:
   - `DaoFactory` + `UserDAO`, `ProductDAO`, `CartDAO`, `OrderDAO`, `OrderItemDAO`, `ReviewDAO`.
   - Every query uses parameterized `PreparedStatement` inside `try-with-resources` over connections borrowed from `DatabaseUtil` (`HikariDataSource`).

---

## 3. Database Schema & Migrations

Full DDL is maintained in `db/schema.sql`, `database/schema.sql`, and versioned migrations under `db/migrations/`:
- `V1__init_schema.sql`: Creates `users`, `products`, `orders`, `order_items`, `cart_items`, and `reviews` with `CHECK` constraints (`role IN ('BUYER','SELLER','ADMIN')`, `status IN ('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED')`, `rating BETWEEN 1 AND 5`, non-negative prices/quantities) and `UNIQUE` constraints (`uq_users_email`, `uq_cart_user_product`, `uq_reviews_user_product`).
- `V2__initial_seed.sql`: Seeds initial accounts, sample products across 4 categories, sample delivered order, and verified reviews.
- `V3__add_fk_indexes.sql`: Ensures indexes on all foreign keys (`seller_id`, `buyer_id`, `order_id`, `product_id`, `user_id`).
- `V4__add_order_status_check.sql`: Adds composite index `idx_orders_buyer_status` on `orders(buyer_id, status)` for verified-purchase review eligibility queries.

> **Documented Schema Notes**:
> - Monetary columns use `DECIMAL(12,2)` (a safe superset of `DECIMAL(10,2)` to prevent overflow on high-value electronics orders and GMV aggregation).
> - Server-side cart items are stored in `cart_items(id, user_id, product_id, quantity)` with `UNIQUE(user_id, product_id)`, avoiding an unnecessary join table while preserving per-user server-side persistence.

---

## 4. Design Patterns Implemented

| Design Pattern | Classes / Packages | Rationale |
| :--- | :--- | :--- |
| **DAO (Data Access Object)** | `UserDAO`, `ProductDAO`, `CartDAO`, `OrderDAO`, `ReviewDAO` + `*DAOImpl` | Isolates raw JDBC SQL from business logic and enables Mockito unit testing of services. |
| **Factory Pattern** | `DaoFactory`, `ChatProviderFactory` | Decouples callers from concrete DAO implementations and selects `GeminiChatProvider` vs `MockChatProvider` via configuration (`AI_CHATBOT_PROVIDER`). |
| **Strategy Pattern** | `ChatProvider` (`GeminiChatProvider`, `MockChatProvider`) | Allows seamless runtime switching and automatic failover between live LLM inference and deterministic FAQ responses. |
| **Singleton Pool** | `AppContextListener` + `DatabaseUtil` (`HikariDataSource`) | Ensures a single, lifecycle-managed connection pool across the entire web application. |
| **Template / Front Controller Base** | `BaseServlet` | Centralizes JSON reading/writing, `ApiResponse<T>` envelope formatting, and exception-to-HTTP-status mapping (`400`, `401`, `403`, `404`, `409`, `429`, `500`). |
| **DTO / Builder Conversion** | `ApiResponse`, `OrderResponse`, `ProductDTO`, `ReviewResponse`, `UserResponseDTO` | Prevents internal entity leakage (such as `passwordHash`) in API and session payloads. |

---

## 5. Key Technical Decisions

1. **10-Step Atomic Checkout (`OrderServiceImpl.checkout`)**: Uses explicit JDBC transaction control (`conn.setAutoCommit(false)`, `conn.commit()`, `conn.rollback()`) to re-read authoritative database prices, verify stock, insert `orders` and `order_items`, decrement `products.stock_qty`, and clear `cart_items` atomically.
2. **Privacy & Failover in AI Chatbot (`GeminiChatProvider` & `ChatServiceImpl`)**:
   - Uses `gemini-3.1-flash-lite` (avoiding deprecated `gemini-2.5-*` models).
   - Redacts email and numeric PII patterns before any external HTTP call and never includes user session/order PII.
   - Enforces a 10 messages/minute per-session rate limit and caches repeated questions per session.
   - Falls back automatically to `MockChatProvider` / static degraded message on HTTP 429, timeout (8s), or missing API key.

---

## 6. Known Limitations & Future Enhancements

- **In-Memory Rate Limiting & Chat Cache**: `ChatServiceImpl` stores rate-limit buckets and session caches in `ConcurrentHashMap`, which is ideal for single-instance deployment (Render / Tomcat) but would move to Redis in a multi-node cluster.
- **Simulated Escrow Payment**: Checkout uses a mock escrow confirmation step rather than a live Stripe/Razorpay webhook, as required by the academic specification.
- **Image Hosting**: Product listings support image URLs with server-side validation rather than external S3 object storage.
