# Changelog

All notable changes to the **MohanMart** project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2026-10-09 (Final Review Release — AI Chatbot, Order Workflow & Cloud Readiness)

### Added
- **AI Marketplace Chatbot (O4 / §11 & §17)**:
  - `ChatProvider` strategy interface with `MockChatProvider` (12 marketplace/product FAQ handlers + dynamic catalog context enrichment + off-topic refusal) and `GeminiChatProvider` (`java.net.http.HttpClient` with 8s timeout, `gemini-3.1-flash-lite` default model, PII redaction, and automatic failover to `MockChatProvider`).
  - `ChatProviderFactory` selecting `gemini` or `mock` via `AI_CHATBOT_PROVIDER` / `ai.chatbot.provider`.
  - `ChatServiceImpl` enforcing input validation (1–500 chars), per-session rate limiting (10 messages/minute -> HTTP 429), per-session repeated-question caching, live catalog context injection, and static degraded fallback reply on provider exceptions.
  - Responsive floating chat widget (`src/main/webapp/js/chat-widget.js`) with quick FAQ chips and XSS-safe `textContent` rendering.
- **Health & Observability (§13 & §18)**:
  - `GET /api/v1/health` running `SELECT 1` via HikariCP pool and returning HTTP 200 `{"status":"UP","db":"UP"}` or HTTP 503 `{"status":"DOWN","db":"DOWN"}`.
  - `RequestIdFilter` / `LoggingFilter` binding UUID `requestId` to SLF4J MDC and emitting `X-Request-Id` response headers.
- **Order Status Workflow (O2) & Verified Reviews (F8)**:
  - Enforced strict order state transitions (`PENDING -> CONFIRMED -> SHIPPED -> DELIVERED` or `CANCELLED`), rejecting invalid backward transitions with `ConflictException` (HTTP 409).
  - Seller ownership verification on order dispatch updates and verified-purchase (`DELIVERED` order) enforcement on product reviews.
- **Database Migrations & Cloud Deployment**:
  - Added `V3__add_fk_indexes.sql`, `V4__add_order_status_check.sql`, `database/schema.sql`, `DaoFactory`, multi-stage `Dockerfile`, and PostgreSQL JDBC driver support for Render.
- **Documentation & Testing**:
  - Added `docs/FINAL_REPORT.md`, `docs/FINAL_REGRESSION.md`, `docs/DEMO_SCRIPT.md`, `docs/testing.md`, and expanded test suite to 176+ JUnit 5 + Mockito automated tests.

---

## [1.0.0] - 2026-09-21 (Sprint 3 Milestone Build)

### Added
- **Core Architecture**: Layered MVC application on Servlet 4.0, raw JDBC, HikariCP, and H2 database.
- **Security & Authorization**:
  - `AuthFilter` implementing Role-Based Access Control (RBAC) across `BUYER`, `SELLER`, and `ADMIN` endpoints.
  - `CsrfFilter` providing state-changing token validation and standard security headers.
  - BCrypt password hashing (cost factor 12) via jBCrypt and session fixation defense.
- **Buyer, Seller & Admin Flows (F1–F8)**:
  - Product catalog search, category/price/stock filtering, server-side cart, atomic checkout, seller operations hub, admin moderation console, and verified buyer reviews.

---

## [0.1.0] - 2026-07-27 (Sprint 0 MVP Baseline)

### Added
- Initial Maven WAR project skeleton, layered package structure (`controller`, `service`, `dao`, `model`, `dto`, `filter`, `listener`, `util`, `exception`), `db/schema.sql`, `db/seed.sql`, and custom error pages (`400`, `403`, `404`, `500`).
