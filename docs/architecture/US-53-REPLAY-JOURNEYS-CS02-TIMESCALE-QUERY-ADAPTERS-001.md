# US-53 Replay Journeys CS02 — Timescale Query Adapters

**Status:** `COMPLETE`

**Date:** 2026-09-15

**Starting application baseline:** `e44412b6c5aaa203a4b29a14367db2ebca815ad4`

**Flyway head:** V92; no migration was created and V93 remains reserved for the later permission change set.

## Delivered adapters

Tracking now queries replay points directly from the existing `tracking_position_history` Timescale hypertable.
The adapter uses Tenant and Vehicle predicates, the half-open source-time range, a fixed `received_at` snapshot,
and the stable keyset `(source_timestamp ASC, id ASC)`. It fetches `limit + 1`, emits an HMAC-SHA256 protected
cursor bound to Tenant, selector, requested range and snapshot, and never uses offsets or returns a partial page
as complete evidence.

Trip selection resolves the same-Tenant replay scope through Trip's published contract and performs one bounded
assignment-range lookup for the effective range. Point attribution is completed in memory, including explicit
`UNATTRIBUTED` and `AMBIGUOUS` states, so replay does not perform a per-point/N+1 Trip query. Routing is accessed
only through its published exact route/revision geometry contract and is cached once per distinct immutable
route context within the request.

## Query and evidence semantics

- Read transactions are read-only and bounded to five seconds; PostgreSQL receives a transaction-local five
  second statement timeout.
- Requested range remains separate from available range. The existing 180-day retention contract produces
  `COMPLETE`, `PARTIAL_RETENTION`, or `NO_DATA` truthfully, with an explicit retention gap when applicable.
- Coordinates and optional decimals retain PostgreSQL precision. Missing required coordinates fail closed.
- Trust, quality, ordering, stale-at-receipt, out-of-order, late, clock-skew, time-gap and implausible-jump facts
  are preserved or derived without rewriting source evidence.
- Page-boundary continuity uses one bounded predecessor query. No raw payload, provider/device secret,
  signature, Driver PII, or Customer PII is selected.
- Stops and incident overlays remain explicitly unavailable for CS03 and later change sets; CS02 does not
  fabricate them.

## Timescale and performance evidence

The genuine TimescaleDB acceptance suite inserted a point into an old chunk, compressed that chunk, and read
the point through the CS02 adapter. All six Timescale tests passed and both existing compression and retention
policies remained registered. A representative 10,000-row PostgreSQL workload verified that the existing
Tenant/Vehicle/source-time history index supports the bounded keyset query without a sequential scan. These
measurements are environment evidence, not a production latency or throughput guarantee.

## Verification

All backend commands used Java 21 and accepted PostgreSQL evidence used only the isolated acceptance database
on port 5433.

| Gate | Result |
| --- | --- |
| Focused CS02 unit and contract tests | 15/15 PASS |
| CS02 PostgreSQL adapter acceptance | 5/5 PASS |
| Genuine TimescaleDB/compressed-chunk acceptance | 6/6 PASS |
| Affected Tracking/Trip/Routing/architecture regression | 84/84 PASS |
| Complete `clean test` | 1,787/1,787 PASS; 0 failures, 0 errors, 0 skipped; 11:07 |
| Checkstyle | PASS; 0 violations |
| PMD, CS02 scope | PASS; no task findings |
| PMD, repository-wide direct goal | 32 pre-existing unrelated findings; classified global debt |
| SpotBugs | PASS; 0 findings |
| Dependency analysis | PASS; pre-existing aggregate-starter/transitive advisory warnings retained |
| Docker Compose validation | PASS |
| `git diff --check` | PASS |

The repository-wide direct PMD goal reports wildcard-import debt in existing Fuel, Notification,
Organization and other unrelated files. No unsuppressed PMD finding names a CS02 file. Correcting that debt
would violate this change set's minimal-diff boundary and is not a CS02 functional or architectural failure.

## Security and boundaries

Tenant identity is mandatory in history, Trip and Routing lookups and in the cursor binding. A cursor cannot be
replayed across Tenant, selector or time range. Tracking reads no Trip or Routing table directly. The adapters
expose only provider-neutral domain contracts and preserve the existing Modulith ownership boundary.

## Rollback and residual risk

Rollback is an application revert only; no schema or stored data changed. Performance evidence is bounded to
the representative acceptance workload. Physical-provider acceptance remains independent, and stop analysis,
overlays, HTTP/RBAC, audit and frontend work remain governed by later US-53 change sets.

## Next controlled change set

`US-53-REPLAY-JOURNEYS-CS03-STOP-ANALYSIS-001`
