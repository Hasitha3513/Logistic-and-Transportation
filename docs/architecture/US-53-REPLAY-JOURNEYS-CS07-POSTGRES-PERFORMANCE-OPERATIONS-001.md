# US-53 Replay Journeys CS07 — PostgreSQL Performance and Operations

## Verdict

`PASS — IMPLEMENTATION_IN_PROGRESS / CS07_COMPLETE`

This change set proves that the existing V86/V87 Timescale history design and Tenant-leading indexes support the frozen replay workload. No V94 migration, schema change, public payload change, permission change, or producer-acceptance promotion was required.

## Operational controls

- The replay endpoints and navigation are controlled by `app.tracking.journey-replay.enabled` and `VITE_JOURNEY_REPLAY_ENABLED`; both default to enabled and may be disabled together for rollback.
- Requests are bounded by the existing seven-day range, 1,000-point page, 20,000-point browser ceiling, bounded stop analysis, and opaque cursor contracts.
- Process-local admission permits 30 requests per actor/minute and 120 requests per Tenant/minute. Rejection is HTTP 429 with code `JOURNEY_REPLAY_RATE_LIMITED` and `Retry-After: 60`.
- Every producer-overlay SQL query now applies deterministic source-time/ID ordering and `limit + 1` internally before the combined cursor page is formed.
- Existing JDBC transaction and PostgreSQL statement timeouts remain five seconds. The frontend passes an abort signal through TanStack Query and Axios when a replay request is superseded.
- Metrics contain only operation, coverage, safe rejection reason, result size, latency, and overlay type. Tenant IDs, actor IDs, Vehicle/Trip IDs, coordinates, cursors, raw telemetry, credentials, and personal data are not metric tags.

## PostgreSQL/Timescale evidence

Authoritative database: `transport_logistics_acceptance` only.

The retained plan acceptance test uses 10,000 history rows and proves the existing `idx_tracking_history_vehicle_time` path supplies Tenant/Vehicle/time filtering and deterministic time/ID ordering without a full retained-history sequential scan. CS07 additionally seeded 40,000 normalized history rows (20 Vehicles × 2,000) and used 20 simultaneous virtual-thread sessions. Each session requested an initial 1,000-point page and its 1,000-point continuation.

Observed on the local acceptance host:

| Measurement | Result | Frozen gate |
| --- | ---: | ---: |
| Initial p50 | 91 ms | informational |
| Initial p95 | 158 ms | <= 2,000 ms |
| Initial p99 | 206 ms | informational |
| Continuation p50 | 95 ms | informational |
| Continuation p95 | 119 ms | <= 1,000 ms |
| Continuation p99 | 135 ms | informational |
| Aggregate request rate | 181.00 requests/s | informational |
| Workload duration | 221 ms | informational |

These are repeatable technical-environment measurements, not production SLO certification. No deadlock, timeout, cross-Tenant result, unbounded page, or connection-pool exhaustion occurred. Test fixtures were removed by the governed clean/migrate lifecycle, leaving Flyway at V93.

## Verification

- Focused admission/performance: 4/4 PASS.
- Retained CS01–CS06, Tracking security and PostgreSQL/Timescale regression: PASS.
- Architecture/Modulith: 59/59 PASS.
- Complete Maven: 1,815 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS (18:16).
- Frontend TypeScript: PASS.
- Frontend Vitest: 325/325 PASS.
- Frontend production build: PASS (existing bundle-size advisory only).
- Changed-file ESLint: PASS.
- Real PostgreSQL-backed Chromium replay journey: 6/6 PASS.
- Checkstyle: 0 violations.
- PMD: 0 findings.
- SpotBugs: 0 findings.
- Dependency analysis: BUILD SUCCESS with retained repository-wide advisory findings.
- Docker Compose validation: PASS.
- `git diff --check`: PASS.

## Security and rollback

All reads remain explicitly Tenant-qualified and retain the exact replay permissions. The limiter keys include Tenant and actor but never expose them through metrics or responses. The existing no-store/referrer protections remain unchanged.

For rollback, set both replay feature flags false and redeploy: the backend endpoints are absent and the frontend navigation/route is removed. Retained source telemetry, audit evidence, permissions, and producer data are not deleted. Re-enabling the flags restores the derived replay because no replay-owned projection exists.

## Residual risk and next queue

Process-local rate limits apply per application instance; distributed global admission would require a separately governed product/operations decision. Timings require production-like load verification before becoming an SLO. US-53 final acceptance must still prove its own physical/provider/operator evidence and cannot inherit US-48, US-50, or US-52 physical acceptance.

Exact next queue: `US-53-REPLAY-JOURNEYS-TECHNICAL-CLOSURE-001`.
