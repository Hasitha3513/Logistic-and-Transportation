# US-55 CS02 Canonical Signal Contract Closure

**Task:** `US-55-HANDLE-GPS-EDGE-CASES-CS02-CANONICAL-SIGNAL-CONTRACT-001`

**Verdict:** PASS

**Story state:** `IMPLEMENTATION_IN_PROGRESS / CS02_COMPLETE`

**Flyway head:** V95; no migration was created
**Accounting:** 73 / 87 COMPLETE; 14 / 87 remaining

## Delivered contract

- Preserved immutable V1 production and DLT topics.
- Added the additive V2 production and DLT topics with topic/envelope version validation.
- Retained V1 consumers while adding separate V2 deserialization paths for Timescale history and Redis live state.
- Moved normalized ingress to exactly one V2 publication; dual publication is prohibited.
- Added optional tamper, battery-level, battery-voltage, external-power and charging observations with frozen enums,
  ranges and precision.
- Added a framework-neutral, Tenant-qualified, source-time capability lookup contract. Persistence remains reserved
  for the governed CS03 authorization slice.
- Preserved V1/V2 common dedupe identity, immutable history and detector-dispatch idempotency.
- Added explicit Flespi and Traccar mappings; Generic ingress does not pass arbitrary signal claims.
- Preserved privacy constraints and version-aware Redis reconstruction without adding type metadata to the Kafka
  contract.

## Database safety

Destructive acceptance paths are guarded using the connected PostgreSQL database name, which must equal
`transport_logistics_acceptance`. The remediation was delivered separately in commits `ba7723d` and `343315b`.
No development-database restore was attempted or claimed.

## Verification

| Gate | Result |
| --- | --- |
| Focused CS02, architecture and proxy compatibility | 76 / 76 PASS |
| Real Kafka V1/V2 publication and V2 DLT | 2 / 2 PASS |
| Timescale V1/V2 cross-version idempotency and contract | 11 / 11 PASS; clean V1 to V95 |
| Redis/Kafka live projection focused rerun | 4 / 4 PASS |
| Complete backend | 1,861 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS; 18:54 |
| Checkstyle | 0 violations; BUILD SUCCESS |
| PMD | BUILD SUCCESS |
| SpotBugs | 0 findings; BUILD SUCCESS |
| Dependency analysis | BUILD SUCCESS; existing declared/used warnings retained |
| Docker Compose validation | PASS |
| `git diff --check` | PASS |

The initial complete run exposed one stale V1 test captor and a version-neutral Redis deserialization defect. Both
primary causes were corrected, their focused groups passed, and the complete suite then passed from a clean build.

## Rollout and rollback

Deploy V1/V2 consumers before V2-producing ingress. Existing V1 backlog remains consumable and committed V1
offsets are not reset. Rollback moves the normalized producer to V1 while retaining dual consumers; it does not
republish, enrich or rewrite stored evidence. V1 retirement requires a separate governed decision after producers,
backlog, retention and rollback-window checks.

## Residual scope and next queue

CS02 adds no persistence for optional observations or capability history. The exact next governed queue is:

`US-55-HANDLE-GPS-EDGE-CASES-CS03-PERSISTENCE-AUTHORIZATION-001`
