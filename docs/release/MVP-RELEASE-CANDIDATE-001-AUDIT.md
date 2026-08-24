# MVP Release Candidate 001 — Production Readiness Audit

**Task:** MVP-RELEASE-CANDIDATE-001  
**Audit date:** August 23, 2026  
**Audited commit:** `0973bce0d7dd1ed76bf7ba521211107b193d64d9`  
**Functional baseline:** 39/39 stories complete  
**Final decision:** **NOT_READY_FOR_RC**  
**Production status:** **NOT YET DECLARED PRODUCTION READY**

## 1. Executive Summary

The functional MVP remains complete and its backend, architecture, frontend, and packaging gates are green. The current code has strong domain validation, explicit business permissions, transactional mutation paths, audit histories, JWT/rotating hashed refresh tokens, Flyway-controlled schema, a working PostgreSQL profile, production-built frontend assets, notification delivery controls, and durable offline synchronization.

The current commit is not ready to be promoted to a release candidate. Five P1 release-candidate blockers remain:

1. no fail-safe production profile: the default profile starts H2, so missing deployment configuration can create an in-memory system;
2. 22 PostgreSQL-specific tests did not run because Docker is unavailable;
3. no CI/CD pipeline enforces the release gates or produces a promotable artifact;
4. verification executed on Java 26.0.1 rather than the supported Java 21 runtime;
5. the full Playwright gate failed logout/session termination in Firefox and WebKit; an isolated rerun passed Firefox but reproduced WebKit.

Production approval has additional blockers: dependency-aware readiness, monitoring/alerts, backup/restore/rollback/DR, secrets delivery, authentication abuse protection, a browser-token risk decision, bounded APIs/reporting, data retention/privacy policy, hardened containers/ingress, staging SMTP proof, and performance/capacity evidence.

No P0 was found, no committed real production secret was identified, no functional scope was expanded, and no production code, migration, or dependency was changed by this audit.

## 2. Current Functional and Technology Baseline

| Item | Actual current evidence |
|---|---|
| MVP | 39 complete, 0 partial, 0 not implemented; 100.00% functional completion |
| Backend | Spring Boot 3.2.12, Spring Modulith 1.2.12, Java release target 21, Maven, 738 production source files compiled |
| Data | PostgreSQL runtime, H2 runtime/default test profile, Flyway V1-V29, JPA/Hibernate, 45 repositories |
| Frontend | React 19.1.1, TypeScript 5.8.3, Vite 7.x lock resolution, Ant Design 5.x, TanStack Query 5.x |
| Browser testing | Playwright 1.62.x declaration; Chromium, Firefox, WebKit projects; retries disabled |
| Deployment assets | Backend and frontend multi-stage Dockerfiles, local Compose stack, Nginx SPA/API proxy |
| Redis | Not present and not required by current code |

The repository version is still `1.0.0-SNAPSHOT`; no release tag, immutable release artifact, SBOM, or provenance record exists.

## 3. Audit Method

The audit read the authoritative MVP/US-71/QA documents, `AGENTS.md`, all application profiles, Maven/npm metadata, Docker/Compose/Nginx assets, security and error infrastructure, Flyway migrations, persistence queries, reporting, notification and offline-sync code, frontend auth/routing, and available release documentation. It searched tracked files for credentials/private-key patterns without copying values, inspected CI/runbook/retention/metrics evidence, and executed the prescribed non-destructive gates.

## 4. Build and Regression Results

| Gate | Actual result |
|---|---|
| `.\mvnw.cmd -B clean test` | PASS — 681 run, 659 passed, 0 failures, 0 errors, 22 skipped; 2:53 |
| `.\mvnw.cmd -B verify` | PASS — same 681/659/22 result; executable JAR packaged; 2:43 |
| Architecture-only Maven gate | PASS — 16/16, no failures/errors/skips |
| Spring context | PASS — `ContextSmokeTest`; Security/Fleet/Trip/Notification/Offline Sync initialized |
| JPA | PASS — 45 repositories discovered; `ddl-auto=validate` |
| Flyway/H2 | PASS — clean V1-V29 application and validation |
| `npm ci` | PASS — 410 packages installed from lockfile |
| `npm run lint` | PASS — zero warnings allowed |
| `npm test` | PASS — 33 files, 170/170 tests |
| `npm run build` | PASS — 5,109 modules; 1,876.05 KB JS, 566.13 KB gzip; chunk advisory |
| `npm audit --omit=dev` | PASS — 0 known vulnerabilities |
| `npm audit` | PASS — 0 known vulnerabilities across production/dev dependencies |
| `npm run test:e2e` | FAIL — 199/201 passed in 9:00; `E2E-AUTH-003` logout redirect failed in Firefox and WebKit |
| Isolated `E2E-AUTH-003` rerun | FAIL — Firefox passed; WebKit reproduced the redirect failure; 1/2 passed |

The 22 conditional skips are 7 bunker concurrency tests, 14 PostgreSQL production-invariant tests, and 1 V29 Offline Sync invariant test. They are environmental, not reported as functional failures, but the missing PostgreSQL release evidence is an RC blocker. The browser failure is not treated as an environmental skip: the logout menu action left the authenticated dashboard at `/` instead of navigating to `/login`. WebKit reproduced this in isolation; Firefox's isolated pass shows browser/timing sensitivity but does not clear the failed full gate.

## 5. Architecture

| Suite | Result |
|---|---:|
| `ApplicationModulesTest` | 2/2 PASS |
| `HexagonalLayerArchitectureTest` | 7/7 PASS |
| `ModuleBoundaryArchitectureTest` | 4/4 PASS |
| `LombokUsageArchitectureTest` | 3/3 PASS |
| **Total** | **16/16 PASS** |

No architecture rule was weakened. Offline Sync continues to use public Fleet/Trip/Identity boundaries and does not depend on Notification internals.

## 6. RC Readiness Matrix

| Area | Current State | Evidence | Risk | Severity | RC Blocker? | Production Blocker? | Recommended Action | Proposed Task ID |
|---|---|---|---|---|---|---|---|---|
| Production configuration | MISSING | Default H2; PostgreSQL profile but no fail-safe production profile | Accidental in-memory/data-loss deployment | P1 | YES | YES | Explicit validated production profile | RC-HARDEN-001 |
| Database | PARTIAL | V1-V29/H2 pass; PostgreSQL config exists; 22 PG tests skipped | PostgreSQL constraints/concurrency unproven now | P1 | YES | YES | Execute PG16 release gate | RC-HARDEN-002 |
| Security | PARTIAL | Strong JWT/RBAC/refresh/BCrypt; missing abuse controls/token decision | Brute force/XSS token theft/exposure policy | P1 | NO | YES | Harden perimeter | RC-HARDEN-006 |
| Secrets | PARTIAL | Env injection/templates; no committed real secret; no secret-store contract | Operational secret delivery/rotation unproven | P1 | NO | YES | Production configuration and supply-chain controls | RC-HARDEN-001/012 |
| API | PARTIAL | Authorization/validation/transactions good; several unbounded lists/ranges | Memory, latency, scan, abuse | P1 | NO | YES | Bounded pagination/ranges | RC-HARDEN-007 |
| Frontend | PARTIAL | Lint/unit/build green; permission-aware UI; Playwright 199/201 | Cross-browser logout regression, LocalStorage tokens, no error boundary, large chunk | P1 | YES | YES | Restore deterministic logout, then address security/resilience | RC-HARDEN-016/006/011 |
| Offline Sync | PARTIAL | Functional contract complete; queue bounded locally | Server inbox growth/monitoring/deployment compatibility | P1 | NO | YES | Retention/metrics/load rehearsal | RC-HARDEN-004/008/013 |
| Notifications | PARTIAL | SMTP TLS/auth validation, timeout/retry/audit | No staging provider/backlog alert/retention proof | P1 | NO | YES if EMAIL launch scope | Staging SMTP gate | RC-HARDEN-010 |
| Observability | MISSING | Default Actuator only; no custom metrics/log schema/alerts | Failures and backlogs can be invisible | P1 | NO | YES | Health/metrics/logs/alerts | RC-HARDEN-004 |
| Backup/restore | MISSING | No approved policy or tested restore | Irrecoverable data loss/long outage | P1 | NO | YES | Prove backup/restore | RC-HARDEN-005 |
| Disaster recovery | MISSING | No RPO/RTO/outage/bad-release procedures | Uncontrolled recovery | P1 | NO | YES | DR/rollback runbooks | RC-HARDEN-005 |
| Deployment | PARTIAL | Docker/Compose/Nginx exist | Assets are local/development-oriented | P1 | NO | YES | Production manifests and promotion process | RC-HARDEN-001/009 |
| Containers | PARTIAL | Multi-stage builds and health checks | Root runtime, mutable tags, frontend context, static health | P1 | NO | YES | Container hardening | RC-HARDEN-009 |
| CI/CD | MISSING | No pipeline | Gates/artifact are not reproducible/enforced | P1 | YES | YES | Java 21 protected pipeline | RC-HARDEN-003 |
| Dependencies | PARTIAL | npm audit 0; Maven BOM/lockfile | No Maven/image/SBOM/provenance scan | P1 | NO | YES | Supply-chain gates | RC-HARDEN-012 |
| Performance | MISSING | No production load/query-plan evidence | Unknown capacity/latency/lock behavior | P1 | NO | YES | Capacity and load program | RC-HARDEN-007/013 |
| Artillery | MISSING | No harness/scenarios/threshold decisions | Critical flows untested under load | P1 | NO | YES | Approved staging load suite | RC-HARDEN-013 |
| Data retention | MISSING | Local synced queue has 7-day purge; server durable data has none | Unbounded growth/compliance ambiguity | P1 | NO | YES | Retention policy and cleanup | RC-HARDEN-008 |
| Privacy | PARTIAL | Permission-protected sensitive endpoints; no credentials in IndexedDB | Policy/encryption/minimization gaps | P1 | NO | YES | Privacy control review | RC-HARDEN-014 |
| Runbooks | MISSING | Older checklists only; no operational procedures | Slow/unsafe incident handling | P1 | NO | YES | Current runbooks/checklist | RC-HARDEN-005/015 |
| Redis | NOT APPLICABLE | No dependency/config/use found | None for current architecture | P3 | NO | NO | Do not introduce Redis without a proven need | — |

## 7. Production Configuration Findings

- `application.yml` defaults to `h2`; production safety depends entirely on an operator selecting `postgres` and supplying variables.
- `application-postgres.yml` requires DB URL/user/password and JWT secret, sets UTC Hibernate time, and bounds Hikari min/max. It does not form a complete production profile and does not define connection/validation/leak/statement timeout policy.
- `LocalIdentityBootstrap` and `LocalSampleDataBootstrap` include the `postgres` profile and are disabled only by properties. Compose explicitly turns both on.
- `application-e2e.yml` and both E2E controller/configuration families are correctly profile-scoped. No E2E bean activates without `e2e`; endpoint security still requires JWT and authority.
- Swagger/OpenAPI remains public in all profiles. Actuator health/info/metrics/modulith are exposed on the web path but require `IDENTITY_MANAGE`.
- No explicit production logging, CORS, server-forward-header, graceful-shutdown, request-size, or management-port policy exists.
- Vite defaults to same-origin `/api`; development alone proxies `localhost:8080`. The production Nginx image proxies `/api` to the backend service.

## 8. Database Readiness

Ready elements include forward Flyway migrations, `ddl-auto=validate`, H2 PostgreSQL mode, PostgreSQL UUID/timestamp/index SQL, foreign keys/checks/unique constraints, pessimistic locks on critical mutation paths, and PostgreSQL-specific invariant tests in source.

Gaps:

- no current executable PostgreSQL evidence because Docker is unavailable;
- pool sizing exists but is not derived from capacity and lacks explicit timeout/lifetime/leak policy;
- many master/list/report queries are unbounded;
- reporting/availability can load full Fleet sets and risks N+1/full scans;
- no retention/partition/archive plan for histories, readings, notification tables, refresh tokens, or the V29 inbox;
- no production-like `EXPLAIN ANALYZE`, vacuum/autovacuum, storage-growth, or long-migration rehearsal.

No V30 is created by this audit. A future forward migration may be required only after retention/query-plan work proves an index or schema need; V1-V29 remain immutable.

## 9. Redis

No Redis client, Spring Redis dependency, cache annotation, configuration, or Compose service exists. The current MVP does not require Redis. Introducing it solely because an earlier design mentioned it would add unsupported operational complexity.

## 10. Security Hardening

### Ready

- HS256 JWT requires a secret of at least 32 bytes, validates algorithm, signature, issuer, subject, and expiry; access TTL is 15 minutes.
- Every authenticated request reloads the active user and current roles/permissions, so user disablement and permission changes take effect immediately rather than trusting token authorities.
- Refresh tokens are 256-bit random values, stored as SHA-256 hashes, pessimistically locked, rotated, expired, and revoked on logout; TTL is 30 days.
- BCrypt strength is 12; disabled users cannot authenticate; password hashes do not enter REST responses.
- Security is stateless; CSRF is disabled consistently with bearer tokens; no permissive CORS configuration was found, supporting the same-origin deployment.
- Explicit business authorities and deny-all fallbacks protect domain paths; tests cover 401/403/success and mutation ordering.
- E2E controls are `e2e`-profile-only and retain authentication/authorization.
- React rendering uses no identified raw-HTML/eval sink, and database calls use JPA/parameter binding or fixed SQL.

### Partial/missing

- no login/refresh rate limit, progressive delay, account lockout, or abuse-event monitoring;
- access and refresh tokens are stored in browser `localStorage`;
- public Swagger exposure and production security headers/CSP are not encoded;
- Nginx has no explicit CSP/HSTS/referrer/content-type/frame headers or TLS boundary;
- unexpected exceptions lack a consistent sanitized `ApiError`; client correlation IDs are not bounded;
- no formal penetration, SAST, secret scan, dependency scan for Maven, SBOM, or image scan gate.

## 11. Secrets Audit

| Classification | Result |
|---|---|
| Real committed production secret | None identified |
| Private key/API-provider token pattern | None identified in tracked files |
| Test-only/dev secret | H2 development default and test fixture values; not suitable for production |
| Example placeholder | README and environment templates use blank/change-me style values |
| Runtime-generated test secret | Playwright generates administrator credentials and JWT material per run |
| Local ignored secret file | `.env` exists locally, is ignored, and is not tracked; values were not copied into the audit |

Production still needs an approved secret manager/injection, least-privilege access, rotation, audit, and incident procedure. Local values must be rotated if they have ever been reused outside local development.

## 12. API Production Audit

Authentication, authorization, Jakarta validation, consistent known-error DTOs, explicit lifecycle endpoints, transactional services, idempotency for critical paths, concurrency locks, and 409 conflict behavior are strong.

Risks are workload boundaries: many master lists return `List`; frontend tables paginate client-side; notification list limits are bounded but several diagnostics/rule lists are broad; driver-assignment and vehicle-utilization reports are unpaged; report date ranges have no maximum; availability may evaluate many records. Offline Sync is correctly bounded to 1..50 items and has durable idempotency.

No multipart/file upload endpoint was found, despite document URL/reference fields. Default HTTP request-size/timeouts therefore remain deployment defaults rather than approved limits.

## 13. Frontend Production Audit

- Production compilation passes and the Nginx image serves static assets with SPA fallback and same-origin API proxy.
- Auth restoration and one-flight refresh rotation work; backend remains authoritative for direct unauthorized requests.
- Navigation and major action controls are permission-aware. Some generic resource routes do not proactively route-guard the view permission; they fail through backend 403/error UX rather than exposing data.
- No application-level error boundary exists.
- Routes are eagerly imported. The build emits one 1,876.05 KB minified JavaScript chunk (566.13 KB gzip); this is non-blocking for functional RC audit but needs measurement against approved performance targets.
- Unit tests emit non-failing Ant Design deprecated-property warnings and two MSW unhandled-request diagnostics. These reduce diagnostic quality but did not hide assertion failures.
- No raw HTML/eval sink or production `console.*` call was found.
- The full Playwright run passed 199/201. `E2E-AUTH-003` remained on `/` after clicking **Log out** in Firefox and WebKit. An isolated Firefox/WebKit rerun passed Firefox and reproduced WebKit (1/2). Because the expected redirect and session-clear completion were not observed consistently, the three-browser release gate is failed and the candidate must not be promoted until the behavior and test are made deterministic without weakening the assertion.

## 14. Offline Sync Production Audit

US-71 remains functionally complete: native IndexedDB v1, stable operation/client identity, owner isolation, queue-first capture, bounded retry, multi-tab leases, current JWT/authority replay, 1..50 server batch, V29 idempotency, partial results, safe conflict UX, and three-browser evidence. IndexedDB stores no passwords, access/refresh tokens, SMTP credentials, or provider secrets.

Production gaps are server inbox retention/growth, metrics and support diagnostics, deployment/schema compatibility procedures, and capacity evidence for operations/day. Local capacity is 1,000 non-synced operations and locally synced operations retain seven days; this does not bound the server inbox. IndexedDB is not a server backup and cross-device/PWA/background sync remain deferred.

## 15. Notification Production Audit

The SMTP path validates production-vs-test mode, sender, host, port, authentication, TLS (`starttls`/`ssl`, no cleartext except loopback), connect/read/write timeouts, and masks password configuration. The worker has durable attempts, deterministic idempotency, three bounded attempts, scheduled due delivery, quiet hours, suppression, escalation, and sanitized diagnostics.

EMAIL is disabled by default. No real staging provider handshake/delivery, sender-domain validation, provider quota, backlog alert, table retention, or outage runbook exists. Classification: development/test ready, staging configuration-ready, not yet production validated.

## 16. Observability and Error Handling

Actuator supplies default health/info/metrics/modulith endpoints behind `IDENTITY_MANAGE`. `CorrelationIdFilter` adds response/MDC correlation IDs; Trip/Fuel/Notification/Offline Sync persist business histories/attempts.

Missing are dependency-aware readiness, custom HTTP/business/backlog metrics, DB pool/database latency dashboards, structured log schema/retention/redaction, authentication/security events, alerts, SLOs, and incident links. `/health` always reports `UP`. Known exceptions use `ApiError`, but there is no sanitized catch-all; unexpected errors may use Spring's generic shape and omit correlation.

## 17. Backup, Restore, and Disaster Recovery

The repository contains checklist reminders but no approved backup frequency, retention, restore procedure/test, RPO, RTO, bad-migration recovery, frontend/backend rollback, SMTP outage, database outage, or configuration-failure runbook. These are production blockers. Values must be decided by product/operations; this audit does not invent them.

Failure considerations:

- database outage: application readiness is not dependency-aware;
- backend crash: durable PostgreSQL state is expected, but recovery/time objectives are undefined;
- frontend deployment: immutable static build exists, but no rollback/promotion procedure;
- SMTP outage: retry exists, but backlog monitoring/runbook does not;
- network interruption: Offline Sync handles accepted flows, but support/metrics remain absent;
- bad migration/release/configuration: no rehearsed rollback or forward-fix procedure.

## 18. Deployment and Containers

Backend and frontend use multi-stage builds. Backend builds an executable JAR; frontend runs `npm ci`, builds static assets, and serves them through Nginx. Compose starts PostgreSQL, waits for database health, starts backend, then frontend; Flyway runs at backend startup.

The assets are local-environment assets, not an approved production topology. Both runtime images use root, base tags are not digest-pinned, backend image packaging skips tests, H2 remains in the runtime artifact, frontend has no context-specific `.dockerignore`, PostgreSQL is published to the host, Compose enables bootstrap/sample data, and Nginx lacks an explicit TLS/security/cache/compression contract. There are no resource limits or immutable artifact promotion controls.

## 19. CI/CD and Supply Chain

No CI/CD configuration exists. Release gates, Java 21, PostgreSQL, npm audit, Playwright, image scanning, secret scanning, SBOM, provenance, artifact signing/checksums, branch protection, and deployment approval are not automated.

Current npm registry audit found zero known vulnerabilities for both production-only and complete dependency trees. Maven uses the Spring BOM plus explicit versions but has no configured vulnerability scanner; no conclusion about zero JVM vulnerabilities is made. Automatic upgrades were not performed.

## 20. Performance and Artillery Readiness

No SLA/threshold is invented. The repository has no Artillery configuration and is not ready for approved load testing until production-like configuration, disposable data, metrics, and capacity decisions exist.

| Scenario | Endpoint/method | Authentication/setup | Concurrency dimension and important metrics |
|---|---|---|---|
| AUTH-LOAD-001 | `POST /api/auth/login` | Synthetic active users; isolated environment | login rate, BCrypt CPU, p50/p95/p99, failures, lock/rate behavior |
| VEHICLE-READ-LOAD-001 | `GET /api/vehicles` | `VEHICLE_VIEW`; representative Fleet | concurrent readers, rows/response, heap, DB scans/latency |
| VEHICLE-AVAILABILITY-LOAD-001 | `GET /api/vehicles/{id}/availability` and availability list | Valid windows/docs/maintenance/trips | Fleet size, overlap density, query count, lock/DB latency |
| TRIP-CREATE-LOAD-001 | `POST /api/trips` | `TRIP_CREATE`; unique synthetic references | write concurrency, validation, sequence/unique conflicts, commit latency |
| TRIP-ASSIGN-LOAD-001 | assignment command endpoints | Approved trips/resources; assignment permissions | hot-resource contention, 409 rate, lock wait, one-winner invariant |
| TRIP-LIFECYCLE-LOAD-001 | submit/approve/dispatch/start/complete/close | Valid staged trips and authorities | same/different aggregate concurrency, history correctness, event latency |
| FUEL-ISSUE-LOAD-001 | create/authorize/issue commands | Synthetic stations/vehicles/tanks/prices | voucher generation, bunker lock contention, rollback, reading latency |
| REPORT-LOAD-001 | `/api/reports/*` GET | `REPORT_VIEW`; production-like ranges | date range/cardinality, query plans, heap, timeout/error rate |
| NOTIFICATION-READ-LOAD-001 | `GET /api/notifications?limit=...` | Owned recipient sets | concurrent polling, unread count, DB latency, response size |
| OFFLINE-SYNC-BATCH-LOAD-001 | `POST /api/offline-sync/operations` | Current JWT/authorities; 1/25/50 mixed items | batch size, duplicate ratio, per-item latency, inbox growth, conflicts/retries |

Every scenario requires a safe isolated environment, synthetic data cleanup, approved concurrency/thresholds, and active DB/JVM/application monitoring.

## 21. Capacity Inputs Required

`CAPACITY-INPUTS-REQUIRED`:

- registered and active users; peak concurrent users and sessions;
- vehicles, drivers, locations, customers, projects, routes, and active documents/licences;
- trips/day, peak dispatch/start/complete rate, average/peak trip duration and assignment contention;
- vehicle readings/day and maintenance/compliance scan cardinality;
- fuel issues/purchases/transfers/dips/day and bunker contention;
- notifications/day by channel, peak event burst, recipient fan-out, suppression ratio, SMTP quota;
- offline operations/day, batch-size distribution, offline duration, retry/conflict ratio;
- reporting date ranges, concurrent report users, export expectations;
- database and application-log retention, growth, legal holds, backup window;
- traffic peak multiplier, acceptable latency/error/availability objectives;
- deployment topology, instance count, DB connection budget, CPU/memory/storage limits.

## 22. Data Retention

| Data | Current behavior | Production decision/gap |
|---|---|---|
| Trip/Fuel/assignment/audit histories | Append/preserve; no purge | Retention/legal hold/archive policy missing |
| Vehicle readings/maintenance/compliance | Durable; no purge | Operational/legal retention missing |
| Driver medical/drug/licence/violation | Durable; no purge | Sensitive-data/legal retention missing |
| Notifications/rule executions/attempts | Durable; no purge | Queue/diagnostic retention missing |
| Offline server inbox | Durable V29 rows; no purge | Idempotency retention horizon and cleanup missing |
| Browser Offline Sync | Max 1,000 non-synced; synced purge after 7 days | Implemented locally; browser storage is not authoritative backup |
| Refresh tokens | Expiry/revocation enforced; no purge | Expired/revoked row cleanup missing |
| Application logs | Framework defaults | Format, retention, shipping, redaction missing |

## 23. Privacy and Sensitive Data

User identity/contact information, Driver contact/licence/medical/drug-test data, Vehicle document references/numbers, and audit actors are exposed only through authenticated permission mappings. Medical and drug-test APIs have distinct view/manage authorities. Offline payloads are limited to Vehicle readings and Trip operational events and contain no credentials or Driver medical/drug data.

Missing are formal classification, lawful/contractual retention, encryption-at-rest responsibility, key management, least-privilege access review, data export/deletion handling, log/cache minimization, and synthetic-test-data policy. No sensitive values are copied into these audit documents.

## 24. Operational Runbook Audit

No current executable runbooks were found for startup/shutdown, deployment/promotion, rollback, Flyway failure, backup/restore, account lockout, JWT/security incident, SMTP failure, offline-sync support, database outage, or health failure. Existing release documents are checklists/historical evidence, not current operating procedures.

## 25. Future Release Checklist

- [ ] Code freeze, reviewed version, candidate commit, and annotated tag policy.
- [ ] Java 21 backend clean test/verify and 16 architecture checks.
- [ ] PostgreSQL V1-V29 clean/upgrade validation and all 22 invariant tests.
- [ ] Frontend clean install, lint, 170+ unit tests, production build.
- [ ] Full three-browser Playwright with zero failures/skips/retries.
- [ ] Maven/npm/image/secret/license scans, SBOM, provenance/checksums.
- [ ] Production profile and environment/secret validation; dev/e2e controls absent.
- [ ] Backup completed and restore/rollback procedures confirmed.
- [ ] Health/readiness, monitoring, logging, dashboards, and alerts validated.
- [ ] Capacity/load evidence meets separately approved thresholds.
- [ ] SMTP staging validation if EMAIL is enabled.
- [ ] Privacy/retention/security/product/operations approvals.
- [ ] Immutable backend/frontend/container artifacts promoted with manual approval.
- [ ] Pre-deployment smoke, deployment, post-deployment smoke, migration validation.
- [ ] Monitoring validation, rollback decision window, and release evidence archive.

## 26. Risk Summary

The complete register is in `MVP-RC-001-RISK-REGISTER.md`.

- P0: 0.
- P1 RC blockers: 5.
- Additional P1 production blockers: production config/secrets, health/observability, backup/DR, security, API/data capacity, retention/privacy, containers/deployment, SMTP if enabled, supply chain, and performance/load evidence.
- P2/P3 non-blocking findings: bundle size, frontend test diagnostics, generic direct-route UX, stale documents, and reporting/dashboard limitations already represented honestly.

## 27. Recommended Hardening Tasks

The complete ordered backlog is in `MVP-RC-001-HARDENING-BACKLOG.md`. Begin with:

1. `RC-HARDEN-016` — deterministic cross-browser logout;
2. `RC-HARDEN-001` — fail-safe production configuration;
3. `RC-HARDEN-003` — protected Java 21 release pipeline;
4. `RC-HARDEN-002` — PostgreSQL V1-V29 release gate.

No hardening task is implemented by this audit.

## 28. Deferred/Non-Blocking Scope

The following remain non-blocking unless a separately approved launch requirement changes: Redis, PWA/service worker/background sync, cold offline startup, cross-device queue, generic merge/CRDT, new business reports/metrics, SSO/MFA/ABAC, new notification channels, and UI redesign. Their absence does not reopen the 39-story functional MVP.

## 29. Final RC Decision

**NOT_READY_FOR_RC**.

The functional regression baseline is strong, and no P0 or committed real secret was found. Promotion is blocked by the cross-browser logout regression, unsafe default/nonexistent production profile, missing current PostgreSQL invariant evidence, absent CI/release pipeline, and lack of Java 21 runtime reproduction. Fix and prove `RC-HARDEN-016` first, then complete `RC-HARDEN-001`, `RC-HARDEN-003`, and `RC-HARDEN-002` before repeating this decision gate. Even then, production must remain unapproved until all production blockers are mitigated or formally accepted by accountable owners.

## 30. Change Control

- Production code changes: none.
- Test code changes: none.
- API changes: none.
- Migration changes: none; no V30.
- Architecture changes: none.
- Dependency changes/upgrades: none.
- Audit documents created: this audit, risk register, hardening backlog.
