# US-53 Replay Journeys Technical Closure

**Verdict:** `TECHNICALLY_COMPLETE`

**Acceptance:** `ACCEPTANCE_PENDING`

**Date:** 2026-09-15

**Application baseline:** `432c069dcf0602c74c4b7ea4fd331fc02d953a1e`

**Flyway head:** V93

**Accounting:** 73/87 complete; 14/87 remaining

## Executive summary

US-53 satisfies its approved technical contract from the streaming/cursor remediation and Trip prerequisite through CS01–CS07. Tracking reconstructs bounded chronological journeys directly from immutable Timescale history, supplies exact published Trip/Routing context, derives deterministic stops without persistence, adds privacy-minimized producer-labelled incidents, enforces Tenant/RBAC/audit boundaries, and provides an accessible responsive operator experience. Consolidated PostgreSQL, Timescale, architecture, Maven, frontend, Chromium, performance and static-analysis gates pass.

The story is not finally accepted. Its frozen boundary requires genuine retained provider/device telemetry, an authorized Vehicle/Trip journey with real stop/gap evidence where safely available, provenance-preserving overlay review, privacy review, and operator sign-off. Physical acceptance is story-specific and is not inherited from US-48, US-49, US-50, or US-52.

## Source and change-set reconciliation

The original DOCX, mind map, US-51–US-60 diagrams, approved US-53 decisions, both roadmaps, applicable governance, and all prerequisite/CS evidence were reconciled. The approved product decision resolves diagram ambiguity by making Timescale history authoritative, keeping export and engine/idle inference outside Phase 1, and requiring published cross-module contracts.

| Slice | Result | Technical evidence |
| --- | --- | --- |
| Streaming/cursor remediation | SATISFIED | Immutable snapshot/boundary evidence, purpose-bound cursors, 20,000-point stream guard and stop paging. |
| Trip prerequisite | SATISFIED | One Tenant-qualified bounded assignment-range query; no Tracking access to Trip persistence. |
| CS01 | SATISFIED | Framework-neutral replay, stop and incident domain/query contracts. |
| CS02 | SATISFIED | Tenant-leading Timescale keyset queries, retention truth and exact Trip/Routing attribution. |
| CS03 | SATISFIED | Deterministic bounded stop analysis with spherical weighted centroid and explicit truncation. |
| CS04/V93 | SATISFIED | Three read-only POST APIs, two narrow permissions, literal-path/use-case security and minimized audit. |
| CS05 | SATISFIED | Accessible responsive map/timeline playback, quality/gap warnings and explicit coordinate expansion. |
| CS06 | SATISFIED | Accepted geofence and opt-in technical speed/route-deviation overlays with bounded paging. |
| CS07 | SATISFIED | Rate limits, safe metrics, rollback flags, bounded producer SQL and 20-session performance evidence. |

## Contract closure

- Movement source is only `tracking_position_history`; Redis and legacy position storage are not replay evidence.
- Selection is exactly one Vehicle or Trip, range is at most seven days, each point page is bounded, and browser accumulation stops at 20,000 points.
- Ordering is immutable `(source_timestamp ASC, history_id ASC)` with a fixed receipt snapshot and authenticated Tenant/filter-bound cursor.
- Coverage is explicitly `COMPLETE`, `PARTIAL_RETENTION`, or `NO_DATA`; source/receipt time, trust, quality, ordering and attribution remain distinct.
- Stop analysis uses trusted points, known accuracy at most 100 m, speed at most 3 km/h or explicit spatial-only evidence, five-minute dwell, two-minute maximum gap and 50 m radius.
- US-49 evidence remains accepted. US-50 and US-52 overlays remain opt-in and explicitly field-acceptance/fidelity pending. US-51 idle evidence is unavailable and is not inferred.
- APIs remain under `/api/v1/tracking/journey-replays`, use POST bodies to protect selectors/ranges, and set no-store/no-referrer protections.
- `JOURNEY_REPLAY_VIEW` and `JOURNEY_REPLAY_INCIDENT_VIEW` are independently enforced with server-derived Tenant/actor context and safe foreign-Tenant absence.
- Admission is 30 requests/actor/minute and 120/Tenant/minute per instance, with HTTP 429 and `Retry-After: 60`.
- Coordinated backend/frontend flags provide operational rollback without deleting history, permissions, or audit evidence.

## Persistence, architecture and security

V93 is permission-only and remains the current head. No replay table, projection, materialized view, retention change, incident copy, or CS07 index was required. Existing V86/V87 Timescale design and Tenant-leading history index satisfy the accepted plans. V1–V92 remain immutable.

Tracking owns replay orchestration and its incident tables. Trip and Routing are accessed only through published minimized contracts. No cross-module repository, JPA relationship, SQL join, physical foreign key, or leaked persistence type was introduced. Every history, incident, attribution, cursor, admission and audit operation remains Tenant-qualified. Responses, metrics and logs exclude raw telemetry, provider/device secrets, signatures, Driver/Customer PII and persistent cursor material.

## Consolidated verification

All accepted PostgreSQL/Timescale and browser evidence used only `transport_logistics_acceptance`.

| Gate | Result |
| --- | --- |
| Focused CS07 admission/performance | 4/4 PASS |
| Retained replay/Tracking/Trip/Routing/security/PostgreSQL suites | PASS within complete aggregate |
| Architecture/Modulith | 59/59 PASS |
| Complete Maven `clean test` | 1,815/1,815 PASS; 0 failures, 0 errors, 0 skipped; 18:16 |
| PostgreSQL performance | 20 concurrent sessions, 40,000 points; initial p95 158 ms; continuation p95 119 ms |
| Frontend Vitest | 325/325 PASS across 80 files |
| TypeScript / production build / changed-file ESLint | PASS; retained bundle-size advisory only |
| Real PostgreSQL-backed Chromium | 6/6 PASS |
| Checkstyle / PMD / SpotBugs | PASS; zero current findings |
| Dependency analysis | BUILD SUCCESS; established aggregate/transitive advisories retained |
| Docker Compose validation / `git diff --check` | PASS |

## Rollback, residual risk and acceptance boundary

Disable both replay feature flags for operational rollback; immutable history and producer evidence remain unchanged. An application revert requires no schema reversal. Residual risks are physical provider fidelity, production-scale variance beyond the controlled workload, per-instance rather than distributed admission, and external map availability; the evidence timeline remains usable without a tile provider.

No known production technical defect or unresolved product decision remains. Final classification is `TECHNICALLY_COMPLETE / ACCEPTANCE_PENDING`. Accounting stays 73/87. The exact next governed task is:

`US-53-REPLAY-JOURNEYS-FINAL-ACCEPTANCE-001`
