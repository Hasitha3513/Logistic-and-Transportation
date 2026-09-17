# US-55 GPS Edge Cases — CS07 Frontend Closure

## Verdict

`PASS — IMPLEMENTATION_IN_PROGRESS / CS07_COMPLETE`

The Tracking workspace now provides a permission-gated GPS exception episode list, minimized detail and immutable
evidence, and controlled operator acknowledgement. This change does not claim physical GPS/device acceptance and
does not alter the V100 backend contract.

## Implemented contract

- Navigation and route: `/tracking/gps-exceptions`, visible only with `GPS_EXCEPTION_VIEW`.
- Required explicit `[from,to)` range, maximum seven days; default page size 100; opaque cursor navigation without
  fabricated totals or page jumps.
- Status, type, severity, Vehicle and device filters stay in component memory and are not persisted in URLs or
  browser storage. TanStack Query cancellation signals prevent obsolete responses from replacing current data.
- List, detail and evidence calls require VIEW. `GPS_EXCEPTION_REVIEW` alone issues no VIEW request and reveals no
  episode. Query keys are session-qualified and GPS-exception cache/state is cleared by remount on actor change.
- Detail and evidence expose only CS06 response fields. No coordinates, provider payload, credential, signature,
  external device reference or diagnostic text is rendered.
- Acknowledgement trims and validates reason length 1–500, submits current `expectedVersion`, prevents concurrent
  double submission, and owns an idempotency key for the command. An uncertain network retry preserves the same
  key/version/reason; a conflict refreshes state and is never silently resubmitted.
- Acknowledgement is explicitly review evidence. Recovery, resolution, severity and evidence remain detector-owned.
- Modal focus moves to the reason field when opened; labelled controls, keyboard activation and text status accompany
  colour presentation.

## Acceptance evidence

| Gate | Result |
| --- | --- |
| Focused frontend | 6/6 PASS |
| Complete Vitest | 336/336 PASS across 85 files |
| TypeScript | PASS |
| Production build | PASS |
| Changed-file ESLint | PASS, zero findings |
| Real PostgreSQL-backed Chromium | 6/6 PASS against `transport_logistics_acceptance` |
| Architecture / Modulith | 59/59 PASS |
| Complete Maven | 1,890 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS |
| Checkstyle | 0 violations |
| PMD | BUILD SUCCESS |
| SpotBugs | BUILD SUCCESS, zero reported findings |
| Dependency analysis | BUILD SUCCESS; existing global declaration warnings retained |
| Docker Compose validation | PASS |
| `git diff --check` | PASS |

The first sandboxed Maven attempt could not initialize Mockito because Byte Buddy self-attach was prohibited. An
unrestricted attempt then correctly exposed an omitted local `POSTGRES_PORT=5433` setting. The accepted complete
run used Java 21 and the isolated acceptance PostgreSQL endpoint on port 5433. Neither failed invocation constitutes
product evidence; the accepted terminal summary is the clean 1,890-test result above.

Passing screenshot: [GPS exception detail](../evidence/us55-cs07-gps-exception-detail.png).

## Security and data disposition

The real suite proves VIEW-only usability, REVIEW-only denial without evidence calls, foreign-Tenant safe 404,
authorized acknowledgement, keyboard focus and read-only acknowledged state. External notification delivery was
not enabled. Acceptance fixtures are guarded by `PGDATABASE=transport_logistics_acceptance`; the development
database was not used.

## Change boundary

No backend API, event, permission, migration, Kafka, Redis, TimescaleDB, Notification or Operations behavior changed.
Flyway remains V100 and MVP accounting remains 73/87.

## Next governed queue

`US-55-HANDLE-GPS-EDGE-CASES-CS08-POSTGRES-KAFKA-REDIS-CONCURRENCY-PERFORMANCE-001`
