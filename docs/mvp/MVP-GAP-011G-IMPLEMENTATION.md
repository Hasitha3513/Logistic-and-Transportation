# MVP-GAP-011G Implementation Evidence

## Outcome

MVP-GAP-011G is complete. US-71 remains PARTIAL pending MVP-GAP-011H offline-specific Playwright acceptance and MVP-GAP-011I closure.

This slice changed frontend UX and its tests only. It did not change backend production code, backend tests, Flyway, the frozen four-operation contract, or IndexedDB v1.

## Implemented behavior

- Header indicator derives Online/Offline, backend reachability, synchronization, and authentication-pause state from the existing coordinator.
- Owner-scoped drawer shows Pending, Syncing, Conflict, and Failed counts plus safe operation summaries and sanitized code/message details.
- Manual Sync invokes the existing coordinator only and is disabled without an owner, while offline, during auth pause/current synchronization, or when no pending item exists.
- The feature boundary owns retry and discard; components never access IndexedDB directly.
- Retry is allowed only for retryable FAILED outcomes and preserves operation ID, idempotency key, payload, owner, createdAt, lastAttemptAt, and cumulative attemptCount.
- Forbidden, conflict, idempotency-mismatch, and invalid-payload outcomes do not offer blind retry.
- Open routes to Trip details or the existing Vehicle detail drawer deep link.
- Refresh invalidates the complete owning aggregate query set: Vehicle readings/latest/mileage/list/detail or Trip events/history/detail/list.
- Discard is available only for FAILED/CONFLICT, requires an Ant Design confirmation that the item was not synchronized, removes only the owner-scoped local copy, and does not mutate server data.
- Vehicle local reading rows and Trip local timeline items reuse the centralized status and action policy.
- IndexedDB changes remain reactive through the existing storage callback/revision mechanism; query keys include the authenticated owner to prevent cross-user leakage.

## Action matrix

| State | Actions |
|---|---|
| PENDING | Global Sync now when eligible; no terminal per-record action |
| SYNCING | Progress only; no duplicate action |
| FAILED | Open, Refresh, Discard, and Retry only for retryable codes |
| CONFLICT | Open, Refresh, Discard; no blind Retry |
| SYNCED | Retained by existing policy but omitted from actionable views |

## Verification

| Gate | Result |
|---|---|
| Frontend lint | PASS, zero warnings |
| Frontend unit/component | PASS, 169/169 |
| Frontend production build | PASS |
| Backend `mvn clean verify` | PASS, 680 run / 658 passed / 22 Docker-conditional skipped |
| Spring Modulith/architecture | PASS, 16/16 checks |
| Spring context / JPA | PASS, 45 repositories |
| Flyway | PASS, V1-V29 |
| Retained Playwright | PASS, 156/156 across Chromium, Firefox, WebKit |

The existing build-size advisory and pre-existing Ant Design deprecation/MSW test diagnostics remain non-failing and are outside this slice.

## Database and scope controls

- New Flyway migration: NONE
- Current Flyway head: V29
- IndexedDB schema/version: v1, unchanged
- Backend production/test files changed by 011G: NONE
- New offline Playwright cases: NONE (reserved for 011H)
- Commit/push: NONE
