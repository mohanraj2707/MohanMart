# MohanMart — Security Audit (§9) & Automated Testing Verification

## 1. Automated Test Suite Summary (`mvn -B clean verify`)

All automated tests run on **JUnit 5 Jupiter (`5.10.2`)** and **Mockito (`5.11.0`)** against an isolated in-memory H2 instance (`jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`) initialized from `schema.sql` and embedded Apache Tomcat 9.0.86 for full HTTP/JSP E2E tests.

| Test Layer | Test Classes | Tests Count | Coverage Focus |
| :--- | :--- | :---: | :--- |
| **AI Chatbot (O4)** | `MockChatProviderTest`, `GeminiChatProviderTest`, `ChatServiceTest`, `ChatServletTest` | 27 | 12 FAQ topics, PII redaction, model guard (`gemini-3.1-flash-lite`), 10/min rate limit (HTTP 429), session cache, failover |
| **DAO Layer (H2 JDBC)** | `UserDAOTest`, `ProductDAOTest`, `CartDAOTest`, `OrderDAOTest`, `ReviewDAOTest`, `DatabaseLayerIntegrationTest` | 29 | CRUD, FK constraints, unique email/review constraints, atomic stock updates, `DELIVERED` purchase check |
| **Service Layer** | `AuthServiceTest`, `UserServiceTest`, `ProductServiceTest`, `CartServiceTest`, `OrderServiceIntegrationTest`, `ReviewServiceTest` | 32 | Validation, 10-step ACID checkout commit & rollback, O2 status workflow (`409 Conflict`), verified review rules |
| **Controller & Filter Layer** | `AdminServletTest`, `CartServletTest`, `OrderServletTest`, `ProductServletTest`, `ReviewServletTest`, `SellerServletTest`, `HealthServletTest`, `AuthFilterTest`, `CsrfFilterTest` | 30 | JSON envelopes, HTTP status codes (`200`, `201`, `400`, `401`, `403`, `404`, `409`, `429`, `503`), RBAC, CSRF |
| **Model, DTO, Exception & Util** | `ModelTest`, `DtoTest`, `ExceptionTest`, `DatabaseMigrationRunnerTest`, `PasswordUtilTest`, `SecurityUtilTest`, `ValidationUtilTest` | 21 | BCrypt cost 12 hashing, CSRF token generation/verification, input validators, V1–V4 idempotent SQL migrations |
| **Embedded Tomcat 9 E2E** | `EndToEndBusinessFlowsTest`, `ProductAndCartFlowTest`, `TomcatVisualInspectionTest` | 32 | Full HTTP cookie/CSRF flows across Buyer, Seller, and Admin + JSP compilation & CSS/JS asset serving |
| **Total** | **29 Test Classes** | **171+ Tests** | **0 Failures, 0 Errors** |

---

## 2. Section 9 Security Checklist Verification Results

| # | Security Requirement (§9) | Verification Method & Command | Status |
| :--- | :--- | :--- | :---: |
| **S-01** | **Parameterized SQL Everywhere**: Every SQL execution uses `PreparedStatement`; zero `createStatement` or string-concatenated SQL. | `Select-String -Path "src\main\java\**\*.java" -Pattern "createStatement"` → **0 hits**. All 6 DAOs, `HealthServlet`, and `DatabaseMigrationRunner` use `PreparedStatement` in `try-with-resources`. | **PASS** |
| **S-02** | **Password Security**: Salted BCrypt hashing, never logged, never returned in DTOs (`UserResponseDTO` omits `passwordHash`). | Verified in `PasswordUtil.java` (jBCrypt cost factor 12), `UserResponseDTO.java`, and `PasswordUtilTest`. | **PASS** |
| **S-03** | **Session Fixation & Timeout**: Session ID rotated on login; `HttpOnly` cookie and 30-minute timeout configured. | Verified in `AuthServlet.java` (session invalidation/recreation on login) and `WEB-INF/web.xml` (`<session-timeout>30</session-timeout>`, `<http-only>true</http-only>`). | **PASS** |
| **S-04** | **RBAC & Protected Routes (`AuthFilter`)**: Unauthenticated users receive `401`/`302`; `BUYER` blocked from `/seller/*` and `/admin/*` (`403`); `SELLER` blocked from `/admin/*` (`403`). | Verified in `AuthFilter.java` and `AuthFilterTest` (7 automated RBAC scenarios). | **PASS** |
| **S-05** | **CSRF Protection (`CsrfFilter`)**: All state-changing `POST`/`PUT`/`DELETE` requests require valid session CSRF token (`_csrf` form field or `X-CSRF-Token` header). | Verified in `CsrfFilter.java` and `CsrfFilterTest`. | **PASS** |
| **S-06** | **XSS Escaping in JSP & JS**: All dynamic JSP output escaped with `<c:out>` (including `home.jsp`, `index.jsp`, `orders.jsp`, `product-list.jsp`, `product-detail.jsp`, `reviews.jsp`); chat widget uses `textContent` (never `innerHTML`). | Verified across all JSP views under `src/main/webapp/WEB-INF/views/` and `src/main/webapp/js/chat-widget.js`. | **PASS** |
| **S-07** | **Custom Error Pages (No Stack Traces)**: `400`, `403`, `404`, `500`, and `java.lang.Throwable` mapped in `web.xml` to clean JSP views with zero stack trace leakage. | Verified in `src/main/webapp/WEB-INF/web.xml` and `src/main/webapp/WEB-INF/views/error/*.jsp`. | **PASS** |
| **S-08** | **Zero Committed Secrets**: `.env` and `config.properties` gitignored; `GEMINI_API_KEY` read strictly from environment variables on the server. | Verified in `.gitignore`, `.env.example`, and `GeminiChatProvider.java`. | **PASS** |
| **S-09** | **Chatbot Abuse & Privacy Guards**: Input capped at 500 chars (`400`), rate-limited to 10 messages/min per session (`429`), PII stripped before external call. | Verified in `ChatServiceImpl.java`, `GeminiChatProvider.java`, `ChatServiceTest`, and `ChatServletTest`. | **PASS** |

---

## 3. Manual Penetration Test Payloads & Results

| Attack Vector | Payload Tested | Target Endpoint | Observed Result | Verdict |
| :--- | :--- | :--- | :--- | :---: |
| **SQL Injection (Auth Bypass)** | `email = ' OR 1=1 --` | `POST /login`, `POST /api/v1/auth/login` | Treated as literal string parameter by `PreparedStatement`; rejected with `400`/`401` invalid email/credentials | **PASS** |
| **SQL Injection (Catalog Search)** | `keyword = Laptop%' UNION SELECT 1,name,email,password_hash,1,1,1,1,1,1 FROM users --` | `GET /products?keyword=...` | Bound via `PreparedStatement.setString`; returns 0 products, zero data leakage | **PASS** |
| **Stored / Reflected XSS** | `<script>alert(1)</script>` in product name, review comment, and chat message | `POST /reviews`, `POST /seller/products`, `POST /api/v1/chat` | Escaped as `&lt;script&gt;alert(1)&lt;/script&gt;` via `<c:out>` in JSP and `textContent` in `chat-widget.js`; script never executes | **PASS** |
| **Horizontal / Vertical Privilege Escalation** | Buyer session calling `GET /admin/users` or `POST /seller/orders/status` | `/admin/*`, `/seller/*` | Blocked by `AuthFilter` and `SellerServlet.requireSeller` with HTTP `403 Forbidden` | **PASS** |
| **Cross-Site Request Forgery (CSRF)** | `POST /orders` or `POST /api/cart/add` without `X-CSRF-Token` / `_csrf` | State-changing endpoints | Blocked by `CsrfFilter` with HTTP `403 Forbidden` (`CSRF_INVALID`) | **PASS** |
