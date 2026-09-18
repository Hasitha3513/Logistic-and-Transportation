# US-55 Idempotency Closure

Status: **COMPLETE**  
Date: 2026-09-18  
Baseline: `48974cb074f0b78aabf34801c59bbc3854f63ab2`  
Flyway: V100  
Accounting: 73 / 87 COMPLETE

## Traceability matrix

| Operation | Logical identity / key | Durable owner and transaction boundary | Retry behavior | Evidence |
| --- | --- | --- | --- | --- |
| Flespi/Traccar polling publication | Tenant + provider alias + governed provider message identity; deterministic event UUID | Tracking provider ingestion; all required Kafka acknowledgements complete before cursor update | Failed/partial publication throws; watermark remains unchanged and the same identity is replayed | `JdbcTrackingProviderIngestionAdapterTest`, `ProviderPollingRecoveryTest`, `ProviderCoordinatorPostgreSqlAcceptanceTest` |
| Kafka history consumption | `(tenant_id, source_timestamp, id)` and tenant-qualified `dedupe_identity` | Tracking Timescale history transaction, including evaluation-dispatch creation | Consumer acknowledges only after atomic persistence; redelivery converges | `TrackingHistoricalTelemetryPersisterTest`, `KafkaTimescaleHistoricalTelemetryAcceptanceTest`, `JdbcHistoricalTelemetryStoreTimescaleAcceptanceTest` |
| V1/V2 canonical convergence | Same tenant-qualified canonical `dedupe_identity` | `tracking_position_history` uniqueness and history transaction | Either version may arrive first; the other becomes a duplicate | `JdbcHistoricalTelemetryStoreTimescaleAcceptanceTest.v1AndV2ShareTheSameTenantScopedIngestionIdentity` |
| Detector dispatch | Tenant + history + evaluator identity | `tracking_telemetry_evaluation_dispatch`; claim/lease/completion transactions | Duplicate dispatch converges; expired leases recover | `TelemetryEvaluationDispatchConcurrencyPostgreSqlAcceptanceTest` |
| GPS episode/evidence | Tenant/device/type active episode; tenant/episode/evidence hash | Tracking PostgreSQL transaction around episode, immutable evidence and durable events | duplicate evidence inserts nothing and cannot advance counters or republish | `GpsExceptionConcurrencyPostgreSqlAcceptanceTest`, `GpsReliabilityEvaluationServiceTest.duplicateRecoveryObservationDoesNotAdvanceRecoveryCounterTwice` |
| Dispatcher notification / HIGH Operations fact | Durable event ID; opened uses episode ID and HIGH uses deterministic episode-derived UUID | P1-01 durable event log, then idempotent Notification/Operations consumers | redelivery retains one logical downstream effect | `DurableGpsExceptionEventPublisherTest`, `GpsExceptionNotificationBridgeTest`, `GpsExceptionOperationsBridgeTest` |
| Acknowledgement/audit | `(tenant_id, idempotency_key)` plus request fingerprint | V100 acknowledgement command, episode mutation and audit in one transaction | identical replay returns stored response after authorization; conflicting reuse fails; rollback leaves no command/audit | `GpsExceptionServiceTest`, `Us55V99V100PostgreSqlAcceptanceTest` |
| Redis live projection | tenant-qualified Vehicle key with source-time/trust ordering | Disposable Redis atomic projection | duplicates are harmless; older/untrusted observations cannot regress trusted live state | `RedisLiveTelemetryProjectionAdapterIntegrationTest`, `KafkaRedisLiveProjectorIntegrationTest` |

## Closure findings

- No production defect or missing durable state was demonstrated. No migration, API, permission,
  dependency, event contract or product-behavior change is required.
- Added focused proof that a duplicate recovery observation cannot count as a second recovery point.
- Added focused proof that provider replay produces the same canonical identity while a distinct
  provider message produces a distinct identity.
- Tenant scope participates in every durable uniqueness or lookup boundary. Redis is not an
  authoritative idempotency store.
- The delivery guarantee remains at-least-once with idempotent logical effects; this report does
  not claim exactly-once Kafka transport.

## Verification

- Focused Java: **31 tests, 0 failures, 0 errors, 0 skipped**.
- PostgreSQL/Timescale/Kafka/Redis: **34 tests, 0 failures, 0 errors, 0 skipped**.
- Clean Flyway execution reached V100 in isolated databases named
  `transport_logistics_acceptance`.
- The initial sandboxed focused invocation was discarded because Mockito could not attach its
  Byte Buddy agent; the identical host-permitted rerun passed.
- Prior technical-closure gates remain applicable because production code, public contracts,
  frontend and schema were unchanged. The new tests compile and execute against those unchanged
  paths.
- `git diff --check`: PASS.

## Remaining limits

Physical/provider evidence remains unavailable and cannot be replaced by these controlled
fixtures. Future-Tenant Notification provisioning remains deferred. The open acceptance queue is
`US-55-HANDLE-GPS-EDGE-CASES-FINAL-ACCEPTANCE-001`.

The exact next independent implementation queue is `US-55-TECHNICAL-CLOSURE`, which performs the
post-extension full verification defined by the approved telemetry-platform ADR without adding
new behavior.
