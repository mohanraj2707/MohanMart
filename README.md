# MohanMart — Multi-Seller E-Commerce Marketplace

**Anna University R2025 Semester 3 JAVA Capstone Project**  
*Package: `com.mohan.mohanmart` · Java 17 LTS · Servlets 4.0 / JSP 2.3 / JSTL 1.2 · Apache Tomcat 9.0.x · Raw JDBC + HikariCP · H2 & PostgreSQL · Gemini 3.1 Flash Lite AI Chatbot*

- **Live Deployment URL**: `<LIVE_URL>` *(Deploy on Render via `render.yaml` Blueprint or `Dockerfile`)*
- **Health Check Endpoints**:
  - `<LIVE_URL>/api/v1/health` (`GET` → `200 OK` `{"status":"UP","db":"UP"}`)
  - `<LIVE_URL>/api/health` (`GET` → `200 OK` `{"status":"UP","db":"UP"}`)
  - `<LIVE_URL>/health` (`GET` → `200 OK` `{"status":"UP","db":"UP"}`)

---

## 1. Problem Statement & Project Overview

**MohanMart** is a production-grade, multi-seller e-commerce marketplace built strictly with core Java EE 8 / Servlet 4.0 fundamentals (no Spring, no Hibernate/JPA, no heavy frontend frameworks). Independent artisans and sellers can publish and manage product listings, buyers can browse, search, filter, manage server-side carts, execute atomic ACID checkouts via a simulated escrow payment gateway, track order statuses (`PENDING → CONFIRMED → SHIPPED → DELIVERED`), and submit verified product reviews, while administrators govern accounts, listings, and platform orders.

In addition, **MohanMart** includes an integrated, privacy-preserving **AI Marketplace Assistant Chatbot (O4)** powered by `ChatProviderFactory` (`GeminiChatProvider` using `gemini-3.1-flash-lite` with an 8-second timeout, PII redaction, and automatic failover to `MockChatProvider`).

---

## 2. Technology Stack

| Layer / Component | Technology & Version | Purpose |
| :--- | :--- | :--- |
| **Language & Runtime** | Java 17 LTS | Core language, `java.net.http.HttpClient`, records/streams |
| **Servlet Container** | Apache Tomcat 9.0.x | Java EE 8 Servlet 4.0 (`javax.servlet.*`), JSP 2.3, JSTL 1.2 |
| **Build & Quality** | Apache Maven 3.9+ | WAR packaging, Surefire 3.2.5, Checkstyle, SpotBugs |
| **Database** | H2 2.2.x (local/test) & PostgreSQL 42.7.2 (Render) | Relational persistence with versioned SQL migrations (`V1`–`V4`) & `DatabaseSeeder` |
| **Connection Pool** | HikariCP 5.1.0 | Single pool managed by `AppContextListener` + `DatabaseConfigResolver` |
| **JSON Serialization** | Google Gson 2.10.1 | REST `/api/v1/...` request/response serialization |
| **Security** | jBCrypt 0.4 + Custom Filters | Salted BCrypt (cost 12), `AuthFilter` RBAC, `CsrfFilter`, `<c:out>` XSS defense |
| **AI Assistant (O4)** | `GeminiChatProvider` / `MockChatProvider` | `gemini-3.1-flash-lite` via REST + 10 msg/min session rate limiter & cache |
| **Logging & Tracing** | SLF4J 2.0.12 + Logback 1.5.3 | Structured logging with `RequestIdFilter` UUID in MDC (`%X{requestId}`) |
| **Testing** | JUnit 5 Jupiter 5.10.2 + Mockito 5.11.0 | 188 unit, DAO, service, controller, filter, and embedded Tomcat E2E tests |
| **CI / CD & Cloud** | GitHub Actions + Docker + Render Blueprint | Automated `mvn -B clean verify`, multi-stage `Dockerfile`, and `render.yaml` |

---

## 3. System Architecture & Diagrams

- **[D1: Entity-Relationship Diagram (ERD)](docs/diagrams/D1_er_diagram.md)**
- **[D2: Use Case Diagram (Buyer, Seller, Admin & AI Chatbot)](docs/diagrams/D2_use_case_diagram.md)**
- **[D3: Place Order Transactional Sequence Diagram](docs/diagrams/D3_sequence_place_order.md)**
- **[D4: Layered Architecture Diagram](docs/diagrams/D4_architecture_diagram.md)**

```mermaid
flowchart TD
    Client["Browser (JSP/JSTL Views, Classical CSS, Vanilla JS Fetch, Floating Chat Widget)"]
    Filters["Filter Pipeline: EncodingFilter -> LoggingFilter/RequestIdFilter -> CsrfFilter -> AuthFilter"]
    Servlets["Servlets: AuthServlet, ProductServlet, CartServlet, OrderServlet, ReviewServlet, SellerServlet, AdminServlet, ChatServlet, HealthServlet"]
    Services["Services: AuthService, UserService, ProductService, CartService, OrderService (ACID), ReviewService, ChatService (Rate Limit + Cache)"]
    AI["AI Strategy & Factory: ChatProviderFactory -> GeminiChatProvider (gemini-3.1-flash-lite) / MockChatProvider"]
    DAOs["DAO Layer (DaoFactory): UserDAO, ProductDAO, CartDAO, OrderDAO, OrderItemDAO, ReviewDAO (PreparedStatement JDBC)"]
    DB[("HikariCP Pool -> H2 (Local/Test) / PostgreSQL (Render)")]

    Client --> Filters --> Servlets --> Services
    Services --> AI
    Services --> DAOs --> DB
```

---

## 4. Environment Variables & Database Configuration

Copy `.env.example` to `.env` or `src/main/resources/config.properties.example` to `src/main/resources/config.properties` (both `.env` and `config.properties` are gitignored and `.dockerignore`d):

| Variable / Property | Default | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | HTTP server port bound to `0.0.0.0` in Docker/Render |
| `DATABASE_URL` | *(unset locally)* | Render PostgreSQL URI (`postgres://user:pass@host:port/dbname` or `postgresql://...`), automatically parsed by `DatabaseConfigResolver` |
| `JDBC_URL` (`db.url`) | `jdbc:h2:mem:mohanmart;DB_CLOSE_DELAY=-1;MODE=PostgreSQL` | Explicit JDBC URL (overrides `DATABASE_URL` if set). Supports `jdbc:h2:tcp://localhost:9092/./data/mohanmart` for local file/TCP H2 |
| `DB_USERNAME` / `JDBC_USER` | `sa` (H2) / parsed from `DATABASE_URL` | Database username |
| `DB_PASSWORD` / `JDBC_PASSWORD` | *(empty)* / parsed from `DATABASE_URL` | Database password |
| `SEED_DATABASE` | `true` | Enables automatic, idempotent startup seeding via `DatabaseSeeder` |
| `H2_CONSOLE_ENABLED` | `true` (local) / `false` (Docker) | Starts local H2 Web Console (`8082`) and TCP Server (`9092`) when using H2 |
| `ADMIN_EMAIL` | `admin@mohanmart.com` | Email address for the initial seeded administrator account |
| `ADMIN_INITIAL_PASSWORD` / `ADMIN_PASSWORD` | *(auto-generated in `render.yaml`)* | Initial administrator password; hashed with BCrypt (`cost=12`) at startup |
| `AI_CHATBOT_PROVIDER` (`ai.chatbot.provider`) | `mock` | `mock` (offline deterministic FAQ + live catalog search) or `gemini` |
| `GEMINI_API_KEY` | *(unset)* | Google Gemini API key (server-side only; falls back to `mock` if unset) |
| `GEMINI_MODEL` (`ai.chatbot.model`) | `gemini-3.1-flash-lite` | Gemini model identifier (`gemini-2.5-*` is blocked) |

---

## 5. Database Migrations & Idempotent Startup Seeding

When `AppContextListener` starts:
1. **`DatabaseConfigResolver`** resolves the active database profile (`PostgreSQL (Cloud/Render)` or `H2 (Local/Embedded)`), converts Render's `postgres://` or `postgresql://` connection string into a clean `jdbc:postgresql://host:port/database` URL, extracts credentials, and masks secrets in startup logs.
2. **`DatabaseMigrationRunner`** applies versioned SQL scripts (`V1__init_schema.sql` through `V4__add_order_status_check.sql`) tracked in the `schema_migrations` table using non-destructive `CREATE TABLE IF NOT EXISTS` and `CREATE INDEX IF NOT EXISTS` statements.
3. **`DatabaseSeeder`** executes an idempotent seed check inside a transaction:
   - **Admin Security**: If `ADMIN_INITIAL_PASSWORD` (or `ADMIN_PASSWORD`) is set, hashes it with BCrypt (cost 12) and configures the admin account. In production mode (`PostgreSQL`), if `ADMIN_INITIAL_PASSWORD` is omitted, the admin account is locked with a random cryptographic hash so no publicly known default password is ever active in production.
   - **Rich Marketplace Catalog**: Idempotently seeds 16 curated products across 5 categories (`Electronics`, `Books`, `Home`, `Clothing`, `Accessories`), demo sellers (`Mohan Electronics`, `BookWorld Store`, `Kaveri Living & Apparel`), demo buyers, 3 sample orders (`DELIVERED`, `SHIPPED`, `PENDING`), order line items, and 4 verified product reviews.
   - **Safe Across Restarts**: Every user is checked by `email`, every product by `name`, and every review by `(user_id, product_id)`. Restarting the container on Render never duplicates records or causes constraint failures.

---

## 6. Local Setup & Docker Instructions

### Local Setup (Maven + H2)
```bash
# 1. Clone repository
git clone https://github.com/mohanraj2707/MohanMart.git
cd MohanMart

# 2. Configure local properties (optional - defaults to embedded H2; or use jdbc:h2:tcp://localhost:9092/./data/mohanmart)
cp src/main/resources/config.properties.example src/main/resources/config.properties

# 3. Run full build, migrations, seeding tests, and all 188 automated JUnit 5 tests
mvn -B clean verify

# 4. Run static analysis (Checkstyle & SpotBugs)
mvn -B checkstyle:check spotbugs:check
```

### Build and Run with Docker Locally
```bash
# 1. Build the production multi-stage Docker image
docker build -t mohanmart:latest .

# 2. Run container locally (with embedded H2 in PostgreSQL compatibility mode + auto-seeding)
docker run --rm -p 8080:8080 \
  -e PORT=8080 \
  -e SEED_DATABASE=true \
  -e ADMIN_INITIAL_PASSWORD=ChangeMeLocalAdmin#2026 \
  mohanmart:latest

# 3. Verify health endpoint and browse marketplace
curl http://localhost:8080/api/v1/health
# Open http://localhost:8080 in your browser
```

---

## 7. Deploying to Render (Docker + Managed PostgreSQL)

### Option A: One-Click Render Blueprint (`render.yaml` — Recommended)
1. Push this repository to GitHub.
2. In the [Render Dashboard](https://dashboard.render.com/), click **New +** → **Blueprint**.
3. Connect the `MohanMart` GitHub repository. Render will automatically detect `render.yaml` and provision:
   - **`mohanmart-db`**: Managed Render PostgreSQL database (`free` plan).
   - **`mohanmart`**: Docker Web Service linked to `mohanmart-db` via `DATABASE_URL`, `DB_USERNAME`, and `DB_PASSWORD`, with health check path `/api/v1/health` and an auto-generated `ADMIN_INITIAL_PASSWORD`.
4. *(Optional)* In the `mohanmart` Web Service **Environment** tab on Render:
   - View or customize `ADMIN_INITIAL_PASSWORD` to sign in as `admin@mohanmart.com`.
   - Set `AI_CHATBOT_PROVIDER=gemini` and add your `GEMINI_API_KEY` if you want live Gemini responses (defaults to `mock` provider with zero external API dependencies).

### Option B: Manual Render Web Service + PostgreSQL Setup
1. In Render Dashboard, click **New +** → **PostgreSQL**:
   - Name: `mohanmart-db`, Database: `mohanmart`, Plan: `Free`.
   - Copy the **Internal Database URL** (`postgresql://user:pass@dpg-.../mohanmart`).
2. Click **New +** → **Web Service**:
   - Connect your GitHub repository.
   - Runtime: **Docker** (`./Dockerfile`).
   - Health Check Path: `/api/v1/health`.
   - Environment Variables:
     - `PORT` = `8080`
     - `DATABASE_URL` = *(paste Internal Database URL from step 1)*
     - `SEED_DATABASE` = `true`
     - `H2_CONSOLE_ENABLED` = `false`
     - `ADMIN_INITIAL_PASSWORD` = *(your strong admin password)*
     - `AI_CHATBOT_PROVIDER` = `mock` (or `gemini` with `GEMINI_API_KEY`)

### How to Re-Deploy or Reset / Re-Seed Safely
- **Standard Redeploy**: Trigger **Manual Deploy → Deploy latest commit** in Render. `DatabaseMigrationRunner` and `DatabaseSeeder` will run idempotently, preserving all existing users, products, and orders while ensuring any missing seed items exist.
- **Updating Admin Password**: Change `ADMIN_INITIAL_PASSWORD` in Render's **Environment** tab and save. On restart, `DatabaseSeeder` hashes and updates the admin password automatically.
- **Full Reset / Re-Seed**: If you want a fresh database state, run `DROP TABLE reviews, cart_items, order_items, orders, products, users, schema_migrations CASCADE;` via Render's PSQL shell and restart the web service; `V1`–`V4` and `DatabaseSeeder` will recreate and re-seed all tables automatically.

### Troubleshooting Common Render Deployment Issues
- **`org.apache.catalina.core.StandardServer: shutdown command [HEAD / HTTP/1.1] received`**: By default, Tomcat opens a TCP shutdown listener on port `8005` alongside the HTTP connector on `8080`. When Render's port scanner or health prober sends `HEAD / HTTP/1.1` to port `8005` (or if Render auto-detected port `8005` instead of `8080`), `StandardServer.await()` logs this warning and drops the connection. The root `Dockerfile` disables port `8005` via `<Server port="-1" shutdown="SHUTDOWN">` in `server.xml` and binds the HTTP connector to `0.0.0.0:${PORT:-8080}`, ensuring only the HTTP port is open and eliminating this warning.
- **Health check timeout on initial deploy**: Render's Free Tier builds the multi-stage Maven + Tomcat image and runs migrations on startup. The `/api/v1/health` endpoint returns `200 OK` as soon as Tomcat finishes initializing `AppContextListener`.
- **Cold starts on Render Free Tier**: Free Render web services spin down after 15 minutes of inactivity and take ~30–50 seconds to wake up on the next request.
- **`DATABASE_URL` connection errors**: Ensure you use Render's **Internal Database URL** when both the Web Service and PostgreSQL instance are in the same Render region. `DatabaseConfigResolver` automatically converts `postgres://` and `postgresql://` URLs (with or without explicit `:5432` port) into `jdbc:postgresql://` format.

---

## 8. Demo Accounts (Seeded for Development)

Passwords are hashed with BCrypt (cost 12):

| Role | Email | Password Source | Capabilities |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin@mohanmart.com` | `ADMIN_INITIAL_PASSWORD` env var (or local dev seed) | Platform metrics, user governance, catalog & order moderation |
| **SELLER** | `seller1@mohanmart.com` | `DEMO_SELLER_PASSWORD` env var (or local dev seed) | Seller Hub KPIs, product CRUD, order fulfillment (`CONFIRMED → SHIPPED → DELIVERED`) |
| **BUYER** | `buyer1@mohanmart.com` | `DEMO_BUYER_PASSWORD` env var (or local dev seed) | Browse/search, cart, atomic checkout, order tracking, verified reviews, AI chat |

---

## 9. Screenshots & Documentation Links

### Screenshots (`docs/screenshots/`)
- `docs/screenshots/01-marketplace-home.png` — Landing page & curated categories *(placeholder)*
- `docs/screenshots/02-catalog-search-filters.png` — Faceted catalog search & sorting *(placeholder)*
- `docs/screenshots/03-cart-and-escrow-checkout.png` — Shopping cart & simulated escrow checkout *(placeholder)*
- `docs/screenshots/04-seller-dashboard-and-orders.png` — Seller Operations Hub & dispatch workflow *(placeholder)*
- `docs/screenshots/05-admin-console.png` — Admin governance & platform GMV console *(placeholder)*
- `docs/screenshots/06-ai-chatbot-widget.png` — Floating AI Marketplace Assistant widget *(placeholder)*

### Project Documentation
- **[Final Capstone Report](docs/FINAL_REPORT.md)** — Architecture, schema, technical decisions, design patterns, and known limitations
- **[Final Regression & Viva Checklist](docs/FINAL_REGRESSION.md)** — End-to-end manual test sheet (localhost + `<LIVE_URL>`) & demo video checklist
- **[Demo Script (2–3 Min Viva Walkthrough)](docs/DEMO_SCRIPT.md)** — Step-by-step rehearsed live demo script
- **[Security & Testing Verification Report](docs/testing.md)** — Section 9 security audit (`PreparedStatement`, BCrypt, CSRF, XSS, RBAC, SQLi)
- **[Changelog](CHANGELOG.md)** · **[Sprint Retrospectives](RETRO.md)** · **[Contributing Guide](CONTRIBUTING.md)**