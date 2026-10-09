# MohanMart — Final Regression Sheet & Deployment Checklist

Use this regression sheet to verify all functional, security, and operational requirements on **localhost (`http://localhost:8080/mohanmart`)** and the **Live URL (`<LIVE_URL>`)** prior to the Final Review.

---

## 1. End-to-End Manual Regression Test Sheet

| ID | Feature / Spec Ref | Test Steps | Expected Result | Localhost | `<LIVE_URL>` |
| :--- | :--- | :--- | :--- | :---: | :---: |
| **R-01** | **Health Check (§18)** | `GET /api/v1/health` | HTTP `200 OK` with `{"status":"UP","db":"UP"}` and `X-Request-Id` header | [x] PASS | [ ] |
| **R-02** | **User Registration (F1)** | Register new `BUYER` on `/register` with valid name, email, password | Account created (BCrypt cost 12), redirected to `/login` | [x] PASS | [ ] |
| **R-03** | **Login & Session Rotation (F1)** | Sign in on `/login` as `buyer1@mohanmart.com` | `JSESSIONID` rotated (`HttpOnly`), redirected to catalog, 30-min timeout active | [x] PASS | [ ] |
| **R-04** | **Catalog Browse, Search & Filter (F3)** | Visit `/products`, search `"Keyboard"`, filter by `Electronics`, sort by price | Matching active products rendered with `<c:out>` escaping and pagination | [x] PASS | [ ] |
| **R-05** | **Server-Side Cart (F4)** | Add product to cart, update quantity in `/cart`, test quantity > stock | Authoritative subtotal calculated; excessive quantity rejected with HTTP `400` | [x] PASS | [ ] |
| **R-06** | **Atomic Checkout (F5)** | Submit `/checkout` form with delivery address & CSRF token | Order created in `CONFIRMED` status, `stock_qty` decremented, cart cleared atomically | [x] PASS | [ ] |
| **R-07** | **Seller Product CRUD (F2)** | Log in as `seller1@mohanmart.com`, create/edit/delete listing on `/seller/products` | Listing saved/updated/soft-deleted; non-owner edits rejected with HTTP `403` | [x] PASS | [ ] |
| **R-08** | **Order Status Workflow (O2)** | Seller advances order `CONFIRMED → SHIPPED → DELIVERED` on `/seller/orders`; attempt backward `DELIVERED → SHIPPED` | Forward transitions succeed (`200`/`302`); invalid backward transition rejected with HTTP `409 Conflict` | [x] PASS | [ ] |
| **R-09** | **Verified Buyer Review (F8)** | Buyer submits 5-star review on a `DELIVERED` product; attempt second review or unpurchased product review | First review saved and average updated; duplicate (`400`) and unpurchased (`403`) rejected | [x] PASS | [ ] |
| **R-10** | **Admin Moderation Console (F7)** | Log in as `admin@mohanmart.com`, inspect `/admin/dashboard`, `/admin/users`, `/admin/products`, `/admin/orders` | Platform GMV, user governance, product moderation, and order status override work | [x] PASS | [ ] |
| **R-11** | **AI Chatbot — Normal & FAQ (O4)** | Click floating `"Ask MohanMart AI"` button, ask `"How does mock payment work?"` and `"What categories are available?"` | Returns JSON envelope `{"success":true,"data":{"reply":"..."}}` rendered via `textContent` | [x] PASS | [ ] |
| **R-12** | **AI Chatbot — Rate Limit & Failover (O4)** | Send >10 chat messages in 1 minute; test with invalid/empty `GEMINI_API_KEY` | 11th message returns HTTP `429`; missing/failed Gemini key seamlessly falls back to `MockChatProvider` | [x] PASS | [ ] |
| **R-13** | **Security Guards (§9)** | Test `<script>alert(1)</script>` in search/review, `' OR 1=1 --` in login, `/admin/dashboard` as `BUYER` | XSS escaped as plain text, SQLi safely parameterized, RBAC returns `403` | [x] PASS | [ ] |

---

## 2. Backup Demo Video Recording Checklist (2–3 Minutes)

- [ ] **Resolution & Audio**: Set screen recorder (OBS / Windows Game Bar `Win+G`) to `1920x1080` at 125% browser zoom so text and badges are crisp; verify microphone audio levels.
- [ ] **Clean Browser State**: Open two browser windows (Window 1: Buyer + AI Chatbot; Window 2: Incognito for Seller & Admin) and one terminal showing `mvn -B clean verify` green output.
- [ ] **No Secrets Visible**: Ensure `.env`, `GEMINI_API_KEY`, and personal tokens are not open in any editor tab.
- [ ] **Record Continuous 2:30 Take**: Follow `docs/DEMO_SCRIPT.md` covering Buyer browse/cart/checkout -> Seller fulfillment (`SHIPPED` -> `DELIVERED`) -> Buyer verified review -> Admin console -> AI Chatbot widget + `/api/v1/health`.
- [ ] **Save Locally & Cloud**: Save as `MohanMart_Capstone_Demo.mp4` on desktop and upload backup copy to Google Drive.
