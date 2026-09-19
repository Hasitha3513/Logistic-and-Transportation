# US-51 Monitor Idle Time — CS06 Frontend Evidence

**Status:** COMPLETE

**Flyway head:** V104 (unchanged)

**Accounting:** 73/87 (unchanged)

## Implemented boundary

The Tracking UI now provides permission-aware, read-only views for current idle state, confirmed/closed episode history, episode detail and minimized immutable evidence. `IDLE_MONITOR_VIEW` and `IDLE_EVENT_VIEW` are enforced independently in navigation and screen rendering. Query keys include the authenticated session and all Tracking idle-monitoring cache entries are removed when that session changes.

History uses an explicit UTC range capped at 31 days, a fixed 50-row request size and opaque previous/next cursor stacks. Filter changes reset pagination. The interface distinguishes unsupported, not reported, stale, conflicting, candidate, idle and normal state; presents credited duration separately from timestamps; and always reports Phase-1 fuel estimation as unavailable. It contains no write action and exposes no coordinates, Driver/customer PII, provider payload, device secret or credential reference.

## Verification

- Focused TypeScript compile: PASS.
- Focused ESLint: PASS, zero findings.
- Focused Vitest: 5/5 PASS.
- `VehicleMasterConfig.java`: assume-unchanged flag observed; working-tree bytes equal `HEAD` exactly, so no logical or line-ending change was included.
- Complete Vitest: 87 files, 346/346 tests PASS (pre-existing Ant Design/MSW diagnostics only).
- Production build: PASS; existing large-chunk advisory retained.
- Changed-file ESLint: PASS. Repository-wide ESLint reports 71 pre-existing Delivery-module findings; no CS06 finding.
- Architecture/Modulith: 59/59 PASS, BUILD SUCCESS.
- Real PostgreSQL-backed Chromium: 4/4 PASS against verified `transport_logistics_acceptance`, covering independent permissions, state/history/detail/evidence, 31-day validation, candidate exclusion, privacy/read-only behavior and foreign-Tenant absence.

## Remaining boundaries

Production Fleet eligibility remains `UNKNOWN`; production engine-running mappings and physical acceptance remain pending. CS06 adds no backend contract, migration, permission, dependency, evaluator, Notification, Operations or fuel-estimation behavior.

**Next approved queue:** `CS07 PostgreSQL/Kafka performance/recovery`.
