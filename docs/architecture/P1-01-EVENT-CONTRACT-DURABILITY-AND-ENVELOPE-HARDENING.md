# P1-01 Event Contract Durability and Envelope Hardening

Status: `COMPLETE`
Date: 2026-09-03
Flyway head: V60
Story accounting: unchanged at 65 / 87 complete and 22 / 87 deferred

## Decision summary

The production source contains 32 event classes or persisted event records. Only ten have actual consumers. Eight Delivery events invalidate the in-process ETA cache and remain local after-commit. `OperationalNotificationEvent` remains local after-commit because Notification already owns durable channel execution and attempts. The five-type `DeliveryCustomerNotificationEvent` family is the single durable internal event family because losing the Delivery-to-Notification bridge after a successful business commit would lose an accepted US-69 customer-notification fact.

P1-01 introduces one shared technical outbox and no broker. It does not change REST APIs, RBAC, accepted Delivery semantics, frontend code, or the 65 / 87 story count.

## Factual event inventory and classification

All Spring publishers below use `AfterCommitEventPublisher`. Before P1-01 this meant in-memory publication after commit. After P1-01, only `DurableEventEnvelope` is inserted atomically into the outbox; all other events keep the prior local after-commit behavior. `TripOperationalEvent` is a persisted Trip record rather than a Spring application event.

| # | Event class / type | Owner | Publisher / transaction | Actual consumer | Tenant | Prior state | Retry / idempotency | Sensitivity / external potential | Class |
| :--: | :-- | :-- | :-- | :-- | :--: | :-- | :-- | :-- | :--: |
| 1 | `DeliveryCustomerNotificationEvent` (five US-69 types) | Delivery | Delivery services and batch dispatch; owning transaction | durable bridge -> Notification | Yes | after-commit, not durable | V60 retry; event ID + Notification execution key dedupe | minimized customer/delivery facts; possible future external delivery | D |
| 2 | `DeliveryBatchCreatedEvent` | Delivery | `DeliveryBatchService`; owning transaction | None | Yes | after-commit | none | operational IDs; no current external requirement | A |
| 3 | `DeliveryBatchOrderMembershipEvent` | Delivery | `DeliveryBatchService`; owning transaction | `DeliveryEtaInvalidationListener` | Yes | after-commit | idempotent cache eviction | IDs only; local | C |
| 4 | `DeliveryBatchRiderAssignedEvent` | Delivery | `DeliveryBatchService`; owning transaction | `DeliveryEtaInvalidationListener` | Yes | after-commit | idempotent cache eviction | IDs only; local | C |
| 5 | `DeliveryBatchStatusChangedEvent` | Delivery | `DeliveryBatchService`; owning transaction | `DeliveryEtaInvalidationListener` | Yes | after-commit | idempotent cache eviction | IDs/status; local | C |
| 6 | `DeliveryEtaCalculatedEvent` | Delivery | `DeliveryEtaService`; owning transaction | None | Yes | after-commit | none | ETA facts; no current external requirement | A |
| 7 | `DeliveryOrderDestinationChangedEvent` | Delivery | `DeliveryOrderService`; owning transaction | `DeliveryEtaInvalidationListener` | Yes | after-commit | idempotent cache eviction | IDs only; local | C |
| 8 | `DeliveryRedeliveryScheduledEvent` | Delivery | `RedeliveryService`; owning transaction | None | Yes | after-commit | none | schedule facts; no current external requirement | A |
| 9 | `DeliveryRiderCreatedEvent` | Delivery | `DeliveryRiderService`; owning transaction | None | Yes | after-commit | none | rider ID only; no current external requirement | A |
| 10 | `DeliveryRiderTransportModeChangedEvent` | Delivery | `DeliveryRiderService`; owning transaction | `DeliveryEtaInvalidationListener` | Yes | after-commit | idempotent cache eviction | IDs/mode; local | C |
| 11 | `DeliveryRiderStatusChangedEvent` | Delivery | `DeliveryRiderService`; owning transaction | None | Yes | after-commit | none | IDs/status; no current external requirement | A |
| 12 | `DeliveryRiderAssignedEvent` | Delivery | `DeliveryRiderService`; owning transaction | `DeliveryEtaInvalidationListener` | Yes | after-commit | idempotent cache eviction | IDs only; local | C |
| 13 | `DeliveryRiderReassignedEvent` | Delivery | `DeliveryRiderService`; owning transaction | `DeliveryEtaInvalidationListener` | Yes | after-commit | idempotent cache eviction | IDs only; local | C |
| 14 | `DeliveryRiderUnassignedEvent` | Delivery | `DeliveryRiderService`; owning transaction | `DeliveryEtaInvalidationListener` | Yes | after-commit | idempotent cache eviction | IDs only; local | C |
| 15 | `VehicleReadingRecorded` | Fleet | vehicle-reading publisher; owning transaction | None | Yes | after-commit | none | vehicle reading fact; possible future integration only | A |
| 16 | `VehicleReadingCorrected` | Fleet | vehicle-reading publisher; owning transaction | None | Yes | after-commit | none | vehicle reading fact; possible future integration only | A |
| 17 | `VehicleMeterResetRecorded` | Fleet | vehicle-reading publisher; owning transaction | None | Yes | after-commit | none | meter fact; possible future integration only | A |
| 18 | `FreightOrderCreatedEvent` | Freight | freight event publisher; owning transaction | None | Yes | after-commit | none | freight IDs/facts; possible future integration only | A |
| 19 | `FreightOrderUpdatedEvent` | Freight | freight event publisher; owning transaction | None | Yes | after-commit | none | freight IDs/facts; possible future integration only | A |
| 20 | `LoadPlanCreatedEvent` | Freight | load-plan publisher; owning transaction | None | Yes | after-commit | none | load-plan facts; possible future integration only | A |
| 21 | `LoadPlanUpdatedEvent` | Freight | load-plan publisher; owning transaction | None | Yes | after-commit | none | load-plan facts; possible future integration only | A |
| 22 | `FuelIssueAuthorized` | Fuel | fuel event publisher; owning transaction | None | Yes | after-commit | none | fuel issue facts; possible future integration only | A |
| 23 | `FuelIssued` | Fuel | fuel event publisher; owning transaction | None | Yes | after-commit | none | fuel issue facts; possible future integration only | A |
| 24 | `FuelIssueCancelled` | Fuel | fuel event publisher; owning transaction | None | Yes | after-commit | none | fuel issue facts; possible future integration only | A |
| 25 | `FuelPurchaseApproved` | Fuel | fuel event publisher; owning transaction | None | Yes | after-commit | none | purchase facts; possible future integration only | A |
| 26 | `FuelPurchaseReceived` | Fuel | fuel event publisher; owning transaction | None | Yes | after-commit | none | purchase facts; possible future integration only | A |
| 27 | `FuelPurchaseReconciled` | Fuel | fuel event publisher; owning transaction | None | Yes | after-commit | none | purchase facts; possible future integration only | A |
| 28 | `FuelPurchaseCancelled` | Fuel | fuel event publisher; owning transaction | None | Yes | after-commit | none | purchase facts; possible future integration only | A |
| 29 | `RouteDisruptionCreatedEvent` | Routing | route event publisher; owning transaction | None | Yes | after-commit | none | route facts; possible future integration only | A |
| 30 | `RouteDisruptionResolvedEvent` | Routing | route event publisher; owning transaction | None | Yes | after-commit | none | route facts; possible future integration only | A |
| 31 | `OperationalNotificationEvent` (13 catalogue types) | Notification | Trip/Fleet publishers and durable Delivery bridge; after source commit | `OperationalNotificationEventListener` -> rule engine | Yes | after-commit | rule execution key and channel-attempt persistence | controlled metadata; retained local to avoid new sensitive retention | C |
| 32 | `TripOperationalEvent` persisted record | Trip | `TripOperationalEventService`; owning transaction | None as application event | Yes | database record | command/persistence semantics | incident/delay facts; not an integration contract | A |

Classification totals: A `NO_CONSUMER` 22; B `LOCAL_EPHEMERAL` 0; C `LOCAL_AFTER_COMMIT` 9; D `DURABLE_INTERNAL_REQUIRED` 1; E `EXTERNAL_INTEGRATION_CANDIDATE` 0. Candidate potential alone does not promote an unused event to E.

Modernized families: 2 (`DeliveryCustomerNotificationEvent`, `OperationalNotificationEvent`). Unchanged event classes: 30.

## Canonical envelope

Modernized events expose `eventId`, `eventType`, trusted `tenantId`, `occurredAt`, integer `version`, `aggregateType`, `aggregateId`, and minimized `payload`. Version 1 is the only supported version. Unsupported durable versions are parked as `UNSUPPORTED`; they are never silently processed. Retries retain the original event ID.

Delivery derives Tenant identity from its owning aggregate/current Tenant context. Background polling enumerates active tenants with `TenantJobExecutor`, reconstructs Tenant context for each bounded claim, and clears it through the established executor boundary. Request data is not trusted as Tenant identity.

## Durable outbox and dedupe boundary

The shared technical module owns `integration_outbox_event`. Business modules depend only on `DurableEventPublisher`; they do not import the JPA repository. A required durable event is inserted inside the caller transaction. A business rollback removes the event, while an outbox insert/serialization/size failure fails the business transaction. Provider and consumer processing happens later, outside the source transaction.

The unique key `(tenant_id, event_id, consumer_name)` deduplicates logical producer replay. Notification's existing stable execution key (`eventId + rule + channel + recipient`, Tenant-scoped) is the consumer inbox/dedupe boundary; no second inbox table is justified. The delivery guarantee is at-least-once, never exactly-once. A crash after consumer acceptance and before marking `PUBLISHED` can replay the same event and is handled by that dedupe key.

Claims use Tenant-filtered pessimistic row locks, status transitions, a five-minute lease, and a batch limit of 50. Rows already claimed with an unexpired lease are excluded. Expired claims are reclaimable; expired claims at five attempts are terminalized. There is no global ordering promise. Claim order is `occurredAt, createdAt` within a Tenant, but consumers must not depend on it.

Retry is bounded to five claims with 30, 60, 120, 240, and at most 480 seconds between the attempts used by the five-attempt policy. Invalid JSON, unsupported version, absent handler, and invalid frozen Delivery contract are permanent. Other runtime failures are retried and end as `FAILED` with a sanitized error code. Raw payloads and provider bodies are never logged.

## Privacy, performance, and retention

The durable Delivery payload is an exact event-type allowlist. It excludes raw address, phone/email, authorization data, customer self-service token/hash/magic link/access code, POD evidence, provider credentials, Rider private data, and medical data. Serialization sorts map keys and rejects payloads over 32 KiB. Polling is bounded, indexed by Tenant/status/due time and Tenant/status/lock expiry, and has no in-memory queue or external call inside the source transaction.

Retention policy is conservative: retain `PUBLISHED` rows for at least 30 days and `FAILED`/`UNSUPPORTED` rows for at least 90 days. Purge only terminal rows through an operationally approved, Tenant-qualified maintenance action; P1-01 intentionally introduces no aggressive automatic deletion. Notification execution history remains the durable inbox/audit record under its existing retention governance.

## Accepted behavior preserved

- US-69: READY emits zero out-for-delivery events; committed DISPATCHED emits exactly one per active member; removed members emit zero; rollback emits zero; all five frozen meanings and payload shapes remain unchanged.
- US-70: raw token and self-service link generation stays transient at final provider send and is absent from event/outbox/inbox payloads.
- Notification retains ownership of rules, channels, templates, quiet hours, suppression, execution history, attempts, and provider retry.
- Trip, Fleet, Freight, Fuel, Routing, Organization, REST APIs, permissions, and frontend contracts are unchanged.

## Migration and verification

V60 creates `integration_outbox_event`, its Tenant/event/consumer uniqueness constraint, bounded status/attempt constraints, and partial indexes for ready, stale-claim, terminal-retention, and event-identity access. The shared technical module is the sole owner.

Evidence on Java 21:

- focused event/US-69/US-70 regression: 52 tests, 0 failures/errors/skips;
- PostgreSQL V60/atomicity/Tenant gate: 7 tests, 0 failures/errors/skips against `transport_logistics_acceptance`;
- full Maven verify: 1,250 tests, 0 failures, 0 errors, 15 skips; `BUILD SUCCESS` in 4:23;
- architecture and Spring Modulith: 45 tests, 0 failures/errors/skips;
- Checkstyle, PMD, SpotBugs: PASS;
- frontend impact: NONE;
- `git diff --check`: PASS at final review.

During an earlier discarded verification attempt, generic Spring tests fell back to the development datasource and ran Flyway validation/repair before the process was stopped; no `clean` was observed there. The authoritative successful full run pinned both generic and destructive-test datasource paths to `transport_logistics_acceptance`. This incident is retained here rather than being concealed.

## Deferred architecture

Kafka/RabbitMQ, external publication, webhooks, global ordering, dead-letter administration UI, automated purge scheduling, and modernization of the 22 unconsumed events remain deferred until an approved consumer or operational requirement exists.
