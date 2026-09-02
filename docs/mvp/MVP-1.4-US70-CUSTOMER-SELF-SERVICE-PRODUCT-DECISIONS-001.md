# US-70 Customer Self-Service — Frozen Product Decisions

**Status:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`

**Authority:** Original US-70 requirement, the complete US-61–US-70 use-case/activity/sequence diagrams, the complete requirements mind map, accepted US-56–US-69 behavior, and the current V58 source/schema.

**Primary actor:** Customer / Recipient.

**Decision date:** 2026-09-03.

## Authoritative Requirement and Acceptance Boundary

US-70 requires a Customer/Recipient to use shipment tracking, manage delivery preferences, submit a customer issue, and provide feedback so that they can manage their service experience. The authoritative activity flow requires customer access validation before every action, an authorized Delivery projection with current status and ETA, persisted preference/issue/feedback submissions, and an optional re-delivery or preference-change request routed to the existing scheduling workflow. Acceptance requires authorized tracking, preference and issue submission, and feedback linked to the appropriate Delivery Order.

The portal is a controlled doorway to existing Delivery capabilities, not another logistics system. US-70 does not create a second scheduling engine, ETA engine, Notification engine, POD store, exception investigation model, customer master, or authentication system.

## Source and Repository Findings

- The requirements class model conceptually recommends `User <|-- Customer`, but current production source does not implement that relationship.
- Identity owns `app_user`, credentials, refresh tokens, tenant memberships, and operator RBAC. `tenant_membership.user_id` has no Customer/Recipient association.
- Organization owns the tenant-scoped Customer master and contact projection. Delivery stores only logical `customer_id`; there is no Recipient aggregate or canonical Recipient-to-user link.
- US-69 consequently deferred Customer IN_APP/push. That factual baseline remains authoritative.
- Delivery owns order status/window, US-59 failed attempts, US-60 redelivery scheduling, US-64 slots, US-67 ETA, and US-57 POD.
- Notification owns Email/SMS operational preferences and message delivery. Organization supplies customer contact facts through its public tenant-aware contract.

An authenticated customer account or organization-portal-user model would therefore require an unapproved Customer/Recipient-to-Identity association and account-provisioning policy. It is not selected for this MVP.

## Frozen Customer Access Model

The one and only US-70 MVP access model is an **opaque Delivery-access token delivered as a magic link**. Possession of a valid token is the customer principal for exactly one Delivery Order. Identity JWT/session authentication is not mixed into this flow.

- The token is not an operator JWT, tenant membership, role, permission, password, OTP, or universal customer credential.
- It authorizes only the specific Tenant + Delivery Order + Customer context recorded by the server and only the allow-listed US-70 actions.
- Delivery owns issuance, validation, revocation, audit, and persistence through a thin customer-facing inbound adapter inside the Delivery module.
- The HTTP route is explicitly public at the authentication layer, but every request remains fail-closed at the Delivery access-token authorization layer.
- Customer registration, password reset, MFA, customer JWT refresh, logout, and account lockout are not part of US-70.

## Token Contract

### Generation, representation, and hashing

- Generate 32 cryptographically random bytes with a CSPRNG (256 bits of entropy).
- Encode the client token as unpadded Base64URL. It has no meaningful or client-editable claims.
- Persist only `SHA-256(token bytes)` as lowercase 64-character hexadecimal. The 256-bit random source makes offline guessing infeasible; no reversible token encryption is required.
- Raw tokens may exist only during issuance, transient Notification channel rendering, and customer possession. They must never be stored in application database rows, outbox/event metadata, audit records, exception text, analytics, browser persistent storage, or logs.
- Token comparison uses a constant-time comparison after hashing.

### Scope and lifetime

- Every token row binds immutable `tenant_id`, `delivery_order_id`, `customer_id`, an `HMAC-SHA-256` fingerprint of the normalized recipient destination used for issuance, and an immutable action set. The HMAC uses a dedicated externally configured secret and key version so low-entropy phone/email values cannot be recovered with an offline dictionary from a plain hash.
- MVP action set: `TRACK`, `PREFERENCE_READ`, `PREFERENCE_WRITE`, `ISSUE_SUBMIT`, `FEEDBACK_SUBMIT`, and `REDELIVERY_REQUEST_SUBMIT`.
- Token lifetime is 30 days from issuance. No permanent link exists.
- Tokens are multi-use until expiry or revocation because tracking and preference management are recurring activities. Action-level lifecycle checks still apply on every use.
- At most five unexpired, non-revoked tokens may exist for one Tenant/Delivery/Customer. A sixth issuance revokes the oldest active token in the same transaction.
- The Delivery link-issuance port accepts the stable Notification delivery-attempt idempotency key. The first call creates one access record and transient token; a repeated call for that same attempt atomically replaces the stored hash in that record and invalidates the previously issued raw token before returning a new transient token. A later independent Notification attempt may create another active access record.

### Revocation and rotation

Revoke all active tokens for the bound Delivery/Customer when:

- the Delivery's Customer association changes;
- Organization reports the Customer inactive or the normalized issuance contact changes;
- a security incident requests revocation;
- a replacement/rotation is explicitly requested by an authorized operator; or
- the access records reach the normal retention/purge boundary.

Expiry is enforced by server time on every request. Delivery completion does not revoke immediately because post-delivery status, issue reporting, feedback, and operational notification preferences remain valid until the token's existing 30-day expiry. Completion removes only scheduling/pre-delivery preference-request actions. Rotation revokes the selected active token(s) and creates a fresh token; it never recovers an old raw token.

Manual operator issue/revoke/regenerate endpoints and UI are deferred because automated link issuance through the Notification integration is sufficient for the source-required customer journey. Delivery still owns internal revocation/rotation use cases for lifecycle/contact changes and security response; no new operator or customer-facing RBAC permission is created.

## Link Transport and Browser Handling

- Public page: `https://<configured-customer-origin>/track#access_token=<opaque-token>`.
- HTTPS is mandatory outside local development. The origin is fixed configuration; user-controlled redirect targets are prohibited.
- The token is placed in the URL fragment, never query or path. Browsers do not send fragments to the server or in HTTP referrers.
- The React route reads the fragment once, stores the token in memory only, and immediately removes it with `history.replaceState` before API calls.
- No token is stored in `localStorage`, `sessionStorage`, IndexedDB, cookies, TanStack persistence, telemetry, error reporting, or service-worker cache. Reload after fragment removal requires reopening the original link.
- API requests use `Authorization: DeliveryAccess <opaque-token>`. The token is never accepted in JSON, query parameters, path parameters, `X-Tenant-ID`, or a customer/delivery ID supplied by the browser.
- Responses set `Cache-Control: no-store` and `Referrer-Policy: no-referrer`; the public page permits no third-party scripts/resources. Logs and traces redact the authorization header and fragment.
- This header credential is not cookie-based, so CSRF tokens are not required. CORS is restricted to the configured customer origin. XSS prevention remains critical because the token lives in memory.

## Tenant, Customer, IDOR, and Authorization Model

- Tenant authority comes only from the persisted token row after successful token lookup. The frontend never sends authoritative Tenant, Customer, Delivery, or Recipient IDs.
- After resolving the token hash, the application establishes a bounded server-side Tenant execution context and performs tenant-qualified lookups for the bound Delivery and Customer.
- The Delivery must exist in the same Tenant and still reference the token-bound Customer. Organization's public `CustomerNotificationContactLookup` (or an equivalently narrow public US-70 projection) must confirm the same-Tenant Customer is active.
- Any mismatch fails closed and may revoke the token. No Customer/Recipient information is inferred from email/phone equality across tenants.
- Invalid, unknown, expired, revoked, wrong-contact, cross-Tenant, or wrong-Delivery access returns the same customer-safe `404 SELF_SERVICE_ACCESS_INVALID`. The response must not reveal which fact failed.
- No API accepts `deliveryId`, `customerId`, or `tenantId` as authority. Guessing another UUID cannot expand the token scope.
- Operator RBAC and customer possession authorization are separate. Customers receive no `DELIVERY_*`, `NOTIFICATION_*`, or Identity permissions.

## Customer-Safe Read Projection

`GET /api/public/v1/delivery-self-service` returns only:

- immutable public `deliveryNumber`;
- customer-friendly status label and explanation;
- scheduled delivery window in the Tenant timezone with unambiguous offset;
- current US-67 estimated arrival time, calculated-at time, and customer-safe freshness (`CURRENT`, `STALE`, or `UNAVAILABLE`);
- available customer actions computed by the server;
- masked destination summary: locality/name plus only the last non-sensitive address line when available, never coordinates or full Organization record;
- POD availability (`NOT_AVAILABLE` or `AVAILABLE`) and delivery completion time where known;
- effective Email/SMS preference flags and masked destinations from Notification;
- current customer submission summaries and the current redelivery/preference-request state.

The projection excludes internal UUIDs, internal lifecycle enums, batch/sequence, zone/slot capacity, ETA source/provider/heuristic/cache internals, distance/speed assumptions, Rider identity/contact/vehicle/live location, full address, delivery instructions/access codes, internal notes, exception investigation/root cause, raw failure notes, notification body/provider data, POD signer/signature/photo/barcode/geotag, and repository entities.

### Customer status vocabulary

| Internal Delivery fact | Customer label | Safe meaning |
| :--- | :--- | :--- |
| `DRAFT` | `Preparing delivery` | Delivery details are being prepared. |
| `READY_FOR_ASSIGNMENT` with no dispatched active batch fact | `Scheduled` | A delivery window is scheduled/prepared. |
| Active batch `DISPATCHED` fact | `Out for delivery` | Delivery is in the active dispatch. This is a projection, not a new DeliveryOrder enum. |
| `FAILED_ATTEMPT` | `Delivery attempt unsuccessful` | Another attempt may be possible; only a safe disposition message is shown. |
| `ESCALATED` | `We are reviewing your delivery` | Internal exception/investigation detail is hidden. |
| `RETURN_TO_BASE` | `Returned for assistance` | Contact support; no internal custody notes are exposed. |
| `DELIVERED` | `Delivered` | Completion recorded; POD availability may be shown. |

Safe failed-attempt text may distinguish “re-delivery may be requested”, “returned for assistance”, or “under review”. It never exposes internal exception classifications, evidence, dispute details, or free text.

## Allowed Customer Actions

### Operational notification preferences

- Customer self-service may read and replace the existing Notification-owned operational Email/SMS profile because “Manage Delivery Preferences” and US-69 channel preferences are explicit source requirements.
- The Delivery facade invokes a new Notification-root published preference-management contract with the same accepted US-69 semantics, using only the token-derived Customer and Tenant; it never imports Notification's internal application port or calls Notification repositories/tables.
- Existing defaults, masking, channel validation, optimistic version, and transactional-only purpose remain unchanged.
- IN_APP, push, WhatsApp, marketing, campaign, locale, and provider settings are unavailable.

### Customer issue

- A valid token may create a traceable Delivery-owned issue linked to the bound Delivery and Customer.
- Categories are `DELIVERY_TIMING`, `ACCESS_OR_ADDRESS_CLARIFICATION`, `DELIVERY_CONDITION`, `DELIVERY_SERVICE`, and `OTHER`; description is required, trimmed plain text, 10–1,000 characters.
- Submission creates a customer-facing reference and `SUBMITTED` status. It does not mutate Delivery status, failure attempts, exceptions, POD, slot, Rider, or Notification.
- Internal triage may later invoke US-59/US-62/US-68 through their operator-authorized workflows. Customer cannot view internal investigation notes, severity, root cause, or corrective action.

### Feedback

- Feedback is allowed only when the bound Delivery is `DELIVERED` and within the token lifetime.
- Rating is an integer 1–5; comment is optional plain text up to 1,000 characters.
- One current feedback record exists per Tenant/Delivery/Customer. A duplicate idempotency key returns the original result; a different second submission returns `SELF_SERVICE_FEEDBACK_ALREADY_SUBMITTED` rather than silently overwriting history.
- Feedback has no automatic Rider score, analytics, marketing, compensation, claim, or Delivery-state effect in US-70.

### Re-delivery and preference-change request

- A customer may submit a **request**, not directly schedule or reschedule a Delivery.
- Initial re-delivery request is available only when `DeliveryOrder.status == FAILED_ATTEMPT` and the latest failed attempt disposition is `REDELIVERY_ELIGIBLE`.
- The request may contain a preferred start/end and notes. If supplied, start/end are paired, start precedes end, minimum duration is 30 minutes, and start is within the existing 30-day horizon. Times are interpreted/displayed in the Tenant timezone and transmitted as offset-bearing instants.
- A pre-first-attempt Delivery may submit a non-binding preference-change request while customer status is `Scheduled`; it does not alter the committed window. Pre-delivery self-scheduling is not approved.
- The request is routed to the Delivery scheduling workflow for operator review. Only US-60 may create/supersede `DeliveryRedeliverySchedule` or transition `FAILED_ATTEMPT -> READY_FOR_ASSIGNMENT`.
- US-64 remains the authority for DeliverySlot availability, cutoff, buffers, capacity, reservation and overbooking protection. US-70 neither selects nor reserves a DeliverySlot and does not expose a slot repository or promise a slot from the customer request. US-70 does not reopen or replace US-60's separately accepted operational-window capacity rules.
- The customer sees `SUBMITTED`, `ACCEPTED`, `DECLINED`, or `SUPERSEDED` request status and the authoritative Delivery window after an operator action. They never see planner-only alternatives or capacity counts.
- A committed US-60 schedule continues to emit `DELIVERY_REDELIVERY_SCHEDULED`; US-69 handles the resulting notification after commit. The self-service controller never sends Email/SMS.

### Explicitly not allowed

Customer delivery/order cancellation, direct slot reservation, direct Delivery window mutation, destination/address change, payment/COD change, POD upload/download, Rider chat/call, live GPS/map, internal exception mutation, and arbitrary status changes are deferred. Address clarification may be reported as an issue only.

## API Ownership and Frozen Surface

Delivery owns a thin public self-service adapter and safe projection. These routes all require `Authorization: DeliveryAccess <token>` and contain no target identifiers:

| Method and external route | Behavior |
| :--- | :--- |
| `GET /api/public/v1/delivery-self-service` | Customer-safe Delivery projection and available actions. |
| `GET /api/public/v1/delivery-self-service/notification-preferences` | Effective Email/SMS preferences and masked destinations via Notification public contract. |
| `PUT /api/public/v1/delivery-self-service/notification-preferences` | Replace Email/SMS operational profile; body `{emailEnabled:boolean,smsEnabled:boolean,version:long|null}`. |
| `POST /api/public/v1/delivery-self-service/issues` | Submit categorized customer issue; requires `Idempotency-Key`. |
| `POST /api/public/v1/delivery-self-service/feedback` | Submit one post-delivery rating/comment; requires `Idempotency-Key`. |
| `POST /api/public/v1/delivery-self-service/redelivery-requests` | Submit re-delivery or pre-delivery preference-change request; requires `Idempotency-Key`. |

No customer slot-list endpoint is frozen because the MVP action is a request routed to scheduling, not direct booking. No customer/Delivery ID appears in a route or request body.

Internal provider-neutral contracts are frozen for:

- Organization: same-Tenant active Customer/contact projection only;
- Notification: a new root-published customer operational-preference read/replace contract and transient link delivery;
- Delivery to Notification channel sender: a transient `CustomerSelfServiceLinkIssuer` invoked at final channel-send time.

US-69 templates may include a controlled self-service-link placeholder only after US-70 implementation. Notification persists the placeholder/redacted representation, not a raw token. Immediately before each provider delivery attempt, the Notification adapter invokes the Delivery public issuance port with Tenant, Delivery, Customer, normalized destination fingerprint, allowed actions, and stable delivery-attempt idempotency key; it substitutes the returned raw URL only in memory. Re-entry for the same attempt rotates the hash in the same access record, while a later retry attempt creates its own bounded access record. Events remain unchanged and contain no token/link. No new cross-module event is frozen by US-70.

## Errors

All responses use the established structured error envelope and customer-safe messages:

- `SELF_SERVICE_ACCESS_INVALID` — 404 for missing, invalid, expired, revoked, mismatched, or cross-Tenant credentials;
- `SELF_SERVICE_ACTION_NOT_ALLOWED` — 409 for lifecycle-ineligible action;
- `SELF_SERVICE_VALIDATION_FAILED` — 400 with allow-listed field errors;
- `SELF_SERVICE_PREFERENCE_VERSION_CONFLICT` — 409;
- `SELF_SERVICE_REDELIVERY_NOT_ELIGIBLE` — 409;
- `SELF_SERVICE_FEEDBACK_ALREADY_SUBMITTED` — 409;
- `SELF_SERVICE_VERSION_CONFLICT` — 409 for concurrent state change;
- `SELF_SERVICE_IDEMPOTENCY_CONFLICT` — 409 when one key is reused with different input;
- `SELF_SERVICE_RATE_LIMITED` — 429 with bounded `Retry-After`;
- `SELF_SERVICE_UNAVAILABLE` — 503 generic retry-safe failure.

Responses never reveal Tenant/Customer/internal Delivery IDs, token state, existence checks, SQL/provider errors, or stack traces.

## Concurrency and Idempotency

- Every token lookup, revocation, action check, Delivery read/write, and submission is Tenant-qualified.
- Preference replacement reuses Notification optimistic version semantics.
- Customer submissions require a 16–128 character `Idempotency-Key`. Delivery stores only a SHA-256 request fingerprint with a unique `(tenant_id, access_id, action, idempotency_key)` constraint.
- Same key + same canonical request returns the original outcome. Same key + different request returns `SELF_SERVICE_IDEMPOTENCY_CONFLICT`.
- Token revoke versus action, Delivery completion versus request, and customer versus operator changes recheck token and Delivery versions inside the owning transaction. Revocation or newer Delivery state wins; stale customer work fails without partial effects.
- Multiple tabs/double-click/network retry cannot create duplicate issue, feedback, or preference/redelivery requests.
- A customer request does not reserve capacity. Any later schedule continues through the existing operator-authorized US-60/US-64 paths as applicable; the US-70 request never reserves capacity and no slot race is resolved by frontend state.
- Business events from accepted operator scheduling remain after-commit under P0-07. Customer submissions do not invent a cross-module event.

## Rate Limiting and Threat Controls

Minimal server-side controls, in addition to 256-bit token entropy:

- valid token: 120 reads and 10 writes per rolling 15 minutes per token hash;
- invalid access attempts: 20 per rolling 15 minutes per source IP, then 15-minute denial;
- customer submissions: 20 writes per rolling hour per bound Delivery;
- always return the same safe invalid-access response before rate-limit details can become an existence oracle;
- trusted reverse-proxy configuration is required before honoring forwarded IP headers.

The implementation must fail closed on counter/storage failure for writes and invalid-token checks. This is a narrow self-service limiter, not a general policy engine.

Threat review covers brute force, link sharing/leakage, IDOR, Tenant/customer spoofing, replay, CSRF, XSS, open redirect, referrer/log leakage, request mass assignment, slot tampering, stale token use, concurrent state changes, notification token persistence, and PII/POD/Rider leakage. Shared-link risk is inherent to possession access and is bounded by Delivery scope, action allow-list, expiry, revocation, active-token cap, contact-change revocation, audit, and rate limits.

## Persistence and Expected Migration

US-70 requires Delivery-owned persistence. No Customer-to-`app_user` association table is approved.

### `delivery_self_service_access`

`id UUID PK`; `tenant_id UUID NOT NULL`; `delivery_order_id UUID NOT NULL`; `customer_id UUID NOT NULL` logical Organization reference; `recipient_contact_hash CHAR(64) NOT NULL`; `contact_hash_key_version VARCHAR(32) NOT NULL`; `token_hash CHAR(64) NOT NULL UNIQUE`; `allowed_actions VARCHAR[] NOT NULL`; `issuance_idempotency_key VARCHAR(128) NOT NULL`; `issued_at`, `expires_at`, nullable `revoked_at`, nullable `last_used_at` as `TIMESTAMPTZ`; `use_count BIGINT NOT NULL DEFAULT 0`; nullable `revocation_reason VARCHAR(64)`; `version BIGINT NOT NULL DEFAULT 0`; standard creator/updater audit facts.

Use a same-module Tenant-consistent FK from `(delivery_order_id, tenant_id)` to Delivery Order where current schema permits. Customer is a logical cross-module UUID with no physical FK. Required indexes/constraints: token-hash uniqueness; issuance idempotency uniqueness; `(tenant_id, delivery_order_id, customer_id)`; active expiry scan; active-token cap enforced transactionally.

### `delivery_customer_submission`

`id UUID PK`; `tenant_id UUID NOT NULL`; `delivery_order_id UUID NOT NULL`; `customer_id UUID NOT NULL` logical reference; `access_id UUID NOT NULL`; `submission_type` in `DELIVERY_PREFERENCE`, `REDELIVERY_REQUEST`, `ISSUE`, `FEEDBACK`; `category VARCHAR(64)` nullable by type; `description VARCHAR(1000)` nullable by type; `rating SMALLINT` nullable by type; `preferred_start_at`, `preferred_end_at TIMESTAMPTZ` nullable by type; `status` in `SUBMITTED`, `RECORDED`, `ACCEPTED`, `DECLINED`, `SUPERSEDED`; `idempotency_key VARCHAR(128) NOT NULL`; `request_hash CHAR(64) NOT NULL`; nullable operator outcome facts; `created_at`, `updated_at TIMESTAMPTZ NOT NULL`; `version BIGINT NOT NULL DEFAULT 0`.

Use same-module Tenant-consistent FKs to Delivery Order and self-service access; no Organization physical FK. Add unique idempotency constraint, one-feedback-per-Tenant/Delivery/Customer partial uniqueness, and Tenant-leading Delivery/type/time indexes. Type-specific checks enforce required/forbidden fields and rating/window bounds.

Issue and delivery/redelivery preference submissions start `SUBMITTED`; preference/redelivery requests may become terminal `ACCEPTED`, `DECLINED`, or `SUPERSEDED` through a later operator-authorized action. Feedback is created as terminal `RECORDED`. Customer self-service cannot set or mutate these statuses.

Current Flyway head is V58. If still free at implementation start, the expected forward migration is `V59__customer_self_service_us70.sql`. Historical migrations are immutable; implementation must use the then-next free version if V59 is no longer free. No migration is created by this decision task.

Access and submission records follow the platform operational/audit retention policy. Raw token, raw contact destination, full address, POD evidence, and provider payload are never copied into either table.

## POD, ETA, Rider, Exception, and Notification Boundaries

- **POD:** show only `AVAILABLE`/`NOT_AVAILABLE` and completion time. No signer name, signature, photo, barcode, coordinates, filenames, content endpoint, or recipient confirmation is exposed.
- **ETA:** Delivery invokes the accepted US-67 read projection, never the force-recalculate command. The existing US-67 read may refresh a missing/stale cache entry under its own rules. Show estimated arrival, safe SLA wording, calculation time and stale/unavailable state. Hide heuristic/provider/source, route, speed, distance, cache generation and Rider position.
- **Rider:** no display name, driver link, phone, vehicle, shift, capacity, batch membership, or live/last location.
- **Failed Delivery/Exceptions:** show a generic customer outcome and action availability. US-59/US-62 records and US-68 Planner internals remain operator-only.
- **Notification:** customer preference calls use a new module-root published contract that preserves the accepted US-69 use-case semantics; US-69 alone sends Email/SMS. Notification history is deferred because persisted bodies/provider/audit records are operator diagnostics and are not required for US-70 acceptance.
- **IN_APP/push:** remain deferred. A Delivery token is not an authenticated `app_user`, inbox owner, or device registration.
- **OTP:** remains deferred. Token possession is the MVP access credential; US-70 does not invent OTP issue/hash/expiry/replay semantics.

## Audit and Privacy

Audit records contain access-record ID/hash prefix (never token), Tenant, Delivery, Customer, action, server time, result code, correlation ID, submission ID, old/new preference or requested-window facts as applicable, and a one-way truncated IP hash only when configured by security policy. User agent and raw IP are not required. No raw contact, address, free-text message, token, Authorization header, POD evidence, or Rider information belongs in general logs.

Customer-entered descriptions/comments are escaped as plain text, length-limited, never interpreted as markup, and visible only through separately authorized operator/customer projections. API payloads use explicit allow-listed DTOs to prevent mass assignment.

## Frontend and UX

- Implement a separate lightweight public React route `/track`, outside `ProtectedRoute` and `AppLayout`.
- It must never show the operator sidebar, header actions, breadcrumbs, menus, notifications center, internal IDs, or RBAC controls.
- Feature-first location: `frontend/src/features/delivery/selfService` with its own API, hooks, types, validation, components, and page as needed; use the shared Axios transport with a dedicated in-memory `DeliveryAccess` header path, TanStack Query, React Hook Form, Zod, and Ant Design.
- Sections: link/access state; Delivery status; ETA/window; Email/SMS preferences; available request/issue/feedback action; submitted-state confirmation; support information.
- Mobile-first responsive layout, visible labels, keyboard operation, focus management, semantic status, accessible validation summary, non-color-only states, and polite live confirmation are mandatory.
- Safe UX states: invalid/expired/revoked link (same wording), access temporarily limited, Delivery complete, ETA stale/unavailable, action unavailable, preference version conflict, request already submitted, concurrent change, and generic server error. Never show raw backend exception text.
- Online-only. No service-worker token cache or US-71 IndexedDB/offline queue.
- No customer behavior analytics, advertising, campaign tracking, or native mobile app is approved.

## Architecture Review Board P0-01 Through P0-07

- **P0-01:** Delivery domain/application depend only on ports; public web and persistence are adapters.
- **P0-02:** Delivery explicitly owns the two new tables; Notification/Organization tables remain with their owners.
- **P0-03:** Organization and Notification are consumed only through published provider-neutral module-root contracts. The existing internal Notification application use case is not imported across the boundary. No foreign repositories, entities, services, or SQL.
- **P0-04:** token, projection, submissions, idempotency, audit, customer lookup, Delivery, ETA and preference calls are Tenant-scoped; Tenant is server-derived.
- **P0-05:** possession authorization is fail-closed and separate from operator RBAC; no customer receives operator permission.
- **P0-06:** UUID/value references and focused projections only; no Customer-Delivery-Identity JPA graph or cross-module FK.
- **P0-07:** each submission uses a Delivery-owned transaction; Notification preference mutation uses its own public use case/transaction; scheduling and messages retain their accepted owning transaction and after-commit semantics.

## Mandatory Implementation Verification

Implementation and technical closure must prove:

- domain/application tests for token generation, hash-only storage, constant-time validation, scope/action/lifecycle checks, expiry, active-token cap, issuance idempotency, rotation/revocation, contact-change invalidation, submissions, validation, feedback eligibility, and customer-safe projection;
- authorization tests for invalid/expired/revoked token, Tenant A/B, Customer A/B, guessed Delivery IDs, wrong association, missing/inactive customer, action-scope denial, no operator-permission shortcut, and indistinguishable 404 denial;
- literal external `/api/public/v1/...` SecurityConfig tests plus effective servlet-path tests for every method; public authentication-layer access must never bypass token authorization;
- rate-limit, header/query/path rejection, CORS, no-store/referrer policy, XSS/open-redirect/log-redaction, mass-assignment, and token-not-persisted-in-Notification tests;
- US-60 boundary tests proving a customer creates only a request; no schedule/status/window changes until an authorized scheduling use case acts;
- US-64 capacity/cutoff/race tests retained on the eventual operator schedule; customer request never reserves or overbooks;
- optimistic concurrency and idempotency tests for double click, network retry, multiple tabs, revoke/action race, Delivery completion/request race, and operator/customer race;
- Organization/Notification published-contract tests, preference version/isolation tests, unchanged US-69 event contracts, transient final-send link substitution, provider retry issuance idempotency, and zero raw token in persisted notification/event/audit/log data;
- POD privacy and Rider/ETA/exception field-exclusion contract tests;
- PostgreSQL acceptance for V59-or-next, constraints/indexes, Tenant-qualified reads/writes, token lookup, revocation, active cap, idempotency, feedback uniqueness, concurrency, and complete current Flyway head;
- frontend Vitest for fragment consumption/removal, memory-only token handling, public shell isolation, projection/actions/forms, responsive/accessibility and all safe error states;
- real PostgreSQL-backed Chromium, no mocked business API, covering valid magic-link entry, correct status/window/ETA, preference read/write, issue submission, eligible re-delivery request, post-delivery feedback, refresh/reopen behavior, invalid/expired/revoked denial, and Tenant-B/Customer-B inaccessibility;
- relevant US-56 through US-69 regressions, architecture/Modulith verification, full Maven verify, Checkstyle, PMD, SpotBugs, TypeScript, complete Vitest, production build, changed-file lint, and `git diff --check`.

All destructive PostgreSQL acceptance uses only `transport_logistics_acceptance`. The development database is never valid acceptance evidence.

## MVP 1.4 Closure Gate

US-70 final acceptance must prove the complete source requirement and every frozen security/tenant/privacy/module boundary against real PostgreSQL and Chromium. Product-decision completion is not story completion. MVP 1.4 may close only after:

1. `MVP-1.4-US70-CUSTOMER-SELF-SERVICE-IMPLEMENTATION-001` completes;
2. US-70 technical closure passes every mandatory backend/frontend/security/architecture/database gate; and
3. independent hostile US-70 final acceptance passes.

Until then:

- US-70: `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`
- MVP 1.4: `7 / 8 COMPLETE`
- Overall: `64 / 87 COMPLETE`
- Deferred: `23 / 87`
- Current Flyway head: `V58`
- Next task: `MVP-1.4-US70-CUSTOMER-SELF-SERVICE-IMPLEMENTATION-001`

## Explicit Deferred Scope

Authenticated customer accounts and Customer/Recipient-to-`app_user` association, customer registration/password/MFA/session/refresh/logout, IN_APP/push/device registration, OTP transport/verification, direct slot booking/rescheduling, cancellation, destination/address/payment mutation, Rider chat/call/location/map, POD evidence downloads, notification history/body/provider diagnostics, manual operator token administration API/UI, marketing/analytics/localization, native app, offline mode, and a new Customer Experience bounded context are not approved by US-70 MVP.
