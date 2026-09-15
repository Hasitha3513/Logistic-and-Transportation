# US-53 Replay Contract Streaming and Stop Paging Remediation

**Status:** `COMPLETE`

**Date:** 2026-09-15

**Starting HEAD:** `dd32f4649225d14dc80ac9db0b5df7c1b1f478cd`
**Flyway:** V92; V93 unused and reserved for US-53 permissions

## Remediated contract gaps

Every `ReplayPage` now carries its immutable `snapshotRecordedAt` and explicit lower/upper
`ReplayBoundaryEvidence`. Tenant/Vehicle/snapshot-qualified predecessor and successor probes are bounded to one
row and never enter replay items. Reasons distinguish absent, ineligible, qualifying, retention-unavailable,
requested-boundary and unsafe evidence.

`StopReplayQuery` freezes the 100 default/500 maximum stop limits. Immutable `StopPage` carries ordered stops,
its dedicated continuation cursor, snapshot, requested/available range, coverage, missing intervals, bounded
analyzed-point count and the 20,000-point ceiling. Derived ordering is start source timestamp then stop ID.

Point cursors now carry the `POINT` purpose and stop cursors the `STOP` purpose. Both reuse HMAC-SHA256 and the
existing secret. Stop state binds Tenant, selector, requested/effective ranges, snapshot, last stop tuple,
`US53_STOP_V1` and 15-minute expiry. The decoders mutually reject the other purpose.

`ReplayStreamGuard` rejects snapshot changes, repeated/non-advancing cursors, empty nonterminal pages and
non-increasing point tuples. Exactly 20,000 points are permitted; the 20,001st fails with
`REPLAY_POINT_LIMIT_EXCEEDED` and no partial result. Expired point and stop cursors have distinct governed
errors.

## Verification

- Contract/cursor/guard tests: 19/19 PASS.
- PostgreSQL/Timescale boundary, snapshot and compressed-history tests: 12/12 PASS.
- Complete Java 21 `clean test`: 1,794 tests, zero failures/errors/skips; BUILD SUCCESS in 10:32.
- Checkstyle: zero violations.
- Task PMD report: PASS; no task finding. The 32 known unrelated repository-wide findings remain classified
  pre-existing debt.
- SpotBugs: zero findings.
- Dependency analysis: BUILD SUCCESS with existing aggregate dependency advisories.
- Docker Compose validation and `git diff --check`: PASS.

No migration, schema, persistence, public API, permission, stop algorithm, frontend, Kafka, Redis, retention or
compression behavior changed. Rollback is an application revert; immutable history and Flyway remain untouched.

## Continuation

CS03 is ready to implement deterministic stop analysis against these internal contracts.
