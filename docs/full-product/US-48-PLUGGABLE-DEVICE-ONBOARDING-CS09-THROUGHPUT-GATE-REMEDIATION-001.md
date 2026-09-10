# US-48 CS09 Throughput Gate Remediation

## Result

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS09-THROUGHPUT-GATE-REMEDIATION-001` is complete. The accepted signed-ingress workload, two concurrent 500-message HTTP batches, full PostgreSQL persistence, assertions and hard `>=1000 msg/s` burst threshold are unchanged. Flyway remains V76 and no schema, index, API, permission, event, outbox or product contract changed.

US-48 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`; accounting remains 72/87 complete with 15 remaining. This technical result does not replace physical FMC130/Flespi evidence and does not authorize US-49.

## Root cause and diagnostic findings

The normalized JDBC ingest loop repeated transaction-stable PostgreSQL work for every packet in one accepted HTTP batch:

- it loaded the identical Tenant retention policy for every message even though the batch has one `receivedAt`;
- it reacquired the same transaction-scoped advisory device lock and reloaded the same ACTIVE device/provider authority for every message from that device;
- it reacquired the same transaction-scoped advisory Vehicle lock for every message associated with that Vehicle.

For a 500-message request this contributed 1,996 avoidable SQL round trips after the first packet: 499 retention reads, 499 repeated device-lock calls, 499 repeated device-authority reads and 499 repeated Vehicle-lock calls. The source-time association lookup, dedupe lookup, trusted-latest read, immutable insert and latest projection remained the per-message business path.

Source inspection also established that CS09 `currentProviderBinding` and `currentVehicleAssociation` response enrichment is confined to management reads and is not invoked by telemetry ingestion. The performance test opens no UI page, so CS09 TanStack invalidation/polling does not overlap its measured interval. The accepted benchmark still uses the existing browser request client, endpoint, concurrency and timer boundaries. No packet auditing or per-packet application logging was found, and no pool-size, durability or threshold tuning was used.

## Remediation

`JdbcTrackingStore` now creates a transaction-local batch context. It loads the retention policy once per batch and acquires/revalidates each device and Vehicle authority once per transaction. Every command is still validated. Transaction-scoped advisory locks preserve serialization against lifecycle, binding and association mutations; authority is reloaded after the first device lock. Source-time Vehicle association remains resolved for every message, and dedupe, payload-conflict, trust, ordering, retention, append-only history and latest-received/latest-trusted semantics are unchanged.

## Throughput matrix

Before remediation, unchanged burst measurements were 876.7, 986.6 and 921.5 msg/s. Sustained measurements were 568.8, 413.4 and 473.1 msg/s and already satisfied the `>=200 msg/s` gate.

Accepted post-remediation measurements:

| Context | Sustained | Burst | Result |
| :--- | ---: | ---: | :--- |
| Isolated fresh process 1 | 702.4 msg/s | 1,300.5 msg/s | PASS |
| Isolated fresh process 2 | 542.6 msg/s | 1,675.3 msg/s | PASS |
| Complete Tracking Chromium, warmed after CS09 onboarding | 621.0 msg/s | 1,229.4 msg/s | PASS |
| Explicit rerun after the complete functional suite | 632.1 msg/s | 1,390.9 msg/s | PASS |

The first three accepted burst runs are all above 1,000 msg/s; their median is 1,300.5 msg/s. The fourth post-functional run independently passes. No measured warm-up was added and no failing run was discarded.

## Verification evidence

- Focused PostgreSQL concurrency/remediation/onboarding contract: 20/20 PASS; clean Flyway restoration through V76 using only `transport_logistics_acceptance`.
- Complete Tracking Java package: 112/112 PASS.
- Complete Maven `verify`: 1,513 tests, 0 failures, 0 errors, 15 skipped; BUILD SUCCESS in 08:43.
- Architecture: 49/49 PASS.
- Checkstyle: 0 violations; PMD: BUILD SUCCESS; SpotBugs: 0 findings.
- Complete real PostgreSQL-backed Chromium Tracking suite: 24/24 PASS, including CS09 onboarding 3/3, functional Tracking 10/10, CS08 Provider Connections 10/10 and throughput 1/1.
- TypeScript: PASS; full Vitest: 290/290 PASS; production build: PASS.
- Tracking/CS09 changed-file ESLint: 0 findings.
- Repository-wide ESLint baseline: 71 pre-existing errors in seven Delivery-module files; no finding is in Tracking or a CS09 file. This unrelated global debt is not hidden and was not modified.
- `git diff --check`: PASS before evidence synchronization and required again at final handoff.
- Development database authoritative evidence: NO. All accepted database-backed results used `transport_logistics_acceptance`.

## Scope exclusions

There is no V77, migration, table, index, dependency, public API, permission, event, outbox, product-semantic or acceptance-accounting change. The existing central-knowledge-base commit `76a6528` is preserved without amend or recreation. CS09 final frontend rerun remains separate.

## Next task

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS09-FRONTEND-DEVICE-ONBOARDING-001-FINAL-RERUN`
