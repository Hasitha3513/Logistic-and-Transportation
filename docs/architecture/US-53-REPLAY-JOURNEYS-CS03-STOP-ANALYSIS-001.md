# US-53 Replay Journeys CS03 — Stop Analysis

**Status:** `COMPLETE`

**Date:** 2026-09-15

**Starting HEAD:** `26a713f`

**Flyway:** V92; V93 remains unused and reserved for US-53 permissions.

## Algorithm

The read-only analyzer consumes CS02 pages in `(sourceTimestamp,id)` order under one immutable receipt
snapshot. It retains one page, the active bounded candidate, confirmed bounded results, and page-boundary
state. The stream guard rejects non-increasing tuples, cursor loops, empty nonterminal pages and snapshot
changes. Exactly 20,000 points are allowed; evidence of another point fails the whole analysis.

Only trusted points with canonical WGS84 coordinates and known accuracy at most 100 metres contribute.
Known speed at or below 3 km/h qualifies; missing speed remains missing and is accepted only as spatial
evidence. More than two minutes between source points or any ineligible point ends the candidate. Visible dwell
from first to last evidence must be at least five minutes.

Distance is Haversine using mean Earth radius 6,371,008.8 metres with the trigonometric intermediate clamped
to `[0,1]`. The centroid uses spherical unit vectors weighted by
`1 / max(accuracyMeters,1)^2`, including correct longitude wrap-around. Every point is revalidated within 50
metres whenever the centroid changes. A violating point starts a new candidate instead of being discarded.

Adjacent confirmed candidates merge only when their gap is at most two minutes, their centroids are at most
50 metres apart, and every combined evidence point remains within 50 metres of the recomputed centroid.

## Boundaries, identity and pagination

Bounded predecessor/successor evidence determines start/end truncation without extending observed timestamps.
Partial retention stays explicit and marks a lower-boundary candidate truncated. Empty retained evidence
returns `NO_DATA` and no fabricated stop.

Stop IDs are lowercase SHA-256 hex over an explicit UTF-8 canonical sequence of Tenant, Vehicle, immutable
snapshot, first history ID and last history ID. Coordinates and personal/provider data are excluded.

Derived results order by start source timestamp then stop ID. Later pages deterministically rescan the same
bounded snapshot, discard through the authenticated derived-stop tuple, return the requested 1–500 items and
read at most one additional derived stop to decide continuation. No cache or persistence is used.

## Verification

- CS03 focused domain/orchestration: 17/17 PASS.
- Real PostgreSQL/Timescale and compressed history: 13/13 PASS.
- Complete Java 21 `clean test`: 1,804 tests, zero failures/errors/skips; BUILD SUCCESS in 11:15.
- Checkstyle: zero violations.
- Task PMD: zero findings; the known 32 unrelated repository findings remain pre-existing debt.
- SpotBugs: zero findings.
- Dependency analysis, Docker Compose validation and `git diff --check`: PASS.
- Flyway remains V92 and V93 is absent.

Tenant is present in every history, Trip, Routing and cursor operation. Stop results contain no raw payload,
device/provider identity, credentials/signatures, Driver/Customer PII or raw cursor state. No persistence,
migration, API, RBAC, audit, incident overlay, frontend, Kafka, Redis or retention behavior changed.

Rollback is an application revert of CS03 wiring/analyzer code; CS01/CS02 contracts and immutable history stay
unchanged.

## Next controlled change set

`US-53-REPLAY-JOURNEYS-CS04-API-RBAC-AUDIT-001`
