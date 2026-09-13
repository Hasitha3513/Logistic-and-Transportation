# Hybrid Telemetry TS02 — Kafka Contract and Secure Ingress

**Status:** COMPLETE

**Date:** 2026-09-13

**Flyway head:** V86 (no TS02 migration)

## Implemented boundary

TS02 reuses TS01's provider registry, Flespi/Traccar/Generic normalizers, V86 Timescale foundation,
and Redis/Kafka deployment topology. The existing signed provider-key ingress remains the only
entry point. It verifies HMAC signature, timestamp freshness, nonce replay protection, active
provider binding and bounded request/batch size before normalization.

Tenant identity, provider type, Device identity and source-time Vehicle association are resolved
from same-Tenant server-side Tracking records. Tenant or Vehicle claims supplied by a provider
payload cannot override that authority. Unsupported providers, versions, devices and associations
fail safely without exposing whether a foreign-Tenant resource exists.

## Canonical Kafka record

- Topic: `tracking.telemetry.ingested.v1`
- Default partitions: 6 (configurable)
- Key: `{tenantId}:{vehicleId}`
- Value: governed version-1 `TrackingTelemetryIngestedV1` envelope
- Headers: Tenant ID, event type and event version
- Producer: `acks=all`, idempotence enabled, LZ4, `linger.ms=20`, batch size 65,536 bytes
- Delivery: at least once with Tenant/Vehicle partition ordering

The canonical envelope contains only normalized Tracking facts and trusted identifiers. Provider
credentials, raw signatures and raw provider payloads are excluded. `recordedAt` preserves source
time and `receivedAt` is captured separately. Event and deduplication identities are deterministic,
so HTTP replay protection, provider-message idempotency, Kafka producer idempotence and future
US-48 consumer deduplication remain distinct layers.

## Acknowledgement and failure semantics

Ingress returns `202 Accepted` only after Kafka returns broker partition/offset metadata within the
configured acknowledgement timeout. Broker failure or timeout returns a safe `503`; invalid trust,
signature, replay, version, payload or size fails closed with the existing bounded API errors. The
controller writes neither Redis nor TimescaleDB. Kafka therefore remains the sole durable handoff,
avoiding live-state ghosts and dual-write divergence.

Actuator health exposes Kafka availability without credentials or payload content. Producer metrics
and standard Kafka client logs provide acknowledgement/failure visibility; response metadata is
limited to safe event, Vehicle, dedupe, partition and offset facts.

## Runtime and CI

`compose.yml` adds pinned `apache/kafka:3.7.2` in KRaft mode, a health check, bounded heap and no host
port. Spring Kafka uses client 3.6.2 through Spring Boot dependency management. Testcontainers Kafka
2.0.3 validates a real six-partition broker; test-scoped Commons Lang 3.17.0 is required by its
Commons Compress 1.28.0 runtime.

## Verification evidence

- Secure ingress, normalizers and publisher focused tests: PASS
- Real Kafka publish/consume acknowledgement test: PASS
- Tracking/US-48/US-49/US-50/US-52 regression: 243 tests, 0 failures, 0 errors, 0 skipped
- Architecture and Modulith suite: 58 tests, 0 failures, 0 errors, 0 skipped
- Complete `./mvnw -B clean test`: 1,695 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS (09:12)
- Focused post-static-analysis regression: 30 tests, 0 failures, 0 errors, 0 skipped
- PMD: PASS; SpotBugs: 0 findings
- `docker compose config -q`: PASS
- `git diff --check`: PASS
- Flyway V1→V86 within regression tests: PASS

There is no frontend change, public management UI, database migration, Redis projection or Timescale
consumer in TS02. MVP accounting remains 73/87.

## Rollback and remaining risk

Rollback removes the TS02 producer/configuration and restores the prior ingress wiring; V86 is not
rolled back. Deployments must provision the topic with the governed partition/retention policy and
must keep Kafka healthy because ingress deliberately fails closed when durable handoff is unavailable.
Consumer lag, live projection and historical persistence become operational gates in TS03 and TS04.

Next queue: `HYBRID-TELEMETRY-TS03-KAFKA-REDIS-LIVE-PROJECTOR`.
