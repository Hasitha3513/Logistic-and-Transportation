# US-53 Replay Journeys CS05 Frontend — Technical Evidence

## Verdict

`PASS — IMPLEMENTATION_IN_PROGRESS / CS05_COMPLETE`

CS05 adds the permission-gated `Tracking → Journey Replay` operator experience without changing backend,
database, event, producer or acceptance semantics. Story accounting remains 73/87 and Flyway remains V93.

## Delivered contract

- Feature-local API, TanStack Query hooks, types and Zod/RHF selection/range validation.
- Vehicle-or-Trip POST-body selection with no selector, time range or cursor in the URL or persistent storage.
- Source-time map/timeline synchronization, paused initial state, seek, restart and exact `0.5x`, `1x`, `2x`,
  `4x`, `8x` playback controls with live-region status.
- Explicit path breaks and warnings for gaps, trust, quality, truncation and partial retention.
- Accessible stop markers/list and coordinates hidden until explicit operator expansion.
- Responsive map-first tablet/phone layout; timeline remains usable without hover.
- Incident capability is shown only with the second permission and remains truthfully unavailable pending CS06.
- Permission-gated navigation and deep-link guard use `JOURNEY_REPLAY_VIEW`; backend remains authoritative.

## Verification

| Gate | Result |
| --- | --- |
| Focused Vitest | 5/5 PASS |
| Complete Vitest | 324/324 PASS |
| TypeScript | PASS |
| Production build | PASS; existing bundle-size advisory only |
| Changed-file ESLint | PASS, zero warnings |
| Real PostgreSQL/Timescale Chromium | 5/5 PASS against `transport_logistics_acceptance` |
| Architecture/Modulith | 59/59 PASS |
| Complete Maven | 1,807 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS (11:24) |
| Checkstyle | PASS, 0 violations; warnings are baseline audit output |
| PMD | PASS after an isolated wildcard-import remediation to the preceding replay API commit |
| SpotBugs | PASS |
| Dependency analysis | PASS with the repository's existing advisory list |
| Docker Compose validation | PASS |
| `git diff --check` | PASS |

The first Maven invocation was invalidated because the sandbox prevented Mockito/Byte Buddy self-attachment.
The next invocation proved attachment but used the default unavailable PostgreSQL port 5432. The authoritative
complete rerun used Java 21 and only `transport_logistics_acceptance` on port 5433.

## Real-browser evidence

The isolated fixture inserts eight Tenant-owned canonical history rows in the acceptance Timescale hypertable.
Chromium proves permission-gated navigation, selector privacy, eight chronological points, a deterministic
five-minute/six-point stop, an explicit time gap, paused/play/pause/speed behavior, phone-width usability,
explicit coordinate expansion, and literal `/api/v1/.../points/query` denial for an unauthorized actor.

## Residual scope and next queue

No frontend stores cursors persistently and no map-provider request is introduced. The local evidence map has
no tile dependency; timeline, stop and quality evidence remain independently usable. CS05 does not claim
producer incident adapters or physical GPS acceptance.

Exact next queue: `US-53-REPLAY-JOURNEYS-CS06-INCIDENT-OVERLAYS-001`.
