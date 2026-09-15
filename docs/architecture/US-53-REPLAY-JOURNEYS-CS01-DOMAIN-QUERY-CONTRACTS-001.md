# US-53 Replay Journeys CS01 — Domain Query Contracts

**Status:** `COMPLETE`

**Date:** 2026-09-15

**Starting application baseline:** `e012577f4c7589fc44127d6728fd1f9af24f6ab9`

**Flyway head:** V92; V93 remains unused and reserved for the later permission change set.

## Delivered contracts

Tracking now owns framework-neutral immutable contracts for Tenant-explicit Vehicle or Trip replay selection,
requested and effective time ranges, bounded pages, chronological replay points, protected cursor state,
retention coverage, data gaps, stop evidence, confirmed stops and privacy-safe incident overlays. Narrow inbound
and outbound ports cover history queries, stop analysis, overlays, Trip attribution, exact immutable route
revision context and opaque cursor encoding without accessing another module's persistence.

No Spring, web, JPA, JDBC, PostgreSQL, Timescale, Redis, Kafka or vendor DTO type appears in the new domain and
port contracts.

## Invariants

- A query selects exactly one Vehicle or Trip. Tenant and actor identity are internal trusted context, never
  request payload fields.
- Vehicle queries may omit both timestamps and then use `[now - 6 hours, now)`. Trip queries require an explicit
  range. Start must precede end, future end times are rejected, and seven days is the inclusive maximum.
- Page limit defaults to 1,000 and is capped at 2,000. Responses carry the 20,000-point browser ceiling as
  metadata; evidence is never silently represented as complete after truncation.
- Ordering is exactly `sourceTimestamp ASC, historyId ASC`. Duplicate history identities resolve
  deterministically.
- Cursor adapters receive only the final ordering position and a Tenant/selector/range binding. Invalid,
  expired, structurally malformed, cross-Tenant or mismatched cursors fail through stable domain errors.
- Requested range remains distinct from effective retained range. `COMPLETE`, `PARTIAL_RETENTION` and
  `NO_DATA` are explicit and a partial result cannot masquerade as complete coverage.

## Stop-analysis contract

Eligible evidence is trusted, has known accuracy no worse than 100.000 metres, and has speed at or below
3.000 km/h. Missing speed remains eligible only for the later spatial-evidence evaluation; it is not inferred
as stationary by CS01. Candidate confirmation requires at least five minutes, telemetry gaps may not exceed
two minutes, and merging requires both a gap no greater than two minutes and centroid separation no greater
than 50.000 metres. Boundary-truncated evidence is explicitly labelled.

CS01 defines these pure predicates and immutable inputs/results only. Complete clustering, centroid calculation
and orchestration remain assigned to CS03.

## Overlay contract

- Geofence: `ACCEPTED`.
- Speed: opt-in and `FIELD_FIDELITY_PENDING`.
- Route deviation: opt-in and `FIELD_ACCEPTANCE_PENDING`.
- Idle/engine evidence: unavailable and rejected.

Overlay records contain evidence identity, producer status, source time and privacy-safe display facts. They do
not expose coordinates, raw telemetry, provider/device material, credentials, signatures, Driver PII or
Customer PII. Display never upgrades producer acceptance.

## Independent Identity remediation

Before closure, the full suite exposed a pre-existing V89 parity defect: the local opt-in administrator test
expected 188 permissions but the bootstrap set omitted the four V89 route-deviation permissions. Commit
`e012577f4c7589fc44127d6728fd1f9af24f6ab9` added exactly the existing V89 keys and an explicit membership
assertion. It added no migration, permission, role assignment or security-policy expansion.

## Verification

All commands used Java 21. PostgreSQL-backed tests used the isolated acceptance instance on port 5433.

| Gate | Result |
| --- | --- |
| Isolated Identity defect | 1/1 PASS |
| Identity/authorization/PostgreSQL permission regression | 51/51 PASS |
| Focused CS01 | 14/14 PASS |
| Tracking/telemetry/Trip/Routing affected regression | 128/128 PASS |
| Architecture/Modulith/ownership | 59/59 PASS |
| Complete `clean test` | 1,768/1,768 PASS; 0 failures, 0 errors, 0 skipped; 10:26 |
| Checkstyle | PASS; 0 violations |
| PMD | PASS after replacing three CS01 wildcard imports; 0 CS01 findings |
| SpotBugs | PASS; 0 findings |
| Dependency analysis | PASS; repository-level advisory warnings retained |
| `git diff --check` | PASS |

## Explicit exclusions

CS01 introduces no database migration or adapter, REST/API behavior, OpenAPI contract, permission or role grant,
audit persistence, frontend, map, Kafka behavior, notification, export, engine/idle inference, or acceptance
status change for US-48 through US-52.

## Rollback

Revert the CS01 application commit. There is no schema or data rollback and no external state to reconcile.

## Next controlled change set

`US-53-REPLAY-JOURNEYS-CS02-TIMESCALE-QUERY-ADAPTERS-001`
