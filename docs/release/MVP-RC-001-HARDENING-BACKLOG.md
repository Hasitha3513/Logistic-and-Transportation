# MVP RC-001 Hardening Backlog

**Source:** MVP-RELEASE-CANDIDATE-001 production-readiness audit  
**Rule:** These tasks are recommendations only. No task is implemented by the audit.  
**Ordering:** Severity, then release dependency

## RC-HARDEN-001 — Establish a fail-safe production configuration

- **Problem:** The default profile is H2, no production profile exists, Swagger is always public, and local Compose explicitly enables administrator/sample-data bootstrap.
- **Evidence:** `application.yml`, `application-h2.yml`, `application-postgres.yml`, `SecurityConfig`, `compose.yml`.
- **Severity:** P1.
- **RC blocker:** Yes.
- **Production blocker:** Yes.
- **Scope:** Add an explicit production profile and validated environment contract; require PostgreSQL and high-entropy JWT/database secrets; forbid H2, `e2e`, test providers, dev identity, and sample data; make OpenAPI/Actuator exposure explicit; document Compose as development-only.
- **Out of scope:** Cloud-provider selection, feature changes, dependency upgrades, Redis.
- **Acceptance criteria:** A production-profile startup fails closed when any mandatory setting is absent; H2/dev/e2e beans cannot load; public endpoints and management exposure match an approved matrix; configuration tests prove the negative cases.
- **Verification:** Production-profile configuration tests, packaged-JAR startup with disposable PostgreSQL, negative startup tests, endpoint exposure integration tests.
- **Dependencies:** Operations/security decisions on ingress, secrets, and management endpoints.

## RC-HARDEN-002 — Execute the PostgreSQL V1-V29 release gate

- **Problem:** Twenty-two PostgreSQL/Testcontainers invariant and concurrency tests are environmentally skipped.
- **Evidence:** Surefire skip report and unavailable Docker daemon.
- **Severity:** P1.
- **RC blocker:** Yes.
- **Production blocker:** Yes.
- **Scope:** Run clean V1-V29 migration, `ddl-auto=validate`, full PostgreSQL invariant suite, concurrent allocation/lifecycle/bunker/reading/offline-sync cases, and rollback tests on PostgreSQL 16.
- **Out of scope:** Editing V1-V29 or using production data.
- **Acceptance criteria:** Zero skipped PostgreSQL tests; clean migration and startup pass; concurrency permits the documented winners; rollback/idempotency/constraints are proven; evidence is retained.
- **Verification:** CI Testcontainers job plus an isolated staging database rehearsal.
- **Dependencies:** RC-HARDEN-003 for repeatable CI execution.

## RC-HARDEN-003 — Build a protected Java 21 release pipeline

- **Problem:** No CI/CD pipeline enforces the gates and local tests ran on Java 26 instead of the supported Java 21 runtime.
- **Evidence:** No `.github` or alternate pipeline; Maven output reports Java 26.0.1.
- **Severity:** P1.
- **RC blocker:** Yes.
- **Production blocker:** Yes.
- **Scope:** Java 21 backend clean/verify/architecture; PostgreSQL tests; npm clean install/lint/unit/build/audit; full Playwright; container builds/scans; immutable artifacts, checksums, test reports, release approval.
- **Out of scope:** Automatic production deployment or selecting a cloud provider.
- **Acceptance criteria:** Protected branches require all gates; artifacts are generated once and promoted; production deployment requires human approval; secrets never enter logs/artifacts.
- **Verification:** Successful pipeline on the candidate commit and a deliberately failing change that blocks promotion.
- **Dependencies:** CI runner with Docker and all three Playwright browsers.

## RC-HARDEN-016 — Restore deterministic cross-browser logout

- **Problem:** The full Playwright gate failed `E2E-AUTH-003` in Firefox and WebKit because clicking **Log out** left the authenticated dashboard at `/` instead of redirecting to `/login`. An isolated rerun passed Firefox but reproduced WebKit.
- **Evidence:** Full result 199/201; isolated Firefox/WebKit result 1/2; `AuthContext.tsx`, `AppLayout.tsx`, `BasePage.ts`, and `auth.spec.ts`.
- **Severity:** P1.
- **RC blocker:** Yes.
- **Production blocker:** Yes.
- **Scope:** Diagnose the asynchronous logout/menu/navigation path; retain best-effort server refresh-token revocation and unconditional local credential clearing; make redirect/session termination deterministic across Chromium, Firefox, and WebKit; strengthen assertions for token removal and protected-route denial.
- **Out of scope:** Authentication redesign, SSO, MFA, broad UI refactoring, or weakening/removing the failing browser assertion.
- **Acceptance criteria:** Logout always removes both browser tokens, navigates to `/login`, and prevents returning to a protected route; API failure cannot strand a locally authenticated session; all supported browsers pass without retries.
- **Verification:** Focused real and mocked logout integration tests, then the complete 201-test Playwright matrix with zero failures/skips/retries.
- **Dependencies:** None; complete before accepting any candidate baseline.

## RC-HARDEN-004 — Add production health, metrics, and structured observability

- **Problem:** The custom health endpoint is static and there are no custom business/backlog metrics, logging schema, dashboards, or alerts.
- **Evidence:** `HealthController`, Actuator configuration, no `MeterRegistry` use or logging configuration.
- **Severity:** P1.
- **RC blocker:** No.
- **Production blocker:** Yes.
- **Scope:** Safe liveness/readiness, DB pool/latency, HTTP, authentication, notification backlog/failure, offline-sync result/backlog, business conflict metrics, structured logs with bounded correlation IDs, alert/runbook links.
- **Out of scope:** Vendor selection or broad application redesign.
- **Acceptance criteria:** Probes distinguish process liveness from dependency readiness; metrics have bounded cardinality; logs exclude credentials/medical payloads; alert conditions are tested.
- **Verification:** Integration tests, outage simulations, metric/log inspection, dashboard and alert acceptance.
- **Dependencies:** RC-HARDEN-001 exposure policy and operations platform decision.

## RC-HARDEN-005 — Approve backup, restore, rollback, and disaster-recovery procedures

- **Problem:** No approved backup/restore frequency, retention, RPO/RTO, restore test, bad-migration response, or release rollback procedure exists.
- **Evidence:** Existing release checklists leave these controls open; no operational runbooks found.
- **Severity:** P1.
- **RC blocker:** No.
- **Production blocker:** Yes.
- **Scope:** PostgreSQL backup/restore rehearsal, RPO/RTO decisions, migration failure response, application/frontend rollback, configuration rollback, SMTP/network/DB outage procedures.
- **Out of scope:** Inventing business RPO/RTO values or treating IndexedDB as backup.
- **Acceptance criteria:** Named owners approve RPO/RTO/retention; restore is timed and validated; forward-migration and rollback strategy is executable; evidence and escalation paths exist.
- **Verification:** Non-production disaster-recovery exercise and signed runbooks.
- **Dependencies:** Target deployment topology and data-retention decisions.

## RC-HARDEN-006 — Harden the authentication and HTTP security perimeter

- **Problem:** No brute-force/rate controls exist, browser tokens are in `localStorage`, production Swagger/security-header decisions are absent, and unexpected error/correlation handling is incomplete.
- **Evidence:** `SecurityConfig`, `AuthContext.tsx`, `client.ts`, `GlobalExceptionHandler`, `CorrelationIdFilter`.
- **Severity:** P1.
- **RC blocker:** No.
- **Production blocker:** Yes.
- **Scope:** Approved login/refresh throttling and lockout policy, frontend token threat model, CSP/security headers, CORS/CSRF rationale, Swagger policy, sanitized catch-all errors, bounded correlation IDs, security-event audit.
- **Out of scope:** SSO, MFA, ABAC, or changing business authorization semantics.
- **Acceptance criteria:** Abuse tests are bounded and auditable; current 401/403 semantics remain; no secret/stack/SQL leakage; production headers and docs exposure pass tests; token-storage risk is mitigated or formally accepted.
- **Verification:** Security integration tests, browser header checks, logging inspection, penetration-test checklist.
- **Dependencies:** Ingress/session architecture decision.

## RC-HARDEN-007 — Bound API and reporting workloads

- **Problem:** Numerous list APIs and two reports are unbounded; browser-only pagination and full Fleet lookups create capacity and N+1/full-scan risk.
- **Evidence:** Fleet/organization/identity/routing controllers and repository ports returning `List`; reporting services call `findAllVehicles/findAllDrivers`.
- **Severity:** P1.
- **RC blocker:** No.
- **Production blocker:** Yes.
- **Scope:** Server pagination with strict maximum limits, bounded report ranges, indexed query plans, batched/projection-based availability/reporting, compatible frontend pagination.
- **Out of scope:** New report metrics or speculative optimization.
- **Acceptance criteria:** Every potentially large collection is bounded; invalid pages/ranges fail consistently; representative query plans avoid full scans/N+1; API contracts and UI tests pass.
- **Verification:** Integration/contract tests, PostgreSQL `EXPLAIN ANALYZE`, Artillery read scenarios.
- **Dependencies:** Capacity inputs from RC-HARDEN-013.

## RC-HARDEN-008 — Define and implement server data-retention controls

- **Problem:** Durable operational, audit, notification, refresh-token, and offline-sync tables have no approved retention or cleanup.
- **Evidence:** V2 and V25-V29 plus absence of purge jobs/repository operations.
- **Severity:** P1.
- **RC blocker:** No.
- **Production blocker:** Yes.
- **Scope:** Record-specific retention/legal holds; safe purge/archive jobs; batching, indexes, observability, dry-run/recovery; keep required audit history intact.
- **Out of scope:** Deleting data before policy approval or editing historical migrations.
- **Acceptance criteria:** Every data family has an owner and retention rule; cleanup is bounded/idempotent/audited; no active or legally retained record is lost.
- **Verification:** Policy review, integration tests, production-like volume rehearsal, restore test.
- **Dependencies:** Privacy/legal and capacity decisions; a future forward migration only if proven necessary.

## RC-HARDEN-009 — Harden backend/frontend container and ingress assets

- **Problem:** Containers run as root, images are not digest-pinned, frontend context is unbounded, and edge TLS/security/cache behavior is unspecified.
- **Evidence:** Both Dockerfiles, missing `frontend/.dockerignore`, `nginx.conf`, `compose.yml`.
- **Severity:** P1.
- **RC blocker:** No.
- **Production blocker:** Yes.
- **Scope:** Non-root runtime, read-only filesystem/capabilities where feasible, approved pinned images, minimized contexts/layers, image scanning, runtime resource limits, trusted proxy/TLS/header/cache contract, health integration.
- **Out of scope:** Cloud selection or Kubernetes requirement.
- **Acceptance criteria:** Images run non-root with no build tooling; SBOM/scan gates pass; frontend artifacts are immutable; ingress behavior and health checks are tested.
- **Verification:** Container inspection, vulnerability scan, runtime smoke, header/TLS checks.
- **Dependencies:** RC-HARDEN-001 and RC-HARDEN-003.

## RC-HARDEN-010 — Validate production-mode SMTP in staging

- **Problem:** SMTP logic is strong in tests but no real staging TLS/auth/delivery/outage evidence or operational alerting exists.
- **Evidence:** Email properties/configuration, delivery worker, retry tests; EMAIL disabled by default.
- **Severity:** P1.
- **RC blocker:** No.
- **Production blocker:** Yes if EMAIL is enabled at launch.
- **Scope:** Staging provider configuration, TLS/auth, allow-listed recipients, timeouts, retry/terminal behavior, backlog alerting, outage and credential-rotation runbooks.
- **Out of scope:** Sending production email or selecting extra channels.
- **Acceptance criteria:** Staging messages deliver with sanitized provider diagnostics; outage behavior is bounded; credentials remain secret; operators can diagnose backlog/failure.
- **Verification:** Controlled staging tests and runbook exercise.
- **Dependencies:** Secrets and observability controls.

## RC-HARDEN-011 — Improve frontend resilience and measured delivery performance

- **Problem:** No top-level error boundary or route splitting exists; build emits a large-chunk warning and tests emit deprecation/MSW diagnostics.
- **Evidence:** `App.tsx`, `main.tsx`, current build/unit output.
- **Severity:** P2.
- **RC blocker:** No.
- **Production blocker:** No unless performance targets require it.
- **Scope:** Error boundary/recovery UX, measured route-level lazy loading, cache/compression validation, warning cleanup, direct-route access UX.
- **Out of scope:** UI redesign or changing backend authorization.
- **Acceptance criteria:** Fatal render failures show recoverable safe UX; critical-route performance improves against approved metrics; tests run without known diagnostics.
- **Verification:** Vitest, Playwright, bundle report, browser performance measurements.
- **Dependencies:** Approved performance targets.

## RC-HARDEN-012 — Add supply-chain, secret, SBOM, and artifact provenance gates

- **Problem:** npm currently reports zero vulnerabilities, but Maven/image/secret/SBOM/provenance checks are absent.
- **Evidence:** `pom.xml`, package lock, no CI/security scanner configuration.
- **Severity:** P1.
- **RC blocker:** No.
- **Production blocker:** Yes.
- **Scope:** Maven and npm advisory scans, secret scanning, container scanning, SBOM generation, license policy, immutable artifact checksums/signing, triage/exception process.
- **Out of scope:** Automatic dependency upgrades.
- **Acceptance criteria:** Critical/high policy is enforced; findings distinguish runtime/dev/test; approved exceptions expire; release artifact provenance is retained.
- **Verification:** CI reports plus seeded-safe failure tests.
- **Dependencies:** RC-HARDEN-003.

## RC-HARDEN-013 — Define capacity inputs and execute approved Artillery tests

- **Problem:** No workload model, SLA thresholds, Artillery harness, or performance evidence exists.
- **Evidence:** No Artillery files/dependency; missing business volume decisions.
- **Severity:** P1.
- **RC blocker:** No.
- **Production blocker:** Yes.
- **Scope:** Approve capacity inputs and thresholds; build isolated scenarios for auth, Fleet reads/availability, Trip create/assignment/lifecycle, Fuel issue, reports, notifications, and offline batches; observe DB/JVM/backlogs.
- **Out of scope:** Production load tests or invented SLAs.
- **Acceptance criteria:** Test data is disposable; workloads reproduce approved peaks; results capture latency/error/resource dimensions; bottlenecks have owners.
- **Verification:** Repeatable staging Artillery report linked to release evidence.
- **Dependencies:** RC-HARDEN-004, 007, and capacity-owner decisions.

## RC-HARDEN-014 — Complete privacy and sensitive-data control review

- **Problem:** Sensitive Driver/user/document/audit data is permission-protected but lacks a documented classification, encryption, retention, export, and minimization decision.
- **Evidence:** Fleet/identity DTOs and permissions; no privacy control document.
- **Severity:** P1.
- **RC blocker:** No.
- **Production blocker:** Yes.
- **Scope:** Data inventory/classification, least-privilege verification, log/cache/IndexedDB review, encryption responsibilities, retention/legal holds, test-data policy, access review.
- **Out of scope:** Copying real sensitive data into documentation or deleting audit history without approval.
- **Acceptance criteria:** Data owners approve handling; high-sensitivity endpoints/logs/caches have explicit controls; test fixtures are synthetic.
- **Verification:** Privacy/security review and automated authorization/logging checks.
- **Dependencies:** Legal/product decisions and RC-HARDEN-008.

## RC-HARDEN-015 — Reconcile release documentation and create current runbooks/checklist

- **Problem:** README and prior release documents contain obsolete scope, migration, branch, and test-count claims; operational runbooks are missing.
- **Evidence:** README V10/V11 wording and older release assessments versus current V29/681/170/201 baseline.
- **Severity:** P2.
- **RC blocker:** No.
- **Production blocker:** Yes for runbooks; stale historical docs alone are non-blocking.
- **Scope:** Mark historical documents, update current setup/architecture/release evidence, and create startup/shutdown/deploy/rollback/migration/backup/restore/security/SMTP/offline-sync/DB-outage/health runbooks plus release checklist.
- **Out of scope:** Rewriting historical evidence as if it never existed.
- **Acceptance criteria:** One authoritative current release index exists; runbooks have owners and tested commands; stale documents are clearly superseded.
- **Verification:** Documentation review and tabletop runbook exercise.
- **Dependencies:** All P1 configuration, deployment, recovery, and observability decisions.

## Recommended dependency order

1. RC-HARDEN-016.
2. RC-HARDEN-001, RC-HARDEN-003.
3. RC-HARDEN-002.
4. RC-HARDEN-004, RC-HARDEN-005, RC-HARDEN-006, RC-HARDEN-009, RC-HARDEN-012.
5. RC-HARDEN-007, RC-HARDEN-008, RC-HARDEN-010, RC-HARDEN-014.
6. RC-HARDEN-013.
7. RC-HARDEN-011 and RC-HARDEN-015.
