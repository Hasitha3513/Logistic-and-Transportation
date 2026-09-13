# Test Infrastructure V86 and Docker 29 Closure

**Task:** `TEST-INFRA-V86-FLYWAY-HEAD-ASSERTION-AND-TESTCONTAINERS-CLOSURE-001`
**Status:** COMPLETE
**Date:** 2026-09-13

## Remediation

- Testcontainers is aligned through its 2.0.3 BOM and renamed PostgreSQL/JUnit modules.
- The effective Docker client is docker-java 3.7.0 with normal client/daemon negotiation. The
  replaced Testcontainers 1.21.3/docker-java 3.4.2 stack attempted Docker API 1.32 against the
  Docker 29 daemon's API 1.40 minimum.
- `DeliveryNotificationPostgreSqlAcceptanceTest` continues to verify every V58 delivery-notification
  asset and now truthfully asserts the immutable current Flyway head V86.
- Commit `9e5510498cb7fc15c955afc8178fd67ee209fab0` remains the verified ownership baseline:
  `tracking_position_history` is Tracking-owned.

## Verification

| Gate | Result |
| :--- | :--- |
| Notification PostgreSQL acceptance | 2/2 PASS |
| V86 TimescaleDB migration acceptance | 1/1 PASS |
| Shared PostgreSQL Testcontainers path | 6/6 PASS |
| Complete PostgreSQL migration/invariant group | 234/234 PASS; 0 skipped; 10:47 |
| Database table ownership | 3/3 PASS |
| Complete architecture selection | 58/58 PASS |
| Complete Maven `clean test` | 1,691/1,691 PASS; 0 skipped; BUILD SUCCESS; 14:32 |
| Dependency consistency | Testcontainers 2.0.3 only; docker-java 3.7.0 only |
| Diff hygiene | PASS |

All destructive local-database verification used only `transport_logistics_acceptance`. The real
Timescale assertion used a disposable Testcontainers database. No migration, production behavior,
API, frontend, permission or security contract changed. The existing `TripMapper` unmapped
`tenantId` warning remains explicitly outside this task.

## Risk and rollback

The pre-existing `timescale/timescaledb:latest-pg16` image reference is mutable. Select an exact
approved tag or digest in a separate supply-chain decision; this closure does not change it.

Rollback is the dependency/test/documentation commit only. V86 and application production code are
unchanged.

MVP accounting remains 73/87. The executable queue is
`HYBRID-TELEMETRY-TS02-KAFKA-CONTRACT-AND-SECURE-INGRESS`.
