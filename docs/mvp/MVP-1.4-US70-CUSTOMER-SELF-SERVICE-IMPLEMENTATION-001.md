# MVP-1.4 US-70 Customer Self-Service — Implementation Evidence

Date: 2026-09-03  
Status: `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`

## Implemented contract

- Delivery issues 256-bit CSPRNG opaque tokens in an HTTPS `/track#access_token=...` fragment. Only the SHA-256 token hash is persisted.
- The access record binds server-derived Tenant, Delivery Order, Organization Customer, HMAC-SHA-256 contact fingerprint and key version, action scope, 30-day expiry, revocation/use facts, issuance idempotency key, and optimistic version.
- Issuance is serialized by a locked Delivery Order, rotates a repeated notification-attempt issuance, and revokes the oldest token before exceeding five active tokens per Delivery/Customer.
- Public requests use only `Authorization: DeliveryAccess <token>`. Invalid, expired, revoked, contact-mismatched, action-denied, and unknown tokens return the same safe 404 contract.
- The public projection exposes only the delivery number, customer-safe status/explanation, destination display name, scheduled window/time zone, ETA and freshness, POD availability/completion state, available actions, preferences, and customer-safe submission references. It excludes exact address, Rider identity/location, POD evidence, notification history/body, internal IDs, and operator UI.
- Notification substitutes `[[SELF_SERVICE_LINK]]` immediately before provider send through the published `FinalSendCustomerLinkIssuer`/`CustomerSelfServiceLinkIssuer` boundary. Raw tokens are absent from persisted Notification, execution, event, audit, and delivery-attempt records.

## API and behavior

Delivery owns these public endpoints beneath the deployed `/api` context:

- `GET /api/public/v1/delivery-self-service`
- `GET /api/public/v1/delivery-self-service/notification-preferences`
- `PUT /api/public/v1/delivery-self-service/notification-preferences`
- `POST /api/public/v1/delivery-self-service/issues`
- `POST /api/public/v1/delivery-self-service/feedback`
- `POST /api/public/v1/delivery-self-service/redelivery-requests`

Notification remains the Email/SMS preference authority through a published root contract. Issues, feedback, delivery preferences, and eligible redelivery requests are Delivery-owned customer submissions with idempotency and safe references.

Customer requests are non-binding: they do not alter Delivery status/window, create or supersede a US-60 redelivery schedule, reserve a US-64 slot, or decrement capacity. ETA is read through the existing US-67 use case. Existing US-69 delivery events are unchanged.

## Persistence

Migration `V59__customer_self_service_us70.sql` creates:

- `delivery_self_service_access` — Tenant-scoped hash-only access credential and lifecycle facts, global token-hash uniqueness, Tenant-local issuance idempotency, same-module `(delivery_order_id, tenant_id)` foreign key, creator/updater audit fields, active lookup indexes, and action constraints.
- `delivery_customer_submission` — Tenant-scoped typed customer submissions with composite Tenant-consistent access association, Delivery/Customer references, request hash and idempotency, `SMALLINT` feedback rating, feedback uniqueness, creator/updater audit fields, status, and optimistic version.

The migration also adds the controlled `[[SELF_SERVICE_LINK]]` placeholder to existing US-69 Email/SMS templates. V59 is the current Flyway head; V58 remains US-69's migration. Organization Customer IDs remain logical cross-module references with no physical foreign key.

## Frontend and security

- `/track` is a public shell outside `ProtectedRoute` and `AppLayout`.
- The fragment is consumed into React memory and immediately removed with `history.replaceState`; no cookie, local storage, session storage, IndexedDB, query parameter, or service-worker persistence is used.
- Reload loses access. Reopening the original fragment link, including a same-page fragment navigation, re-consumes it while valid.
- A dedicated public Axios client and feature-owned TanStack Query hook send `DeliveryAccess` only to the self-service API. React Hook Form, Zod, and controlled Ant Design fields preserve nullable initial preference versions and validate public writes.
- Backend responses enforce `Cache-Control: no-store` and `Referrer-Policy: no-referrer`; CORS is restricted to the configured customer origin and required methods/headers.
- Read/write/invalid-attempt throttles have bounded per-key queues and a 10,000-key upper bound with stale pruning. Strict request DTOs reject unknown fields and prevent Tenant/Delivery/Customer mass assignment.

## Verification evidence

- Full Maven: `./mvnw verify` against `transport_logistics_acceptance` — **1,238 tests, 0 failures, 0 errors, 15 skipped; BUILD SUCCESS; 4:19**.
- Complete focused US-70 backend/security group — **28/28 PASS**; the PostgreSQL subset is **4/4 PASS** with clean Flyway V1→V59, V59 constraints/indexes, table ownership, idempotency, and Tenant-consistent access foreign key.
- Self-service hard-gate unit suite — **10/10 PASS**, including entropy/hash/HMAC/expiry, active cap/revocation, action scope, safe projection/contact mismatch, preference throttling, concurrent invalidation, feedback, idempotency, and non-binding request boundaries.
- Architecture suite — **42/42 PASS**, including Spring Modulith/hexagonal/P0-01 through P0-07 boundaries. The 28-test focused group also covers transient Notification final-send links and literal external `/api/public/...` security routes.
- Checkstyle — **0 violations**; PMD — **PASS**; SpotBugs — **0 bugs**.
- Frontend TypeScript — **PASS**; Vitest — **59 files, 259 tests PASS**; production build — **PASS**; changed-file ESLint — **0 errors**.
- Global ESLint debt — **71 pre-existing errors in unrelated Delivery screens**; no US-70 or E2E introduced error.
- Real PostgreSQL-backed Chromium — **9/9 PASS** in one complete serial run, covering link/header/fragment/privacy, Email/SMS preferences, issue persistence, non-binding request behavior, reload/reopen, Tenant/Customer mass-assignment denial, post-delivery feedback, identical expired/revoked/customer-mismatch denial, safe invalid access, and operator-shell isolation.
- `git diff --check` — **PASS**.
- Development database used — **NO**.

## Deferred and excluded scope

No customer account or `app_user` relationship, OTP, IN_APP/push/WhatsApp, direct scheduling, slot booking, cancellation, address/payment mutation, Rider data/location, POD evidence, notification history, offline mode, native app, or new bounded context was introduced.

## Program state

US-70 is ready for independent final acceptance, not complete. MVP accounting remains 7/8 for MVP 1.4, 64/87 overall, and 23/87 deferred.

Next task: `MVP-1.4-US70-CUSTOMER-SELF-SERVICE-FINAL-ACCEPTANCE-001`.
