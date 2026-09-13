# Hybrid Telemetry — Kafka Support Architecture

**Status:** APPROVED / IMPLEMENTATION_PENDING  
**Date:** 2026-09-13

## Runtime flow

```text
Signed provider ingress
  -> authenticated provider/Tenant/device normalization
  -> Kafka tracking.telemetry.ingested.v1 (acks=all)
       -> Redis live-state projector
       -> TimescaleDB batch persister
       -> later approved geofence/speed/deviation consumers
```

The controller performs no telemetry database write and returns `202 Accepted` only after Kafka
acknowledges the record. Redis is projected from Kafka rather than written independently by the
controller, preventing ghost live positions and making cache recovery deterministic.

## Kafka contract

- Topic: `tracking.telemetry.ingested.v1`
- Default partitions: 6, configurable
- Key: canonical `{tenantId}:{vehicleId}`
- Producer: idempotence enabled, `acks=all`, LZ4, `linger.ms=20`, batch size 65,536 bytes
- Payload: `eventId`, `tenantId`, `vehicleId`, `deviceId`, trusted `providerAlias`, normalized
  WGS84/quality/meter facts, `recordedAt`, `receivedAt`, dedupe identity and schema version 1
- Headers: `tenantId`, `eventType`, `eventVersion`, and optional correlation ID
- Classification: precise Tenant-owned operational location data; no Driver/Customer PII,
  credentials or raw provider payload
- Ordering: partition order per Tenant/Vehicle only
- Delivery: at-least-once; every consumer deduplicates by Tenant and event identity

Kafka is Tracking infrastructure, not the P1-01 business outbox. Per-ping events do not cross
Spring Modulith boundaries unless a later consumer contract explicitly authorizes that boundary.

## Consumers and failure behavior

The Redis projector writes `tracking:live:{tenantId}:{vehicleId}` with a sliding 24-hour TTL and
uses source time/event identity so replay cannot replace newer state. Redis outage degrades live
reads; Kafka retains the rebuild source.

The Timescale consumer group is `tracking-telemetry-persister-group`, uses manual acknowledgment
and batches up to 500. Offsets commit only after the database transaction. Static reduction may
remove only consecutive coordinate-identical, zero-speed points with unchanged engine state.

All consumers validate agreement among Kafka key, headers and payload Tenant. Mismatch is rejected
to a bounded access-controlled dead-letter path. Tenant Redis scans are prohibited; a bounded
Tenant live index supports enumeration. Telemetry failure never synchronously fails Trip, Billing
or Delivery because those modules do not publish telemetry.
