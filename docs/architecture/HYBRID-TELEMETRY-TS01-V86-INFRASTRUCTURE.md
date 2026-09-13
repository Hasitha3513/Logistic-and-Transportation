# Hybrid Telemetry TS01 — V86 Infrastructure Evidence

**Status:** IMPLEMENTED / VERIFIED  
**Date:** 2026-09-13

## Delivered

- V86 creates the Tenant-qualified `tracking_position_history` table and converts it into a
  one-day TimescaleDB hypertable in production.
- Flyway placeholders isolate the real extension operation from ordinary schema-clean regression
  baselines. A dedicated acceptance test executes the production SQL against the real TimescaleDB
  PostgreSQL 16 image.
- Local Compose uses TimescaleDB PostgreSQL 16 and Redis 7.4 with AOF and `noeviction`.
- The backend has Spring Data Redis plus disabled-by-default hybrid settings; Compose explicitly
  enables the promoted runtime.
- All current-head PostgreSQL assertions advance from V85 to V86.
- Flespi, Traccar and Generic provider-neutral normalizers and their fail-fast registry are present;
  secure ingress integration remains the next slice.

## Verification

- Maven compile: PASS.
- Compose model validation: PASS.
- Existing provider-connection PostgreSQL acceptance: 9/9 PASS at V86.
- Real TimescaleDB migration acceptance: 1/1 PASS; extension and hypertable verified.
- Normalizer unit tests: 4/4 PASS.
- `git diff --check`: PASS.

## Safety

No development database was contacted. Testcontainers used isolated disposable databases. No
existing migration was modified. No public endpoint or frontend behavior is active in this slice.

## Superseding stream decision

The later high-throughput amendment retains Redis as live state but supersedes the planned Redis
Stream buffer with Kafka topic `tracking.telemetry.ingested.v1`. V86 remains immutable. Kafka
producer/consumer code and V87 Timescale policy hardening are outside TS01.
