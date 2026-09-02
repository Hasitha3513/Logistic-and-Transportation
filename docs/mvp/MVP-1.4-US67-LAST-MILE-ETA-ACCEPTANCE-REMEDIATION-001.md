# US-67 Last-Mile ETA Technical Closure Evidence

**Task ID:** `MVP-1.4-US67-FULL-TECHNICAL-CLOSURE-001`
**User Story:** `US-67` — Calculate Last-Mile ETA
**Date:** 2026-09-02
**Status:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`
**US-67 migration:** `V56__delivery_rider_transport_mode_us67.sql`
**Current repository/runtime Flyway head:** `V57__tenant_local_business_keys.sql`

## Scope and implementation confirmation

US-67 remains intentionally limited to `HEURISTIC_ONLY` routing; no external routing provider was introduced. ETA calculation obtains Rider mode through the Delivery-owned `RiderEtaContextPort`, uses the Delivery Zone depot-origin fallback, and never fabricates Rider GPS coordinates.

`DeliveryRider.transportMode` supports `BICYCLE`, `MOTORBIKE`, `VAN`, `CAR`, and `WALKER`. The tenant-qualified ETA cache uses a generation token to reject stale writes. Cache keys include tenant identity; order, batch membership, Rider assignment/reassignment, Rider mode change, dispatch, and destination-change events invalidate affected entries. Generation entries are bounded and pruned when their cached subject disappears; no global cache clear is used by business invalidation.

Security is enforced twice: `SecurityConfig` explicitly protects the literal `/api/v1/deliveries/orders/*/eta/calculate` path with `DELIVERY_UPDATE`, and controller methods use `@PreAuthorize`. Single-order reads require `DELIVERY_VIEW`; single-order recalculation requires `DELIVERY_UPDATE`; Batch and Rider actions retain their declared `DELIVERY_BATCH_*` and `DELIVERY_RIDER_*` authorities. The profile-restricted `/e2e/tenant-fixtures` controller is annotated `@Profile("e2e")`.

## PostgreSQL acceptance safety and remediation

All destructive direct PostgreSQL acceptance paths resolve through `PostgreSqlIntegrationTest`. Local mode requires all of `TRANSPORT_TEST_DB_MODE=local`, `TRANSPORT_TEST_DB_URL`, `TRANSPORT_TEST_DB_USERNAME`, and `TRANSPORT_TEST_DB_PASSWORD`; it rejects non-test database names unless an explicit destructive acknowledgement is supplied. The executed datasource was exclusively `transport_logistics_acceptance` at port 5433. The development database `transport_logistics` was not used.

| PostgreSQL-capable destructive test path classification | Count |
| :--- | ---: |
| `SAFE_SHARED` — guarded shared local/Testcontainers resolver | 9 |
| `SAFE_EXPLICIT` | 0 |
| `TESTCONTAINERS_ONLY_EXCLUDED_IN_LOCAL_MODE` | 0 |
| `UNSAFE_DEVELOPMENT_DATASOURCE` | 0 |
| `UNKNOWN` | 0 |

The nine shared paths are the delivery analytics, number, order, and redelivery acceptance tests; bunker concurrency; offline-sync invariant; production invariant; local sample-data bootstrap; and sample-data RBAC idempotency tests. Testcontainers remains available when local mode is not selected; Docker Desktop API compatibility is an external-runtime concern, not a product fallback.

The PostgreSQL fixture remediation retained business-key role upserts and actual persisted role IDs, including the alternate ADMIN UUID/repeated-fixture regression. It retained safe Flyway baseline restoration instead of shared truncation and the corrected cargo, notification, trip fuel-cost, business-authorization, bunker, and offline-sync cleanup dependency ordering.

The full-suite root cause was accumulation of Hikari connections across many Spring contexts. The final, test-only fix keeps the required pool size at two and sets a 10-second idle timeout in `src/test/resources/application.yml`, allowing inactive test-context pools to release connections. A Spring context-cache eviction experiment was rejected because it conflicted with shared Flyway schema restoration. Production pool configuration is unchanged.

## Executed verification

| Gate | Actual result |
| :--- | :--- |
| Focused PostgreSQL regression | 162 tests, 0 failures, 0 errors — PASS |
| Complete `./mvnw verify` | 1,195 tests, 0 failures, 0 errors, 15 skipped; 4m24s — BUILD SUCCESS |
| Flyway acceptance runtime | V1 through V57 — PASS; V56 recorded as the US-67 migration |
| Architecture | 40/40 — PASS (`ModulithBoundaryEnforcement`, Hexagonal Layer, Table Ownership, Module Boundary, Application Modules) |
| Checkstyle | PASS; 0 violations |
| PMD | PASS |
| SpotBugs | PASS |
| TypeScript | `tsc -b` — PASS (bundled Node v24.19.0) |
| Vitest | 56 files, 252 tests passed, 0 failed — PASS |
| Production build | Vite production build — PASS; existing chunk-size advisory only |
| Changed-file ESLint | No changed frontend or E2E files; US-67 introduced errors: 0; E2E introduced errors: 0 |
| Global ESLint | `BASELINE_DEBT`: 71 pre-existing errors in seven unrelated Delivery analytics/zones/slots/riders/batches/exceptions files; no US-67/E2E introduced error |
| `git diff --check` | PASS |

## Real Chromium acceptance evidence

`frontend/e2e/tests/delivery/deliveryEta.real.spec.ts` was run with one Chromium worker, fresh Playwright-managed backend/frontend sessions, E2E profile fixture support, and the isolated PostgreSQL acceptance database. It uses real authentication, Rider, Delivery Zone, Delivery Order, Delivery Batch, order ETA, batch ETA, and recalculation APIs; those core flows are not mocked.

**Result: 6/6 PASS (21.5 seconds).**

1. Real UI/backend MOTORBIKE Batch ETA and browser reload.
2. Real single-order ETA view and recalculation.
3. `MOTORBIKE -> BICYCLE` change and cache invalidation; recalculated duration increases.
4. Missing destination coordinates: safe UI state and `400 DELIVERY_ETA_COORDINATES_MISSING`.
5. Tenant-B Order and Batch ETA read/calculate IDOR attempts: `404`; spoofed `X-Tenant-Id` header is ineffective.
6. A fresh limited user with `DELIVERY_VIEW` but without `DELIVERY_UPDATE`: recalculate hidden and literal direct order-calculate POST returns `403`.

## Security and performance conclusion

Tenant context is server-authoritative, client tenant spoofing is ineffective, and cross-tenant IDs cannot disclose or calculate ETA. The cache lifecycle is tenant-scoped, generation-aware, bounded, and event-invalidated. Rider context lookup is a focused port lookup and batch calculation works from tenant-qualified membership; no evident global cache clearing, unbounded generation map, N+1 path, or connection-pool accumulation remains.

## Status and next task

Technical closure is complete. US-67 is **not accepted complete** and remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING` pending final acceptance.

- MVP 1.4: **4 / 8 COMPLETE**
- Overall: **61 / 87 COMPLETE**
- Deferred: **26 / 87**
- Next task: `MVP-1.4-US67-LAST-MILE-ETA-FINAL-ACCEPTANCE-001-RERUN`
