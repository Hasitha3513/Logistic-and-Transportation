# US-67 Last-Mile ETA Final Acceptance

## Decision

- **Task:** `MVP-1.4-US67-LAST-MILE-ETA-FINAL-ACCEPTANCE-001-RERUN`
- **Date:** 2026-09-02
- **Decision:** `ACCEPTED — US-67 COMPLETE`
- **Provider strategy:** `HEURISTIC_ONLY`; no external provider or fallback is active.
- **US-67 migration:** `V56__delivery_rider_transport_mode_us67.sql`
- **Current Flyway head:** `V57__tenant_local_business_keys.sql`

US-67 owns the computed, non-persisted ETA projection in `com.transportlogistics.app.delivery`. It uses persisted `DeliveryRider.transportMode` through `RiderEtaContextPort`, a Haversine/circuity heuristic with zone/mode speeds and service buffers, and the fixed batch sequence. It neither creates a Route or Trip aggregate nor implements GPS, telematics, route optimisation, external routing, US-68 exceptions, US-69 notifications, or US-70 self-service.

## Acceptance Evidence

| Gate | Verified result |
| :--- | :--- |
| Heuristic, Rider mode, and origin policy | PASS — normal source is `HEURISTIC`; no hardcoded mode; batch origin is the delivery-zone depot when present. |
| Cache and concurrency | PASS — tenant/fingerprint keys, 15-minute TTL, bounded generation metadata, and conditional generation publication reject late writes. |
| Production invalidation | PASS — membership, rider assignment/reassignment/unassignment, Rider transport-mode change, batch status/dispatch, and destination-change events evict affected entries. |
| Flyway and database ownership | PASS — V1–V57 validated; V56 only adds the tenant-scoped Rider transport-mode column, constraint, and index; V57 is the legitimate later tenant-local-key migration. |
| PostgreSQL safety | PASS — destructive/direct acceptance paths resolve to `transport_logistics_acceptance`; inventory: unsafe development datasource `0`, unknown `0`. |
| Backend verification | PASS — `./mvnw verify`: 1,195 tests, 0 failures, 0 errors, 15 skipped, `BUILD SUCCESS` (4m24). |
| Architecture and ARB | PASS — 40/40 architecture tests; no P0-01 through P0-07 regression. |
| Static analysis | PASS — Checkstyle, PMD, and SpotBugs. |
| Frontend | PASS — TypeScript; Vitest 56 files / 252 tests; production build. |
| Lint | `GLOBAL_FRONTEND_LINT=BASELINE_DEBT`: 71 unchanged unrelated errors; US-67 introduced `0`, E2E introduced `0`. |
| Fresh real Chromium | PASS — 6/6 against real React, REST, Spring Security, Flyway, and `transport_logistics_acceptance` (20.7s). |
| Security and tenancy | PASS — literal `POST /api/v1/deliveries/orders/{id}/eta/calculate` requires `DELIVERY_UPDATE`; method security remains active; Tenant-B order/batch access is safe 404; client tenant spoofing is ineffective. |

The Chromium suite proves Rider creation, batch ETA and reload, single-order recalculation, `MOTORBIKE -> BICYCLE` invalidation and changed ETA, missing-coordinate `DELIVERY_ETA_COORDINATES_MISSING`, Tenant-B IDOR, tenant-header spoofing resistance, and a real `DELIVERY_VIEW`-only user receiving direct calculation `403`. `/e2e/tenant-fixtures` is restricted to the `e2e` profile.

## Test Infrastructure and Security Review

The Hikari correction is confined to `src/test/resources/application.yml`: pool size two remains sufficient for Flyway/Spring and inactive test-context connections expire after ten seconds. Production datasource configuration is unchanged. ETA events carry tenant and operational projection data only; no credentials, medical/drug-test data, or customer-sensitive data were introduced. The React client displays backend ETA responses and does not calculate ETA locally.

## Final Accounting

- MVP 1.4: **5 / 8 COMPLETE**
- Overall: **62 / 87 COMPLETE**
- Deferred: **25 / 87**
- Next task: `MVP-1.4-US68-LAST-MILE-EXCEPTIONS-PRODUCT-DECISIONS-001`

The earlier failed acceptance report is superseded by this rerun; no product decision was reopened.
