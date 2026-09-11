# US-48 Live Vehicle Tracking — Technical Remediation Evidence

**Result:** `COMPLETE`  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**Flyway:** V1→V74 PASS; story migration V73; remediation migration V74  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining

## Implemented authorized remediation

- Replaced caller-controlled Tenant authority with an ACTIVE `tracking_provider_binding` selected by globally unique opaque provider key ID.
- Uses only the published US-73 `IntegrationSecretResolver`; Tracking stores only the opaque credential reference and performs no Integration SQL, repository, exchange, message, or outbox work per packet.
- Enforces provider alias, timestamp, HMAC over the exact frozen canonical raw-body value, binding-scoped database nonce, derived-Tenant device ownership, and source-time Vehicle association. Provider failures return sanitized `401 / TRACKING_PROVIDER_UNAUTHORIZED`; legacy Tenant headers cannot override binding authority.
- V74 creates only `tracking_provider_binding`, `tracking_provider_ingest_nonce`, and `tracking_retention_policy`, with Tenant-consistent constraints and bounded indexes. V1–V73 are unchanged and no V75 exists.
- Implements optional external retention policy, strict-before `TRACKING_POSITION_TOO_OLD`, exact-boundary acceptance, retained policy metadata, and no purge scheduler/API.
- Implements internal transactional deterministic latest-projection rebuild, including empty-history deletion and unchanged-history idempotency.
- Adds safe ingest/result/auth/rate counters, processing/database timers, stale/offline/latest-success health facts, provider credential resolvability health, safe management audit, and no per-packet audit.

## Fresh evidence

- Provider security and freshness/connectivity boundaries: **17/17 PASS**.
- PostgreSQL remediation, indexes, retention, rebuild, audit, append-only history and concurrency: **15/15 PASS**; exact race matrix **9/9 PASS**.
- Clean Flyway migration: **V1→V74 PASS** on `transport_logistics_acceptance`.
- Full Maven: **1,430 tests, 0 failures, 0 errors, 15 skipped — BUILD SUCCESS**, 07:08.
- Architecture: **49/49 PASS**. Checkstyle: zero violations. PMD: BUILD SUCCESS. SpotBugs: zero findings.
- Frontend: TypeScript PASS; Vitest **265/265**; production build PASS; US-48 changed-file ESLint zero.
- Real PostgreSQL Chromium: **10/10 PASS**.
- Signed ingress: **483.5 msg/s sustained** and **1,087.1 msg/s burst**.
- PostgreSQL sampled read p95: latest **2.222 ms**; 24-hour history **0.989 ms**. Provider key, binding nonce, and retention lookups have asserted bounded indexes.
- `git diff --check`: PASS. `DEVELOPMENT_DATABASE_AUTHORITATIVE_EVIDENCE = NO`.

## Containment and remaining dependency

No new human API/permission/event/source type, no US-49–55 feature, no Customer tracking, no Kafka/Redis/PostGIS/TimescaleDB, no second outbox, no per-packet P1-01 event, no Integration table access, no source-history mutation, and no migration beyond V74 were introduced.

Technical closure must be independently rerun. Physical-device and real-provider evidence is still required for final acceptance; US-48 is not story-complete and accounting does not advance.

**Next task:** `US-48-LIVE-VEHICLE-TRACKING-TECHNICAL-CLOSURE-001-RERUN`
