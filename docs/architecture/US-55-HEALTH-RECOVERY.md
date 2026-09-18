# US-55 Provider Health and Recovery

## Verdict

`COMPLETE` at Flyway V100. This change set preserves the separate
`IMPLEMENTATION_COMPLETE_ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM` hold for genuine provider/device
evidence and leaves MVP accounting at 73/87.

## Implemented behavior

- Flespi and Traccar polling failures cross the provider boundary as a bounded, privacy-safe
  operational category. Provider payloads, credentials, endpoints and exception text remain absent.
- The polling coordinator persists that category with the failed lease release. It does not advance
  the successful watermark or report a successful poll.
- Overflow, authentication, endpoint-policy, rejection, unavailability, transient, rate-limit and
  canonical Kafka publication failures have distinct recovery guidance in the provider UI.
- An empty successful provider response records provider reachability and the completed poll only.
  It does not manufacture telemetry, update `lastProviderMessageAt`, resolve a GPS-exception episode
  or imply that live tracking recovered.
- Required Kafka acknowledgements remain the durable polling boundary. Failed or partial
  acknowledgement leaves the watermark unchanged; deterministic canonical identities make retry
  duplicate-safe.
- Database leases plus the process-local flight guard retain bounded, non-overlapping polling.
  Restart recovery reclaims expired leases without stealing active work.
- Generic signed-HMAC ingress is unchanged.

## Recovery matrix

| Condition | Durable outcome | Displayed truth |
| --- | --- | --- |
| Provider outage/rate limit | failed lease, bounded backoff, watermark retained | provider unavailable/rate limited |
| Invalid credential | failed lease, watermark retained | safe credential-reference action |
| Endpoint policy rejection | failed lease, watermark retained | deployment-admin allowlist guidance |
| Oversized response | whole result rejected, no truncation or progress | governed-limit action |
| Kafka unavailable/ack timeout | no successful progress acknowledgement | canonical publication recovery action |
| Empty response | successful poll, no provider-message timestamp | reachable but no telemetry received |
| Duplicate/out-of-order retry | canonical deduplication and source-time guards | no false freshness or duplicate business fact |

## Verification evidence

All provider responses and failures used by automated tests are controlled fixtures; they are not
physical-device or genuine-provider acceptance evidence.

- Focused provider/polling/PostgreSQL: 36 tests, 0 failures, 0 errors.
- Provider health UI: 15 tests, 0 failures.
- Checkstyle: 0 violations.
- PMD: pass.
- SpotBugs: 0 findings.
- Complete backend regression: 1,906 tests total in isolated phases (1,852 non-PostgreSQL and 54
  PostgreSQL-tagged), 0 failures, 0 errors and 0 skipped. The separation prevents destructive
  migration tests from rebuilding a schema underneath cached scheduled contexts; all tests run.
- Architecture/Modulith: 59 tests, 0 failures.
- Complete frontend Vitest: 341 tests, 0 failures; TypeScript and production build pass.
- Changed-file ESLint: pass.
- Real PostgreSQL-backed Chromium provider continuity: 10/10 pass.
- Dependency analysis and Docker Compose validation: pass; dependency analysis retains the
  repository's pre-existing declaration warnings without adding a dependency.

## Security and boundaries

Tenant-qualified connection and device bindings remain mandatory. The UI consumes only persisted
safe categories and exposes no raw provider response, credential, signature or precise position.
No migration, permission, public API, dependency, notification policy or canonical event contract
changed. Future-Tenant Notification provisioning remains deferred.

## Limitations and rollback

Genuine Flespi/Traccar device outage and recovery still require the open US-55 physical acceptance
journey. Traccar production polling is implemented for the governed supported version, while its
real-provider capture remains pending. Rollback may revert this change set without database action;
the previous behavior collapses provider categories to `PROVIDER_FAILURE` but retains watermarks.

## Next queue

The exact next independent queue is `US-55-IDEMPOTENCY-CLOSURE`. The open acceptance queue remains
`US-55-HANDLE-GPS-EDGE-CASES-FINAL-ACCEPTANCE-001`.
