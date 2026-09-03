# ADR: Shared Database Outbox for Required Internal Events

Status: Accepted
Date: 2026-09-03
Decision owner: Platform architecture (`shared` technical module)

## Context

Most repository events either have no consumer or drive idempotent in-process cache invalidation. The accepted US-69 Delivery customer-notification facts cross from Delivery into Notification and must not be lost in the process-crash window after the Delivery transaction commits. Spring's in-memory after-commit callback isolates failures but is not durable.

## Decision

Use one PostgreSQL outbox table, `integration_outbox_event`, owned by the shared technical module. Only contracts implementing `DurableEventEnvelope` use it. The first approved route is `DeliveryCustomerNotificationEvent` to `delivery-customer-notification-bridge`.

The source transaction writes business state and the outbox row atomically. A Tenant-scoped scheduled worker claims at most 50 due rows using pessimistic database locks and a five-minute lease, invokes the registered handler outside the source transaction, and records publish, retry, unsupported, or failed state in short new transactions. Delivery is at-least-once. Stable event IDs and the existing Notification execution key provide producer and consumer dedupe.

## Why PostgreSQL and not a broker

The modular monolith already has one PostgreSQL transaction boundary and no approved external broker requirement. A database outbox closes the proven crash window without distributed transactions, a second operational platform, or speculative external contracts. A broker adapter can later consume the same canonical envelope after explicit approval.

## Ownership and security

Business modules publish through the shared `DurableEventPublisher` port and never access outbox persistence. Every current durable row is Tenant-owned. Payloads are explicit, deterministic, limited to 32 KiB, and must not contain tokens, magic links, access codes, credentials, POD evidence, or private/medical data. Logs contain routing and diagnostic facts only.

## Consequences

- Required Delivery events survive process failure after commit.
- A source transaction fails if its required outbox insert fails.
- Duplicate delivery is possible and consumers must remain idempotent.
- No global ordering is guaranteed.
- Five bounded attempts and terminal states prevent infinite retry.
- Published rows are retained for at least 30 days and failed/unsupported rows for at least 90 days; automatic purge is deferred.
- Unconsumed and cache-only events are not gratuitously rewritten.
