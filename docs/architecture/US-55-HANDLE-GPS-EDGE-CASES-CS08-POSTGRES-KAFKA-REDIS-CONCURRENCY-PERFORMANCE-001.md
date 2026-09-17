# US-55 CS08 PostgreSQL, Kafka and Redis Closure Evidence

## Verdict

`PASS` — CS08 is technically complete at Flyway V100. US-55 remains implementation-in-progress; story accounting remains 73/87.

## Environment and isolation

- PostgreSQL/TimescaleDB: `transport_logistics_acceptance` on the isolated port only.
- Kafka: Apache Kafka 3.7.2, dedicated Tracking topics and consumer groups.
- Redis: Redis 7.4, Tenant-qualified `tracking:live:{tenantId}:vehicleId` keys.
- Runtime: Java 21, hybrid storage enabled, external notification delivery disabled.
- No V101 or historical-migration edit was required.

## Corrections proven by CS08

1. First-observation episode creation is serialized with a Tenant/device-qualified PostgreSQL transaction advisory lock. This closes the absent-row race while preserving the V97 partial uniqueness invariant.
2. Signed HTTP batches are normalized and authorized before Kafka publication, then submitted together and acknowledged against one bounded deadline. Kafka still uses `acks=all` and idempotent production; delivery remains at-least-once and business effects remain idempotent.
3. The unchanged `/api/v1/tracking/vehicles/{id}/latest` contract now falls back to the governed Redis projection when the legacy PostgreSQL live row is absent. The existing PostgreSQL result remains first priority.
4. The real performance fixture binds devices to the exact governed fixture connection and expects the current durable-ingress `202 Accepted` contract.

## Correctness and concurrency

- Concurrent same-Tenant first detections converge to one active episode and one evidence chain.
- Different Tenants proceed independently and cannot observe each other's episode.
- Existing CS04–CS07 tests retain duplicate, stale/out-of-order, recovery, acknowledgement replay, notification and Operations idempotency semantics.
- Focused PostgreSQL/Kafka/Redis/security selection: 39/39 PASS.
- Broader infrastructure selection: 43/43 PASS (42 container-native plus the separately configured proxy-context test).

## Controlled performance evidence

Final real Chromium run, 2026-09-17, isolated local services:

| Workload | Result | Frozen target |
| --- | ---: | ---: |
| Sustained signed ingress | 882.2 msg/s | >= 200 msg/s |
| Two-request burst | 3,884.2 msg/s | >= 1,000 msg/s |
| Latest-state latency p95, 20 samples | 21.9 ms | <= 200 ms |
| History latency p95, 20 samples | 19.1 ms | <= 500 ms |
| Errors | 0 | 0 |

These are controlled-environment results, not production-capacity or physical-device certification. Kafka offsets drained through the governed consumers; Redis projection and Timescale history remained Tenant-qualified. The run used bounded readiness polling before latency sampling, so consumer startup was excluded from steady-state read latency.

## Query-plan disposition

The production episode, evidence, capability, acknowledgement, Notification and Operations paths retain the V97–V100 Tenant-leading indexes and constraints. Representative acceptance data did not prove a missing index. Small or unselective fixtures were not treated as index defects, and no V101 proposal was justified.

## Gates

- Architecture and Modulith: 59/59 PASS.
- Complete backend: 1,892 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS.
- Frontend continuity: TypeScript PASS; Vitest 336/336 PASS; production build PASS; changed-scope ESLint PASS.
- Real GPS-exception Chromium continuity: 6/6 PASS.
- Real hybrid performance Chromium: 1/1 PASS.
- Checkstyle, PMD and SpotBugs: PASS.
- Dependency analysis: BUILD SUCCESS with unchanged repository-wide declared/used warnings.
- Docker Compose validation and `git diff --check`: PASS.

## Recovery and residual risk

Kafka replay and Redis reconstruction use durable Timescale history and idempotent event identities; Redis is rebuildable and never authoritative for exception history. Restart tests retain V1/V2 compatibility and acknowledge only after governed work completes. Physical GPS/provider fidelity and future-Tenant Notification provisioning remain separate approved holds.

## Next governed queue

`US-55-HANDLE-GPS-EDGE-CASES-TECHNICAL-CLOSURE-001`
