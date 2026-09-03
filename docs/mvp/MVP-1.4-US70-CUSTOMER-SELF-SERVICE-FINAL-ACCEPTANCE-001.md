# MVP-1.4 US-70 Customer Self-Service — Final Acceptance

Date: 2026-09-03

Decision: `PASS — US-70 COMPLETE`

Milestone: `MVP 1.4 — 8/8 COMPLETE — CLOSED`

## Independent decision

Independent source, security, privacy, persistence, architecture, frontend, and real-browser review found no acceptance blocker. US-70 implements the frozen per-Delivery possession-authorization model without creating customer accounts, operator permissions, direct scheduling, slot booking, or a new bounded context.

## Access and token security

- The credential is an opaque 256-bit value generated from 32 `SecureRandom` bytes and encoded as unpadded Base64URL. It contains no claims.
- Persistence contains only the SHA-256 hash. The raw token exists only in the returned fragment URL and transient final provider payload.
- The issuance contact is stored only as HMAC-SHA-256 using an externally configured secret and key version; the self-service access table contains no raw contact.
- Each access record binds Tenant, Delivery Order, Organization Customer, contact fingerprint, allow-listed actions, 30-day expiry, revocation facts, and issuance idempotency.
- A locked Delivery Order serializes issuance. Same-attempt re-entry rotates the same row; later attempts remain bounded to five active tokens, with the oldest revoked before a sixth is stored.
- Expiry uses server time. Tokens remain multi-use until expiry, revocation, or binding failure.

## Authorization, Tenant isolation, and privacy

- Public APIs accept only `Authorization: DeliveryAccess <token>`. Tenant, Customer, Delivery, and recipient authority are derived from the persisted hash match; query/body identifier injection is non-authoritative and strict write DTOs reject extra fields.
- Unknown, expired, revoked, inactive-customer, wrong-customer, wrong-delivery, cross-Tenant, contact-mismatch, and action-denied access fail closed as `404 SELF_SERVICE_ACCESS_INVALID`.
- Possession authorization grants no `DELIVERY_*`, `NOTIFICATION_*`, Identity role, tenant membership, or operator session.
- The customer projection contains the delivery number, friendly status/explanation, scheduled window and Tenant time zone, US-67 ETA/freshness, available actions, destination display name, POD availability/completion time, masked Email/SMS preferences, and safe submission references.
- It excludes internal IDs/enums, full address, Rider data/location, batch/zone/slot capacity, ETA provider/cache/heuristic details, exception investigation, notification bodies/provider facts, and POD evidence.

## Customer workflows and owned boundaries

- Notification preferences read/write through Notification's published `CustomerOperationalPreferenceManagement` root contract. Stale versions map to `409 SELF_SERVICE_PREFERENCE_VERSION_CONFLICT` without a partial update.
- Issues use the frozen category allow-list, trimmed plain text of 10–1,000 characters, and `SUBMITTED` status.
- Feedback requires `DELIVERED`, rating 1–5, a maximum 1,000-character comment, idempotent replay, one active record per Tenant/Delivery/Customer, and terminal `RECORDED` status.
- Delivery preference/redelivery actions create only Delivery-owned customer submissions. They do not mutate Delivery state/window, create a US-60 redelivery schedule, reserve a US-64 slot, or consume capacity.
- ETA is read through the accepted US-67 projection. US-69's five accepted event contracts are unchanged.

## Notification link integration

- V59 adds only a controlled `[[SELF_SERVICE_LINK]]` placeholder to eligible Email/SMS templates.
- Notification invokes the published final-send issuance seam only after claiming the provider attempt, substitutes the raw URL in memory, and persists no raw token in Notification, execution, attempt, event, or audit state.
- Same-attempt retry uses the stable attempt idempotency key and rotates the existing access row; later attempts create independently bounded credentials.

## Public frontend and transport controls

- `/track` is outside `ProtectedRoute` and `AppLayout`; no sidebar, breadcrumbs, notifications center, admin menu, RBAC controls, or internal IDs are rendered.
- `#access_token` is consumed once into React memory and removed immediately with `history.replaceState`. It is not written to localStorage, sessionStorage, IndexedDB, cookies, service-worker state, or persistent TanStack cache.
- Reload loses access; reopening the original valid fragment link restores access.
- The dedicated public Axios client sends `DeliveryAccess` only to self-service routes. Responses use `Cache-Control: no-store` and `Referrer-Policy: no-referrer`; CORS is restricted to the configured customer origin.
- Rate limits enforce 120 reads/15 minutes and 10 writes/15 minutes per valid token, 20 invalid attempts/15 minutes per source IP, and 20 customer writes/hour per Delivery. In-memory key storage is bounded to 10,000 entries and fails closed.

## Persistence and Flyway

- `V59__customer_self_service_us70.sql` creates Delivery-owned `delivery_self_service_access` and `delivery_customer_submission`, including Tenant-leading indexes, hash/idempotency uniqueness, feedback uniqueness, typed checks, optimistic versions, and same-Tenant Delivery/access foreign keys.
- Organization Customer remains a logical cross-module UUID; no cross-module physical foreign key was added.
- Only V59 changed for US-70; V1–V58 remain unchanged. A clean V1→V59 migration passed and V59 is the current head.
- All destructive PostgreSQL acceptance used `transport_logistics_acceptance`. The development database was not used.

## Independent verification evidence

| Gate | Actual result |
| :--- | :--- |
| Focused backend/security | 28 tests, 0 failures, 0 errors, 0 skipped — PASS (33.613 s) |
| PostgreSQL subset | 4/4 PASS; clean V1→V59 on `transport_logistics_acceptance` |
| Full Maven `verify` | 1,238 tests, 0 failures, 0 errors, 15 skipped — BUILD SUCCESS (4:56) |
| Architecture / Modulith | 42/42 PASS |
| Checkstyle | 0 error-severity violations; configured gate PASS |
| PMD | 0 violations — PASS |
| SpotBugs | 0 bugs — PASS |
| TypeScript | PASS |
| Vitest | 59 files, 259/259 tests PASS |
| Production build | PASS |
| Changed-file ESLint | 0 errors |
| Global ESLint | 71 pre-existing errors in eight unchanged Delivery screens; US-70/E2E introduced errors: 0 |
| Real Chromium | 9/9 PASS against real React, REST, DeliveryAccess security, Delivery, Notification, Organization, PostgreSQL, and Flyway V59 (23.5 s) |
| `git diff --check` | PASS |

The global ESLint debt is confined to unchanged Delivery analytics, batches, exceptions, riders, slots, and zones screens. It is unrelated to US-70 and does not alter this acceptance decision.

## Scope containment

No customer account or `app_user` mapping, OTP/MFA, IN_APP/push, direct scheduling, slot booking, cancellation, address/payment mutation, Rider data/location, POD evidence, notification history, offline mode, native app, or new bounded context was introduced.

## Final program state

- US-70: `COMPLETE`.
- MVP 1.4 Last-Mile Delivery: `8/8 COMPLETE — CLOSED`.
- Overall: `65/87 COMPLETE`.
- Deferred: `22/87`.
- Arithmetic: `65 + 22 = 87`.
- Authoritative next queue item: `P1-01` — modernize only legacy event contracts that acquire real consumers and approve an outbox/inbox boundary before claiming durable external delivery.

## Governance synchronization

- Central knowledge base closure commit: `7014395dc7b1bfde43218f175e92aa703d908eb5` on `main`.
- Remote synchronization: `BLOCKED_GOVERNANCE_SYNC_AUTHENTICATION`.
- Remote: `https://github.com/Hasitha3513/central-knowledge-base.git`; local `main` is one commit ahead of `origin/main` (`0 behind / 1 ahead`).
- Failed command: `git -C ../central-knowledge-base push origin main` with `fatal: could not read Username for 'https://github.com': No such device or address`.
- Credential diagnostics: no configured credential helper and `gh` is not installed. Minimum action is to provide a secure authenticated GitHub HTTPS credential mechanism, then push the existing commit; acceptance and story accounting remain valid.
